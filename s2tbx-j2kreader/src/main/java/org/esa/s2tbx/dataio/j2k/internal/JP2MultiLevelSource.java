package org.esa.s2tbx.dataio.j2k.internal;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.s2tbx.dataio.jp2.TileLayout;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.jai.ImageManager;

import javax.media.jai.*;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * A single banded multi-level image source for JP2 files.
 *
 * @author  Cosmin Cara
 */
public class JP2MultiLevelSource extends AbstractMultiLevelSource {

    private final TileLayout tileLayout;
    private final File sourceFile;
    private final File cacheFolder;
    private final int dataType;
    private final Logger logger;
    private final int bandIndex;

    /**
     * Constructs an instance of a single band multi-level image source
     *
     * @param jp2File       The original (i.e. compressed) JP2 file
     * @param cacheFolder   The cache (temporary) folder
     * @param bandIndex     The destination Product band for which the image source is created
     * @param imageWidth    The width of the scene image
     * @param imageHeight   The height of the scene image
     * @param tileWidth     The width of a JP2 tile composing the scene image
     * @param tileHeight    The height of a JP2 tile composing the scene image
     * @param numTilesX     The number of JP2 tiles in a row
     * @param numTilesY     The number of JP2 tiles in a column
     * @param levels        The number of resolutions found in the JP2 file
     * @param dataType      The pixel data type
     * @param geoCoding     (optional) The geocoding found (if any) in the JP2 header
     */
    public JP2MultiLevelSource(File jp2File, File cacheFolder, int bandIndex, int imageWidth, int imageHeight,
                               int tileWidth, int tileHeight, int numTilesX, int numTilesY, int levels, int dataType,
                               GeoCoding geoCoding) {
        super(new DefaultMultiLevelModel(levels,
                geoCoding == null ? new AffineTransform() : ImageManager.getImageToModelTransform(geoCoding),
                imageWidth, imageHeight));
        sourceFile = jp2File;
        this.cacheFolder = cacheFolder;
        this.dataType = dataType;
        logger = Logger.getLogger(JP2MultiLevelSource.class.getName());
        tileLayout = new TileLayout(imageWidth, imageHeight, tileWidth, tileHeight, numTilesX, numTilesY, levels);
        this.bandIndex = bandIndex;
    }

    /**
     * Creates a planar image corresponding of a tile identified by row and column, at the specified resolution.
     *
     * @param row       The row of the tile (0-based)
     * @param col       The column of the tile (0-based)
     * @param level     The resolution level (0 = highest)
     */
    protected PlanarImage createTileImage(int row, int col, int level) throws IOException {
        return JP2TileOpImage.create(sourceFile, cacheFolder, bandIndex, row, col, tileLayout, getModel(), dataType, level);
    }

    @Override
    protected RenderedImage createImage(int level) {
        final List<RenderedImage> tileImages = Collections.synchronizedList(new ArrayList<>(tileLayout.numXTiles * tileLayout.numYTiles));
        TileLayout layout = tileLayout;
        double factorX = 1.0 / Math.pow(2, level);
        double factorY = 1.0 / Math.pow(2, level);
        for (int x = 0; x < tileLayout.numYTiles; x++) {
            for (int y = 0; y < tileLayout.numXTiles; y++) {
                PlanarImage opImage;
                try {
                    opImage = createTileImage(x, y, level);
                    if (opImage != null) {
                        opImage = TranslateDescriptor.create(opImage,
                                                             (float) (y * layout.tileWidth * factorX),
                                                             (float) (x * layout.tileHeight * factorY),
                                                             Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                                             null);
                    }
                } catch (IOException ex) {
                    opImage = ConstantDescriptor.create((float)layout.tileWidth, (float)layout.tileHeight, new Number[] { 0 }, null);
                }
                tileImages.add(opImage);
            }
        }
        if (tileImages.isEmpty()) {
            logger.warning("No tile images for mosaic");
            return null;
        }

        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setMinX(0);
        imageLayout.setMinY(0);
        imageLayout.setTileWidth(JAI.getDefaultTileSize().width);
        imageLayout.setTileHeight(JAI.getDefaultTileSize().height);
        imageLayout.setTileGridXOffset(0);
        imageLayout.setTileGridYOffset(0);

        RenderedOp mosaicOp = MosaicDescriptor.create(tileImages.toArray(new RenderedImage[tileImages.size()]),
                MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                null, null, null, null,
                new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));

        int fittingRectWidth = scaleValue(tileLayout.width, level);
        int fittingRectHeight = scaleValue(tileLayout.height, level);

        Rectangle fitRect = new Rectangle(0, 0, fittingRectWidth, fittingRectHeight);
        final Rectangle destBounds = DefaultMultiLevelSource.getLevelImageBounds(fitRect, Math.pow(2.0, level));

        BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);

        if (mosaicOp.getWidth() < destBounds.width || mosaicOp.getHeight() < destBounds.height) {
            int rightPad = destBounds.width - mosaicOp.getWidth();
            int bottomPad = destBounds.height - mosaicOp.getHeight();

            mosaicOp = BorderDescriptor.create(mosaicOp, 0, rightPad, 0, bottomPad, borderExtender, null);
        }

        return mosaicOp;
    }

    @Override
    public synchronized void reset() {
        super.reset();
    }

    private int scaleValue(int source, int level) {
        int size = source >> level;
        int sizeTest = size << level;
        if (sizeTest < source) {
            size++;
        }
        return size;
    }
}
