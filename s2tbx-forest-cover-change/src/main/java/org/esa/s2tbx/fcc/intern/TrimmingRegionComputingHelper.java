package org.esa.s2tbx.fcc.intern;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.utils.AbstractImageTilesHelper;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jean Coravu
 */
public class TrimmingRegionComputingHelper extends AbstractImageTilesHelper {
    private static final Logger logger = Logger.getLogger(TrimmingRegionComputingHelper.class.getName());

    private final Product segmentationSourceProduct;
    private final Product sourceProduct;
    private final int[] sourceBandIndices;

    private final Int2ObjectMap<AveragePixelsSourceBands> validRegionsMap;

    TrimmingRegionComputingHelper(Product segmentationSourceProduct, Product sourceProduct, int[] sourceBandIndices, int imageWidth, int imageHeight, int tileWidth, int tileHeight) {
        super(imageWidth, imageHeight, tileWidth, tileHeight);

        this.segmentationSourceProduct = segmentationSourceProduct;
        this.sourceProduct = sourceProduct;
        this.sourceBandIndices = sourceBandIndices;

        this.validRegionsMap = new Int2ObjectLinkedOpenHashMap<>();
    }

    @Override
    protected void runTile(int tileLeftX, int tileTopY, int tileWidth, int tileHeight, int localRowIndex, int localColumnIndex) throws IOException, IllegalAccessException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Compute trimming statistics for tile region: bounds [x=" + tileLeftX+", y="+tileTopY+", width="+tileWidth+", height="+tileHeight+"], row index: "+ localRowIndex+", column index: "+localColumnIndex);
        }

        Band firstBand = this.sourceProduct.getBandAt(this.sourceBandIndices[0]);
        Band secondBand = this.sourceProduct.getBandAt(this.sourceBandIndices[1]);
        Band thirdBand = this.sourceProduct.getBandAt(this.sourceBandIndices[2]);

        Band segmentationBand = this.segmentationSourceProduct.getBandAt(0);

        int tileBottomY = tileTopY + tileHeight;
        int tileRightX = tileLeftX + tileWidth;
        for (int y = tileTopY; y < tileBottomY; y++) {
            for (int x = tileLeftX; x < tileRightX; x++) {
                int segmentationPixelValue = segmentationBand.getSampleInt(x, y);
                if (segmentationPixelValue != ForestCoverChangeConstans.NO_DATA_VALUE) {
                    synchronized (this.validRegionsMap) {
                        AveragePixelsSourceBands value = this.validRegionsMap.get(segmentationPixelValue);
                        if (value == null) {
                            value = new AveragePixelsSourceBands();
                            this.validRegionsMap.put(segmentationPixelValue, value);
                        }
                        value.addPixelValuesBands(firstBand.getSampleFloat(x, y), secondBand.getSampleFloat(x, y), thirdBand.getSampleFloat(x, y));
                    }
                }
            }
        }
    }

    Int2ObjectMap<AveragePixelsSourceBands> computeRegionsUsingThreads(int threadCount, Executor threadPool) throws IllegalAccessException, IOException, InterruptedException {
        super.executeInParallel(threadCount, threadPool);

        return this.validRegionsMap;
    }
}
