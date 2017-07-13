package org.esa.s2tbx.fcc.trimming;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.esa.s2tbx.fcc.common.AveragePixelsSourceBands;
import org.esa.s2tbx.fcc.common.ForestCoverChangeConstans;
import org.esa.s2tbx.fcc.common.PixelSourceBands;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.utils.AbstractImageTilesParallelComputing;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jean Coravu
 */
public class DifferenceRegionComputingHelper extends AbstractImageTilesParallelComputing {
    private static final Logger logger = Logger.getLogger(DifferenceRegionComputingHelper.class.getName());

    private final Product differenceSegmentationProduct;
    private final Product currentSourceProduct;
    private final Product previousSourceProduct;
    private final Product unionMask;
    private final int[] sourceBandIndices;

    private final Int2ObjectMap<AveragePixelsSourceBands> validRegionsMap;

    public DifferenceRegionComputingHelper(Product differenceSegmentationProduct, Product currentSourceProduct, Product previousSourceProduct,
                                    Product unionMask, int[] sourceBandIndices, int tileWidth, int tileHeight) {

        super(differenceSegmentationProduct.getSceneRasterWidth(), differenceSegmentationProduct.getSceneRasterHeight(), tileWidth, tileHeight);

        this.differenceSegmentationProduct = differenceSegmentationProduct;
        this.currentSourceProduct = currentSourceProduct;
        this.previousSourceProduct = previousSourceProduct;
        this.unionMask = unionMask;
        this.sourceBandIndices = sourceBandIndices;

        this.validRegionsMap = new Int2ObjectLinkedOpenHashMap<>();
    }

    @Override
    protected void runTile(int tileLeftX, int tileTopY, int tileWidth, int tileHeight, int localRowIndex, int localColumnIndex) throws IOException, IllegalAccessException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Trimming statistics for tile region: row index: "+ localRowIndex+", column index: "+localColumnIndex+", bounds [x=" + tileLeftX+", y="+tileTopY+", width="+tileWidth+", height="+tileHeight+"]");
        }

        Band firstCurrentBand = this.currentSourceProduct.getBandAt(this.sourceBandIndices[0]);
        Band secondCurrentBand = this.currentSourceProduct.getBandAt(this.sourceBandIndices[1]);
        Band thirdCurrentBand = this.currentSourceProduct.getBandAt(this.sourceBandIndices[2]);

        Band firstPreviousBand = this.previousSourceProduct.getBandAt(this.sourceBandIndices[0]);
        Band secondPreviousBand = this.previousSourceProduct.getBandAt(this.sourceBandIndices[1]);
        Band thirdPreviousBand = this.previousSourceProduct.getBandAt(this.sourceBandIndices[2]);

        Band segmentationBand = this.differenceSegmentationProduct.getBandAt(0);
        Band unionBand = this.unionMask.getBandAt(0);

        int tileBottomY = tileTopY + tileHeight;
        int tileRightX = tileLeftX + tileWidth;
        for (int y = tileTopY; y < tileBottomY; y++) {
            for (int x = tileLeftX; x < tileRightX; x++) {
                if (unionBand.getSampleFloat(x, y) != ForestCoverChangeConstans.NO_DATA_VALUE) {
                    int segmentationPixelValue = segmentationBand.getSampleInt(x, y);

                    float a = firstCurrentBand.getSampleFloat(x, y) - firstPreviousBand.getSampleFloat(x, y);
                    float b = secondCurrentBand.getSampleFloat(x, y) - secondPreviousBand.getSampleFloat(x, y);
                    float c = thirdCurrentBand.getSampleFloat(x, y) - thirdPreviousBand.getSampleFloat(x, y);

                    synchronized (this.validRegionsMap) {
                        AveragePixelsSourceBands value = this.validRegionsMap.get(segmentationPixelValue);
                        if (value == null) {
                            value = new AveragePixelsSourceBands();
                            this.validRegionsMap.put(segmentationPixelValue, value);
                        }
                        value.addPixelValuesBands(a, b, c);
                    }
                }
            }
        }
    }

    private void doClose() {
        ObjectIterator<AveragePixelsSourceBands> it = this.validRegionsMap.values().iterator();
        while (it.hasNext()) {
            AveragePixelsSourceBands value = it.next();
            WeakReference<AveragePixelsSourceBands> reference = new WeakReference<>(value);
            reference.clear();
        }
        this.validRegionsMap.clear();
    }

    public IntSet computeRegionsInParallel(int threadCount, Executor threadPool) throws Exception {
        super.executeInParallel(threadCount, threadPool);

        Int2ObjectMap<PixelSourceBands> differenceRegionsTrimming = TrimmingHelper.computeStatisticsPerRegion(this.validRegionsMap);

        doClose();

        IntSet differenceTrimmingSet = TrimmingHelper.doTrimming(threadCount, threadPool, differenceRegionsTrimming);

        ObjectIterator<PixelSourceBands> it = differenceRegionsTrimming.values().iterator();
        while (it.hasNext()) {
            PixelSourceBands value = it.next();
            WeakReference<PixelSourceBands> reference = new WeakReference<PixelSourceBands>(value);
            reference.clear();
        }
        differenceRegionsTrimming.clear();

        return differenceTrimmingSet;
    }
}
