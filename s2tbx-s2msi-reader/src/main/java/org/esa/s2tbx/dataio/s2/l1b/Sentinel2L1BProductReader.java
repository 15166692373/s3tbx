/*
 *
 * Copyright (C) 2013-2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 * Copyright (C) 2014-2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s2tbx.dataio.s2.l1b;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import com.vividsolutions.jts.geom.Coordinate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.math3.util.Pair;
import org.esa.s2tbx.dataio.Utils;
import org.esa.s2tbx.dataio.jp2.TileLayout;
import org.esa.s2tbx.dataio.s2.*;
import org.esa.s2tbx.dataio.s2.filepatterns.S2GranuleImageFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2ProductFilename;
import org.esa.s2tbx.dataio.s2.l1b.filepaterns.S2L1BGranuleDirFilename;
import org.esa.s2tbx.dataio.s2.l1b.filepaterns.S2L1BGranuleMetadataFilename;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.*;
import org.esa.snap.jai.ImageManager;
import org.esa.snap.util.Guardian;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.FileUtils;
import org.jdom.JDOMException;

import javax.media.jai.*;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.esa.s2tbx.dataio.s2.S2Metadata.ProductCharacteristics;
import static org.esa.s2tbx.dataio.s2.S2Metadata.Tile;
import static org.esa.s2tbx.dataio.s2.l1b.CoordinateUtils.*;
import static org.esa.s2tbx.dataio.s2.l1b.L1bMetadata.parseHeader;

// import com.jcabi.aspects.Loggable;

// import org.github.jamm.MemoryMeter;

// todo - register reasonable RGB profile(s)
// todo - set a band's validMaskExpr or no-data value (read from GML)
// todo - viewing incidence tie-point grids contain NaN values - find out how to correctly treat them

// todo - better collect problems during product opening and generate problem report (requires reader API change), see {@report "Problem detected..."} code marks

/**
 * <p>
 * This product reader can currently read single L1C tiles (also called L1C granules) and entire L1C scenes composed of
 * multiple L1C tiles.
 * </p>
 * <p>
 * To read single tiles, select any tile image file (IMG_*.jp2) within a product package. The reader will then
 * collect other band images for the selected tile and wiull also try to read the metadata file (MTD_*.xml).
 * </p>
 * <p>To read an entire scene, select the metadata file (MTD_*.xml) within a product package. The reader will then
 * collect other tile/band images and create a mosaic on the fly.
 * </p>
 *
 * @author Norman Fomferra
 */
public class Sentinel2L1BProductReader extends Sentinel2ProductReader {

    static final String L1B_CACHE_DIR = "l1b-reader";

    protected final Logger logger;

    public Sentinel2L1BProductReader(ProductReaderPlugIn readerPlugIn, S2SpatialResolution productResolution) {
        super(readerPlugIn, productResolution, false);
        logger = SystemUtils.LOG;
    }

    public Sentinel2L1BProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn, S2SpatialResolution.R10M, true);
        logger = SystemUtils.LOG;
    }

    @Override
    protected String getReaderCacheDir() {
        return L1B_CACHE_DIR;
    }



    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        // Should never not come here, since we have an OpImage that reads data
    }

    private TiePointGrid addTiePointGrid(int width, int height, String gridName, float[] tiePoints) {
        return createTiePointGrid(gridName, 2, 2, 0, 0, width, height, tiePoints);
    }

    @Override
    protected Product getMosaicProduct(File metadataFile) throws IOException {

        boolean isAGranule = S2L1BGranuleMetadataFilename.isGranuleFilename(metadataFile.getName());

        if(isAGranule) {
            logger.fine("Reading a granule");
        }

        // update the tile layout
        if(isMultiResolution()) {
            updateTileLayout(metadataFile.toPath(), isAGranule, null);
        } else {
            updateTileLayout(metadataFile.toPath(), isAGranule, getProductResolution());
        }

        Objects.requireNonNull(metadataFile);

        String filterTileId = null;
        File productMetadataFile = null;

        // we need to recover parent metadata file if we have a granule
        if(isAGranule)
        {
            try
            {
                Objects.requireNonNull(metadataFile.getParentFile());
                Objects.requireNonNull(metadataFile.getParentFile().getParentFile());
                Objects.requireNonNull(metadataFile.getParentFile().getParentFile().getParentFile());
            } catch (NullPointerException npe)
            {
                throw new IOException(String.format("Unable to retrieve the product associated to granule metadata file [%s]", metadataFile.getName()));
            }

            File up2levels = metadataFile.getParentFile().getParentFile().getParentFile();
            File tileIdFilter = metadataFile.getParentFile();

            filterTileId = tileIdFilter.getName();

            File[] files = up2levels.listFiles();
            if(files != null) {
                for (File f : files) {
                    if (S2ProductFilename.isProductFilename(f.getName()) && S2ProductFilename.isMetadataFilename(f.getName())) {
                        productMetadataFile = f;
                        break;
                    }
                }
            }
            if(productMetadataFile == null)
            {
                throw new IOException(String.format("Unable to retrieve the product associated to granule metadata file [%s]", metadataFile.getName()));
            }
        }
        else
        {
            productMetadataFile = metadataFile;
        }

        final String aFilter = filterTileId;

        L1bMetadata metadataHeader;

        try {
            metadataHeader = parseHeader(productMetadataFile, getConfig());
        } catch (JDOMException|JAXBException e) {
            SystemUtils.LOG.severe(Utils.getStackTrace(e));
            throw new IOException("Failed to parse metadata in " + productMetadataFile.getName());
        }

        L1bSceneDescription sceneDescription = L1bSceneDescription.create(metadataHeader, getProductResolution());
        logger.fine("Scene Description: " + sceneDescription);

        File productDir = getProductDir(productMetadataFile);
        initCacheDir(productDir);

        ProductCharacteristics productCharacteristics = metadataHeader.getProductCharacteristics();

        List<L1bMetadata.Tile> tileList = metadataHeader.getTileList();

        if(isAGranule)
        {
            tileList = metadataHeader.getTileList().stream().filter(p -> p.getId().equalsIgnoreCase(aFilter)).collect(Collectors.toList());
        }

        Map<String, Tile> tilesById = new HashMap<>(tileList.size());
        for (Tile aTile : tileList) {
            tilesById.put(aTile.getId(), aTile);
        }

        // Order bands by physicalBand
        Map<String, S2SpectralInformation> sin = new HashMap<>();
        for (S2SpectralInformation bandInformation : productCharacteristics.getBandInformations()) {
            sin.put(bandInformation.getPhysicalBand(), bandInformation);
        }

        Map<Pair<String, String>, Map<String, File>> detectorBandInfoMap = new HashMap<>();
        Map<String, L1BBandInfo> bandInfoByKey = new HashMap<>();
        if (productCharacteristics.getBandInformations() != null) {
            for (Tile tile : tileList) {
                S2L1BGranuleDirFilename gf = (S2L1BGranuleDirFilename) S2L1BGranuleDirFilename.create(tile.getId());
                Guardian.assertNotNull("Product files don't match regular expressions", gf);

                for (S2SpectralInformation bandInformation : productCharacteristics.getBandInformations()) {
                    S2GranuleImageFilename granuleFileName = gf.getImageFilename(bandInformation.getPhysicalBand());
                    String imgFilename = "GRANULE" + File.separator + tile.getId() + File.separator + "IMG_DATA" + File.separator + granuleFileName.name;

                    logger.finer("Adding file " + imgFilename + " to band: " + bandInformation.getPhysicalBand() + ", and detector: " + gf.getDetectorId());

                    File file = new File(productDir, imgFilename);
                    if (file.exists()) {
                        Pair<String, String> key = new Pair<>(bandInformation.getPhysicalBand(), gf.getDetectorId());
                        Map<String, File> fileMapper = detectorBandInfoMap.getOrDefault(key, new HashMap<>());
                        fileMapper.put(tile.getId(), file);
                        if (!detectorBandInfoMap.containsKey(key)) {
                            detectorBandInfoMap.put(key, fileMapper);
                        }
                    } else {
                        logger.warning(String.format("Warning: missing file %s\n", file));
                    }
                }
            }

            if (!detectorBandInfoMap.isEmpty()) {
                for (Pair<String, String> key : detectorBandInfoMap.keySet()) {
                    L1BBandInfo tileBandInfo = createBandInfoFromHeaderInfo(
                            key.getSecond(), sin.get(key.getFirst()), detectorBandInfoMap.get(key));

                    // composite band name : detector + band
                    String keyMix = key.getSecond() + key.getFirst();
                    bandInfoByKey.put(keyMix, tileBandInfo);
                }
            }
        } else {
            // fixme Look for optional info in schema
            logger.warning("There are no spectral information here !");
        }

        Product product;

        if(sceneDescription != null) {
            product = new Product(FileUtils.getFilenameWithoutExtension(productMetadataFile),
                                  "S2_MSI_" + productCharacteristics.getProcessingLevel(),
                                  sceneDescription.getSceneRectangle().width,
                                  sceneDescription.getSceneRectangle().height);


            Map<String, GeoCoding> geoCodingsByDetector = new HashMap<>();

            if (!bandInfoByKey.isEmpty()) {
                for (L1BBandInfo tbi : bandInfoByKey.values()) {
                    if (!geoCodingsByDetector.containsKey(tbi.detectorId)) {
                        GeoCoding gc = getGeoCodingFromTileBandInfo(tbi, tilesById, product);
                        geoCodingsByDetector.put(tbi.detectorId, gc);
                    }
                }
            }

            addDetectorBands(product, bandInfoByKey, new L1bSceneMultiLevelImageFactory(sceneDescription, ImageManager.getImageToModelTransform(product.getGeoCoding())));
        } else {
            product = new Product(FileUtils.getFilenameWithoutExtension(productMetadataFile),
                                  "S2_MSI_" + productCharacteristics.getProcessingLevel());
        }

        product.setFileLocation(productMetadataFile.getParentFile());

        for(MetadataElement metadataElement : metadataHeader.getMetadataElements()) {
            product.getMetadataRoot().addElement(metadataElement);
        }


        return product;
    }

    /**
     * Uses the 4 lat-lon corners of a detector to create the geocoding
     */
    private GeoCoding getGeoCodingFromTileBandInfo(L1BBandInfo tileBandInfo, Map<String, Tile> tileList, Product product) {
        Objects.requireNonNull(tileBandInfo);
        Objects.requireNonNull(tileList);
        Objects.requireNonNull(product);

        Set<String> ourTileIds = tileBandInfo.getTileIdToFileMap().keySet();
        List<Tile> aList = new ArrayList<>(ourTileIds.size());
        List<Coordinate> coords = new ArrayList<>();
        for (String tileId : ourTileIds) {
            Tile currentTile = tileList.get(tileId);
            aList.add(currentTile);
        }

        // sort tiles by position
        Collections.sort(aList, (Tile u1, Tile u2) -> u1.getTileGeometry10M().getPosition().compareTo(u2.getTileGeometry10M().getPosition()));

        coords.add(aList.get(0).corners.get(0));
        coords.add(aList.get(0).corners.get(3));
        coords.add(aList.get(aList.size() - 1).corners.get(1));
        coords.add(aList.get(aList.size() - 1).corners.get(2));

        float[] lats = convertDoublesToFloats(getLatitudes(coords));
        float[] lons = convertDoublesToFloats(getLongitudes(coords));

        TiePointGrid latGrid = addTiePointGrid(aList.get(0).getTileGeometry10M().getNumCols(), aList.get(0).getTileGeometry10M().getNumRowsDetector(), tileBandInfo.getSpectralInfo().getPhysicalBand() + ",latitude", lats);
        product.addTiePointGrid(latGrid);
        TiePointGrid lonGrid = addTiePointGrid(aList.get(0).getTileGeometry10M().getNumCols(), aList.get(0).getTileGeometry10M().getNumRowsDetector(), tileBandInfo.getSpectralInfo().getPhysicalBand() + ",longitude", lons);
        product.addTiePointGrid(lonGrid);

        return  new TiePointGeoCoding(latGrid, lonGrid);
    }

    private void addDetectorBands(Product product, Map<String, L1BBandInfo> stringBandInfoMap, MultiLevelImageFactory mlif) throws IOException {
        product.setPreferredTileSize(S2Config.DEFAULT_JAI_TILE_SIZE, S2Config.DEFAULT_JAI_TILE_SIZE);
        product.setNumResolutionsMax(getConfig().getTileLayout(getProductResolution().resolution).numResolutions);

        product.setAutoGrouping("D01:D02:D03:D04:D05:D06:D07:D08:D09:D10:D11:D12");

        ArrayList<String> bandIndexes = new ArrayList<>(stringBandInfoMap.keySet());
        Collections.sort(bandIndexes);

        if (bandIndexes.isEmpty()) {
            throw new IOException("No valid bands found.");
        }

        for (String bandIndex : bandIndexes) {
            L1BBandInfo tileBandInfo = stringBandInfoMap.get(bandIndex);
            if (isMultiResolution() || tileBandInfo.getSpectralInfo().getResolution() == this.getProductResolution()){
                Band band = addBand(product, tileBandInfo);
                band.setSourceImage(mlif.createSourceImage(tileBandInfo));

/*                try {
                    band.setGeoCoding(new CrsGeoCoding(envelope.getCoordinateReferenceSystem(),
                                                       band.getRasterWidth(),
                                                       band.getRasterHeight(),
                                                       envelope.getMinX(),
                                                       envelope.getMaxY(),
                                                       tileBandInfo.getWavebandInfo().resolution.resolution,
                                                       tileBandInfo.getWavebandInfo().resolution.resolution,
                                                       0.0, 0.0));
                } catch (FactoryException e) {
                    logger.severe("Illegal CRS");
                } catch (TransformException e) {
                    logger.severe("Illegal projection");
                }*/
            }
        }
        

    }

    private L1BBandInfo createBandInfoFromHeaderInfo(String detector, S2SpectralInformation bandInformation, Map<String, File> tileFileMap) {
        S2SpatialResolution spatialResolution = bandInformation.getResolution();
        return new L1BBandInfo(tileFileMap,
                                bandInformation.getBandId(),
                                detector,
                                bandInformation,
                                getConfig().getTileLayout(spatialResolution.resolution));
    }

    static File getProductDir(File productFile) throws IOException {
        final File resolvedFile = productFile.getCanonicalFile();
        if (!resolvedFile.exists()) {
            throw new FileNotFoundException("File not found: " + productFile);
        }

        if (productFile.getParentFile() == null) {
            return new File(".").getCanonicalFile();
        }

        return productFile.getParentFile();
    }


    private abstract class MultiLevelImageFactory {
        protected final AffineTransform imageToModelTransform;

        protected MultiLevelImageFactory(AffineTransform imageToModelTransform) {
            this.imageToModelTransform = imageToModelTransform;
        }

        public abstract MultiLevelImage createSourceImage(L1BBandInfo tileBandInfo);
    }

    private class L1bSceneMultiLevelImageFactory extends MultiLevelImageFactory {

        private final L1bSceneDescription sceneDescription;

        public L1bSceneMultiLevelImageFactory(L1bSceneDescription sceneDescription, AffineTransform imageToModelTransform) {
            super(imageToModelTransform);

            SystemUtils.LOG.fine("Model factory: " + ToStringBuilder.reflectionToString(imageToModelTransform));

            this.sceneDescription = sceneDescription;
        }

        @Override
        // @Loggable
        public MultiLevelImage createSourceImage(L1BBandInfo tileBandInfo) {
            BandL1bSceneMultiLevelSource bandScene = new BandL1bSceneMultiLevelSource(sceneDescription, tileBandInfo, imageToModelTransform);
            SystemUtils.LOG.log(Level.parse(S2Config.LOG_SCENE), "BandScene: " + bandScene);
            return new DefaultMultiLevelImage(bandScene);
        }
    }


    /**
     * A MultiLevelSource for a scene made of multiple L1C tiles.
     */
    private abstract class AbstractL1bSceneMultiLevelSource extends AbstractMultiLevelSource {
        protected final L1bSceneDescription sceneDescription;

        AbstractL1bSceneMultiLevelSource(L1bSceneDescription sceneDescription, AffineTransform imageToModelTransform, int numResolutions) {
            super(new DefaultMultiLevelModel(numResolutions,
                                             imageToModelTransform,
                                             sceneDescription.getSceneRectangle().width,
                                             sceneDescription.getSceneRectangle().height));
            this.sceneDescription = sceneDescription;
        }
    }

    /**
     * A MultiLevelSource used by bands for a scene made of multiple L1C tiles.
     */
    private final class BandL1bSceneMultiLevelSource extends AbstractL1bSceneMultiLevelSource {
        private final L1BBandInfo tileBandInfo;

        public BandL1bSceneMultiLevelSource(L1bSceneDescription sceneDescription, L1BBandInfo tileBandInfo, AffineTransform imageToModelTransform) {
            super(sceneDescription, imageToModelTransform, tileBandInfo.getImageLayout().numResolutions);
            this.tileBandInfo = tileBandInfo;
        }

        protected PlanarImage createL1bTileImage(String tileId, int level) {
            File imageFile = tileBandInfo.getTileIdToFileMap().get(tileId);

            PlanarImage planarImage = S2TileOpImage.create(imageFile,
                                                           getCacheDir(),
                                                           null, // tileRectangle.getLocation(),
                                                           tileBandInfo.getImageLayout(),
                                                           getConfig(),
                                                           getModel(),
                                                           getProductResolution(),
                                                           level);

            logger.fine(String.format("Planar image model: %s", getModel().toString()));

            logger.fine(String.format("Planar image created: %s %s: minX=%d, minY=%d, width=%d, height=%d\n",
                                      tileBandInfo.getSpectralInfo().getPhysicalBand(), tileId,
                                      planarImage.getMinX(), planarImage.getMinY(),
                                      planarImage.getWidth(), planarImage.getHeight()));

            return planarImage;
        }

        @Override
        protected RenderedImage createImage(int level) {
            ArrayList<RenderedImage> tileImages = new ArrayList<>();

            List<String> tiles = sceneDescription.getTileIds().stream().filter(x -> x.contains(tileBandInfo.detectorId)).collect(Collectors.toList());


            TileLayout thisBandTileLayout = this.tileBandInfo.getImageLayout();
            TileLayout productTileLayout = getConfig().getTileLayout(getProductResolution());
            float layoutRatioX = (float) productTileLayout.width / thisBandTileLayout.width;
            float layoutRatioY = (float) productTileLayout.height / thisBandTileLayout.height;


            for (String tileId : tiles) {
                int tileIndex = sceneDescription.getTileIndex(tileId);
                Rectangle tileRectangle = sceneDescription.getTileRectangle(tileIndex);

                PlanarImage opImage = createL1bTileImage(tileId, level);
                {

                    double factorX = 1.0 / (Math.pow(2, level) * layoutRatioX);
                    double factorY = 1.0 / (Math.pow(2, level) * layoutRatioY);

                    opImage = TranslateDescriptor.create(opImage,
                                                         (float) Math.floor((tileRectangle.x * factorX)),
                                                         (float) Math.floor((tileRectangle.y * factorY)),
                                                         Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);

                    logger.log(Level.parse(S2Config.LOG_SCENE), String.format("Translate descriptor: %s", ToStringBuilder.reflectionToString(opImage)));
                }

                logger.log(Level.parse(S2Config.LOG_SCENE), String.format("opImage added for level %d at (%d,%d) with size (%d,%d)%n", level, opImage.getMinX(), opImage.getMinY(), opImage.getWidth(), opImage.getHeight()));
                tileImages.add(opImage);
            }

            if (tileImages.isEmpty()) {
                logger.warning("No tile images for mosaic");
                return null;
            }

            ImageLayout imageLayout = new ImageLayout();
            imageLayout.setMinX(0);
            imageLayout.setMinY(0);
            imageLayout.setTileWidth(S2Config.DEFAULT_JAI_TILE_SIZE);
            imageLayout.setTileHeight(S2Config.DEFAULT_JAI_TILE_SIZE);
            imageLayout.setTileGridXOffset(0);
            imageLayout.setTileGridYOffset(0);

            RenderedOp mosaicOp = MosaicDescriptor.create(tileImages.toArray(new RenderedImage[tileImages.size()]),
                                                          MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                                                          null, null, new double[][]{{1.0}}, new double[]{S2Config.FILL_CODE_MOSAIC_BG},
                                                          new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));


            int fitRectWidht = (int) (sceneDescription.getSceneEnvelope().getWidth() /
                    (layoutRatioX * getProductResolution().resolution ));
            int fitRectHeight = (int) (sceneDescription.getSceneEnvelope().getHeight() /
                    (layoutRatioY * getProductResolution().resolution ));

            Rectangle fitRect = new Rectangle(0, 0, fitRectWidht, fitRectHeight);
            final Rectangle destBounds = DefaultMultiLevelSource.getLevelImageBounds(fitRect, Math.pow(2.0, level));

            BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);

            if (mosaicOp.getWidth() < destBounds.width || mosaicOp.getHeight() < destBounds.height) {
                int rightPad = destBounds.width - mosaicOp.getWidth();
                int bottomPad = destBounds.height - mosaicOp.getHeight();
                SystemUtils.LOG.log(Level.parse(S2Config.LOG_SCENE), String.format("Border: (%d, %d), (%d, %d)", mosaicOp.getWidth(), destBounds.width, mosaicOp.getHeight(), destBounds.height));

                mosaicOp = BorderDescriptor.create(mosaicOp, 0, rightPad, 0, bottomPad, borderExtender, null);
            }

            logger.log(Level.parse(S2Config.LOG_SCENE), String.format("mosaicOp created for level %d at (%d,%d) with size (%d, %d)%n", level, mosaicOp.getMinX(), mosaicOp.getMinY(), mosaicOp.getWidth(), mosaicOp.getHeight()));

            return mosaicOp;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }



    @Override
    protected String[] getBandNames(S2SpatialResolution resolution) {
        String[] bandNames;

        switch (resolution) {
            case R10M:
                bandNames = new String[] {"B02", "B03", "B04", "B08"};
                break;
            case R20M:
                bandNames = new String[] {"B05", "B06", "B07", "B8A", "B11", "B12"};
                break;
            case R60M:
                bandNames = new String[] {"B01", "B09", "B10"};
                break;
            default:
                SystemUtils.LOG.warning("Invalid resolution: " + resolution);
                bandNames = null;
                break;
        }

        return bandNames;
    }

        public static class L1BBandInfo extends BandInfo {

        private final String detectorId;

        L1BBandInfo(Map<String, File> tileIdToFileMap, int bandIndex, String detector, S2SpectralInformation spectralInfo, TileLayout imageLayout) {
            super(tileIdToFileMap, bandIndex, spectralInfo, imageLayout);

            this.detectorId = detector == null ? "" : detector;
        }

        public String getDetectorId() {
            return detectorId;
        }
    }
}
