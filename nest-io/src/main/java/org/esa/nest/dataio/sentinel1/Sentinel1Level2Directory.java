/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.nest.dataio.sentinel1;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.nest.dataio.SARReader;
import org.esa.nest.dataio.XMLProductDirectory;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.datamodel.metadata.AbstractMetadataIO;
import org.esa.snap.eo.Constants;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.util.XMLSupport;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a product directory.
 */
public class Sentinel1Level2Directory extends XMLProductDirectory implements Sentinel1Directory {

    private final transient Map<String, String> imgBandMetadataMap = new HashMap<>(4);
    private Sentinel1OCNReader OCNReader = null;
    private String acqMode = "";

    public Sentinel1Level2Directory(final File inputFile) {
        super(inputFile);
    }

    protected String getHeaderFileName() {
        return Sentinel1Constants.PRODUCT_HEADER_NAME;
    }

    protected String getRelativePathToImageFolder() {
        return getRootFolder() + "measurement" + '/';
    }

    protected void addImageFile(final String imgPath) throws IOException {
        final String name = imgPath.substring(imgPath.lastIndexOf('/')+1, imgPath.length()).toLowerCase();
        if (name.endsWith(".nc")) {
            if (OCNReader == null)
                OCNReader = new Sentinel1OCNReader(this);
            if(isCompressed()) {
                throw new IOException("Compressed format is not supported for level-2");
            }
            OCNReader.addImageFile(new File(getBaseDir(), imgPath), name);
        }
    }

    @Override
    protected void addBands(final Product product) {

        String bandName;
        boolean real = true;
        Band lastRealBand = null;
        String unit;

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        for (Map.Entry<String, ImageIOFile> stringImageIOFileEntry : bandImageFileMap.entrySet()) {
            final ImageIOFile img = stringImageIOFileEntry.getValue();
            final String imgName = img.getName().toLowerCase();
            final MetadataElement bandMetadata = absRoot.getElement(imgBandMetadataMap.get(imgName));
            final String swath = bandMetadata.getAttributeString(AbstractMetadata.swath);
            final String pol = bandMetadata.getAttributeString(AbstractMetadata.polarization);
            final int width = bandMetadata.getAttributeInt(AbstractMetadata.num_samples_per_line);
            final int height = bandMetadata.getAttributeInt(AbstractMetadata.num_output_lines);

            String tpgPrefix = "";
            String suffix = pol;
            if (isSLC() && isTOPSAR()) {
                suffix = swath + '_' + pol;
                tpgPrefix = swath;
            }

            int numImages = img.getNumImages();
            if (isSLC()) {
                numImages *= 2; // real + imaginary
            }
            for (int i = 0; i < numImages; ++i) {

                if (isSLC()) {
                    for (int b = 0; b < img.getNumBands(); ++b) {
                        if (real) {
                            bandName = "i" + '_' + suffix;
                            unit = Unit.REAL;
                        } else {
                            bandName = "q" + '_' + suffix;
                            unit = Unit.IMAGINARY;
                        }

                        final Band band = new Band(bandName, ProductData.TYPE_INT16, width, height);
                        band.setUnit(unit);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));
                        AbstractMetadata.addBandToBandMap(bandMetadata, bandName);

                        if (real) {
                            lastRealBand = band;
                        } else {
                            ReaderUtils.createVirtualIntensityBand(product, lastRealBand, band, '_' + suffix);
                        }
                        real = !real;

                        // add tiepointgrids and geocoding for band
                        addTiePointGrids(band, imgName, tpgPrefix);

                        // reset to null so it doesn't adopt a geocoding from the bands
                        product.setGeoCoding(null);
                    }
                } else {
                    for (int b = 0; b < img.getNumBands(); ++b) {
                        bandName = "Amplitude" + '_' + suffix;
                        final Band band = new Band(bandName, ProductData.TYPE_INT32, width, height);
                        band.setUnit(Unit.AMPLITUDE);

                        product.addBand(band);
                        bandMap.put(band, new ImageIOFile.BandInfo(band, img, i, b));
                        AbstractMetadata.addBandToBandMap(bandMetadata, bandName);

                        // add tiepointgrids and geocoding for band
                        addTiePointGrids(band, imgName, tpgPrefix);
                    }
                }
            }
        }
    }

    @Override
    protected void addAbstractedMetadataHeader(final Product product, final MetadataElement root) throws IOException {

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);
        final MetadataElement origProdRoot = AbstractMetadata.addOriginalProductMetadata(product);

        Sentinel1Level1Directory.addManifestMetadata(product, absRoot, origProdRoot, true);
        acqMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        setSLC(absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE).equals("COMPLEX"));

        // get metadata for each band
        addBandAbstractedMetadata(product, absRoot, origProdRoot);
    }

    private void addBandAbstractedMetadata(final Product product, final MetadataElement absRoot,
                                           final MetadataElement origProdRoot) throws IOException {

        MetadataElement annotationElement = origProdRoot.getElement("annotation");
        if (annotationElement == null) {
            annotationElement = new MetadataElement("annotation");
            origProdRoot.addElement(annotationElement);
        }

        if (OCNReader != null) {
            // add netcdf metadata for OCN product
            OCNReader.addNetCDFMetadataAndBands(product, annotationElement);
        }
    }

    @Override
    protected void addGeoCoding(final Product product) {

        TiePointGrid latGrid = product.getTiePointGrid(OperatorUtils.TPG_LATITUDE);
        TiePointGrid lonGrid = product.getTiePointGrid(OperatorUtils.TPG_LONGITUDE);
        if (latGrid != null && lonGrid != null) {
            setLatLongMetadata(product, latGrid, lonGrid);
            return;
        }

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        final String acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        int numOfSubSwath;
        switch (acquisitionMode) {
            case "IW":
                numOfSubSwath = 3;
                break;
            case "EW":
                numOfSubSwath = 5;
                break;
            default:
                return;
        }

        String[] bandNames = product.getBandNames();
        Band firstSWBand = null, lastSWBand = null;
        boolean firstSWBandFound = false, lastSWBandFound = false;
        for (String bandName : bandNames) {
            if (!firstSWBandFound && bandName.contains(acquisitionMode + 1)) {
                firstSWBand = product.getBand(bandName);
                firstSWBandFound = true;
            }

            if (!lastSWBandFound && bandName.contains(acquisitionMode + numOfSubSwath)) {
                lastSWBand = product.getBand(bandName);
                lastSWBandFound = true;
            }
        }
        if (firstSWBand == null && lastSWBand == null)
            return;

        final GeoCoding firstSWBandGeoCoding = firstSWBand.getGeoCoding();
        final int firstSWBandHeight = firstSWBand.getRasterHeight();

        final GeoCoding lastSWBandGeoCoding = lastSWBand.getGeoCoding();
        final int lastSWBandWidth = lastSWBand.getRasterWidth();
        final int lastSWBandHeight = lastSWBand.getRasterHeight();

        final PixelPos ulPix = new PixelPos(0, 0);
        final PixelPos llPix = new PixelPos(0, firstSWBandHeight - 1);
        final GeoPos ulGeo = new GeoPos();
        final GeoPos llGeo = new GeoPos();
        firstSWBandGeoCoding.getGeoPos(ulPix, ulGeo);
        firstSWBandGeoCoding.getGeoPos(llPix, llGeo);

        final PixelPos urPix = new PixelPos(lastSWBandWidth - 1, 0);
        final PixelPos lrPix = new PixelPos(lastSWBandWidth - 1, lastSWBandHeight - 1);
        final GeoPos urGeo = new GeoPos();
        final GeoPos lrGeo = new GeoPos();
        lastSWBandGeoCoding.getGeoPos(urPix, urGeo);
        lastSWBandGeoCoding.getGeoPos(lrPix, lrGeo);

        final float[] latCorners = {ulGeo.getLat(), urGeo.getLat(), llGeo.getLat(), lrGeo.getLat()};
        final float[] lonCorners = {ulGeo.getLon(), urGeo.getLon(), llGeo.getLon(), lrGeo.getLon()};

        ReaderUtils.addGeoCoding(product, latCorners, lonCorners);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, ulGeo.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, ulGeo.getLon());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, urGeo.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, urGeo.getLon());

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, llGeo.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, llGeo.getLon());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, lrGeo.getLat());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lrGeo.getLon());
    }

    @Override
    protected void addTiePointGrids(final Product product) {
        // replaced by call to addTiePointGrids(band)
    }

    private static void addTiePointGrids(final Band band, final String imgXMLName, final String tpgPrefix) {

        final Product product = band.getProduct();
        String pre = "";
        if (!tpgPrefix.isEmpty())
            pre = tpgPrefix + '_';

        final TiePointGrid existingLatTPG = product.getTiePointGrid(pre + OperatorUtils.TPG_LATITUDE);
        final TiePointGrid existingLonTPG = product.getTiePointGrid(pre + OperatorUtils.TPG_LONGITUDE);
        if (existingLatTPG != null && existingLonTPG != null) {
            // reuse geocoding
            final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(existingLatTPG, existingLonTPG, Datum.WGS_84);
            band.setGeoCoding(tpGeoCoding);
            return;
        }

        final String annotation = FileUtils.exchangeExtension(imgXMLName, ".xml");
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        final MetadataElement annotationElem = origProdRoot.getElement("annotation");
        final MetadataElement imgElem = annotationElem.getElement(annotation);
        final MetadataElement productElem = imgElem.getElement("product");
        final MetadataElement geolocationGrid = productElem.getElement("geolocationGrid");
        final MetadataElement geolocationGridPointList = geolocationGrid.getElement("geolocationGridPointList");

        final MetadataElement[] geoGrid = geolocationGridPointList.getElements();

        final float[] latList = new float[geoGrid.length];
        final float[] lngList = new float[geoGrid.length];
        final float[] incidenceAngleList = new float[geoGrid.length];
        final float[] elevAngleList = new float[geoGrid.length];
        final float[] rangeTimeList = new float[geoGrid.length];
        final int[] x = new int[geoGrid.length];
        final int[] y = new int[geoGrid.length];

        int gridWidth = 0, gridHeight = 0;
        int i = 0;
        for (MetadataElement ggPoint : geoGrid) {
            latList[i] = (float) ggPoint.getAttributeDouble("latitude", 0);
            lngList[i] = (float) ggPoint.getAttributeDouble("longitude", 0);
            incidenceAngleList[i] = (float) ggPoint.getAttributeDouble("incidenceAngle", 0);
            elevAngleList[i] = (float) ggPoint.getAttributeDouble("elevationAngle", 0);
            rangeTimeList[i] = (float) (ggPoint.getAttributeDouble("slantRangeTime", 0) * Constants.oneBillion); // s to ns

            x[i] = (int) ggPoint.getAttributeDouble("pixel", 0);
            y[i] = (int) ggPoint.getAttributeDouble("line", 0);
            if (x[i] == 0) {
                if (gridWidth == 0)
                    gridWidth = i;
                ++gridHeight;
            }
            ++i;
        }

        final int newGridWidth = gridWidth;
        final int newGridHeight = gridHeight;
        final float[] newLatList = new float[newGridWidth * newGridHeight];
        final float[] newLonList = new float[newGridWidth * newGridHeight];
        final float[] newIncList = new float[newGridWidth * newGridHeight];
        final float[] newElevList = new float[newGridWidth * newGridHeight];
        final float[] newslrtList = new float[newGridWidth * newGridHeight];
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        final float subSamplingX = (float) sceneRasterWidth / (newGridWidth - 1);
        final float subSamplingY = (float) sceneRasterHeight / (newGridHeight - 1);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, latList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newLatList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, lngList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newLonList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, incidenceAngleList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newIncList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, elevAngleList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newElevList);

        getListInEvenlySpacedGrid(sceneRasterWidth, sceneRasterHeight, gridWidth, gridHeight, x, y, rangeTimeList,
                newGridWidth, newGridHeight, subSamplingX, subSamplingY, newslrtList);

        TiePointGrid latGrid = product.getTiePointGrid(pre + OperatorUtils.TPG_LATITUDE);
        if (latGrid == null) {
            latGrid = new TiePointGrid(pre + OperatorUtils.TPG_LATITUDE,
                    newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newLatList);
            latGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(latGrid);
        }

        TiePointGrid lonGrid = product.getTiePointGrid(pre + OperatorUtils.TPG_LONGITUDE);
        if (lonGrid == null) {
            lonGrid = new TiePointGrid(pre + OperatorUtils.TPG_LONGITUDE,
                    newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newLonList, TiePointGrid.DISCONT_AT_180);
            lonGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(lonGrid);
        }

        if (product.getTiePointGrid(pre + OperatorUtils.TPG_INCIDENT_ANGLE) == null) {
            final TiePointGrid incidentAngleGrid = new TiePointGrid(pre + OperatorUtils.TPG_INCIDENT_ANGLE,
                    newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newIncList);
            incidentAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(incidentAngleGrid);
        }

        if (product.getTiePointGrid(pre + OperatorUtils.TPG_ELEVATION_ANGLE) == null) {
            final TiePointGrid elevAngleGrid = new TiePointGrid(pre + OperatorUtils.TPG_ELEVATION_ANGLE,
                    newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newElevList);
            elevAngleGrid.setUnit(Unit.DEGREES);
            product.addTiePointGrid(elevAngleGrid);
        }

        if (product.getTiePointGrid(pre + OperatorUtils.TPG_SLANT_RANGE_TIME) == null) {
            final TiePointGrid slantRangeGrid = new TiePointGrid(pre + OperatorUtils.TPG_SLANT_RANGE_TIME,
                    newGridWidth, newGridHeight, 0.5f, 0.5f, subSamplingX, subSamplingY, newslrtList);
            slantRangeGrid.setUnit(Unit.NANOSECONDS);
            product.addTiePointGrid(slantRangeGrid);
        }

        final TiePointGeoCoding tpGeoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
        band.setGeoCoding(tpGeoCoding);

        //setLatLongMetadata(product, latGrid, lonGrid);
    }

    private static void setLatLongMetadata(Product product, TiePointGrid latGrid, TiePointGrid lonGrid) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);

        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_lat, latGrid.getPixelFloat(0, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_near_long, lonGrid.getPixelFloat(0, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_lat, latGrid.getPixelFloat(w, 0));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.first_far_long, lonGrid.getPixelFloat(w, 0));

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_lat, latGrid.getPixelFloat(0, h));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_near_long, lonGrid.getPixelFloat(0, h));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_lat, latGrid.getPixelFloat(w, h));
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.last_far_long, lonGrid.getPixelFloat(w, h));
    }

    private boolean isTOPSAR() {
        return acqMode.equals("IW") || acqMode.equals("EW");
    }

    @Override
    protected String getProductName() {
        String name = getBaseName();
        if (name.toUpperCase().endsWith(".SAFE"))
            return name.substring(0, name.length() - 5);
        else if (name.toUpperCase().endsWith(".ZIP"))
            return name.substring(0, name.length() - 4);
        return name;
    }

    protected String getProductType() {
        return "Level-2 OCN";
    }

    public Sentinel1OCNReader getOCNReader() {
        return OCNReader;
    }

    private static void getListInEvenlySpacedGrid(
            final int sceneRasterWidth, final int sceneRasterHeight, final int sourceGridWidth,
            final int sourceGridHeight, final int[] x, final int[] y, final float[] sourcePointList,
            final int targetGridWidth, final int targetGridHeight, final float subSamplingX, final float subSamplingY,
            final float[] targetPointList) {

        if (sourcePointList.length != sourceGridWidth * sourceGridHeight) {
            throw new IllegalArgumentException(
                    "Original tie point array size does not match 'sourceGridWidth' x 'sourceGridHeight'");
        }

        if (targetPointList.length != targetGridWidth * targetGridHeight) {
            throw new IllegalArgumentException(
                    "Target tie point array size does not match 'targetGridWidth' x 'targetGridHeight'");
        }

        int k = 0;
        for (int r = 0; r < targetGridHeight; r++) {

            float newY = r * subSamplingY;
            if (newY > sceneRasterHeight - 1) {
                newY = sceneRasterHeight - 1;
            }
            float oldY0 = 0, oldY1 = 0;
            int j0 = 0, j1 = 0;
            for (int rr = 1; rr < sourceGridHeight; rr++) {
                j0 = rr - 1;
                j1 = rr;
                oldY0 = y[j0 * sourceGridWidth];
                oldY1 = y[j1 * sourceGridWidth];
                if (oldY1 > newY) {
                    break;
                }
            }

            final float wj = (newY - oldY0) / (oldY1 - oldY0);

            for (int c = 0; c < targetGridWidth; c++) {

                float newX = c * subSamplingX;
                if (newX > sceneRasterWidth - 1) {
                    newX = sceneRasterWidth - 1;
                }
                float oldX0 = 0, oldX1 = 0;
                int i0 = 0, i1 = 0;
                for (int cc = 1; cc < sourceGridWidth; cc++) {
                    i0 = cc - 1;
                    i1 = cc;
                    oldX0 = x[i0];
                    oldX1 = x[i1];
                    if (oldX1 > newX) {
                        break;
                    }
                }
                final float wi = (newX - oldX0) / (oldX1 - oldX0);

                targetPointList[k++] = MathUtils.interpolate2D(wi, wj,
                        sourcePointList[i0 + j0 * sourceGridWidth],
                        sourcePointList[i1 + j0 * sourceGridWidth],
                        sourcePointList[i0 + j1 * sourceGridWidth],
                        sourcePointList[i1 + j1 * sourceGridWidth]);
            }
        }
    }

    public static ProductData.UTC getTime(final MetadataElement elem, final String tag) {

        String start = elem.getAttributeString(tag, AbstractMetadata.NO_METADATA_STRING);
        start = start.replace("T", "_");

        return AbstractMetadata.parseUTC(start, Sentinel1Constants.sentinelDateFormat);
    }

    @Override
    public Product createProduct() throws IOException {

        final Product product = new Product(getProductName(),
                getProductType(),
                sceneWidth, sceneHeight);

        addMetaData(product);
        findImages();
        addTiePointGrids(product); // empty

        addBands(product);
        addGeoCoding(product);

        product.setName(getProductName());
        //product.setProductType(getProductType());
        product.setDescription(getProductDescription());

        ReaderUtils.addMetadataProductSize(product);

        return product;
    }
}