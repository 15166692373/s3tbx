package org.esa.s2tbx.fcc.trimming;

import it.unimi.dsi.fastutil.ints.IntSet;
import org.esa.s2tbx.fcc.common.ForestCoverChangeConstants;
import org.esa.snap.utils.AbstractImageTilesParallelComputing;
import org.esa.snap.utils.matrix.IntMatrix;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jean Coravu
 */
public class UnionMasksTilesComputing extends AbstractImageTilesParallelComputing {
    private static final Logger logger = Logger.getLogger(UnionMasksTilesComputing.class.getName());

    private final IntMatrix currentSegmentationMatrix;
    private final IntMatrix previousSegmentationMatrix;
    private final IntSet currentSegmentationTrimmingRegionKeys;
    private final IntSet previousSegmentationTrimmingRegionKeys;
    private final IntMatrix resultMatrix;

    public UnionMasksTilesComputing(IntMatrix currentSegmentationMatrix, IntMatrix previousSegmentationMatrix,
                                    IntSet currentSegmentationTrimmingRegionKeys, IntSet previousSegmentationTrimmingRegionKeys,
                                    int tileWidth, int tileHeight) {

        super(currentSegmentationMatrix.getColumnCount(), currentSegmentationMatrix.getRowCount(), tileWidth, tileHeight);

        this.currentSegmentationMatrix = currentSegmentationMatrix;
        this.previousSegmentationMatrix = previousSegmentationMatrix;
        this.currentSegmentationTrimmingRegionKeys = currentSegmentationTrimmingRegionKeys;
        this.previousSegmentationTrimmingRegionKeys = previousSegmentationTrimmingRegionKeys;

        this.resultMatrix = new IntMatrix(currentSegmentationMatrix.getRowCount(), currentSegmentationMatrix.getColumnCount());
    }

    @Override
    protected void runTile(int tileLeftX, int tileTopY, int tileWidth, int tileHeight, int localRowIndex, int localColumnIndex)
                           throws IOException, IllegalAccessException, InterruptedException {

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Union masks for tile region: row index: "+ localRowIndex+", column index: "+localColumnIndex+", bounds [x=" + tileLeftX+", y="+tileTopY+", width="+tileWidth+", height="+tileHeight+"]");
        }

        int tileBottomY = tileTopY + tileHeight;
        int tileRightX = tileLeftX + tileWidth;
        for (int y = tileTopY; y < tileBottomY; y++) {
            for (int x = tileLeftX; x < tileRightX; x++) {
                // get the pixel value from the previous segmentation
                int segmentationPixelValue = this.previousSegmentationMatrix.getValueAt(y, x);

                // check if the pixel value from the previous segmentation exists among the trimming region keys of the previous segmentation
                if (this.previousSegmentationTrimmingRegionKeys.contains(segmentationPixelValue)) {
                    // the pixel value from the previous segmentation exists among the trimming region keys of the previous segmentation

                    // get the pixel value from the current segmentation
                    segmentationPixelValue = this.currentSegmentationMatrix.getValueAt(y, x);

                    // check if the pixel value from the current segmentation exists among the trimming region keys of the current segmentation
                    if (this.currentSegmentationTrimmingRegionKeys.contains(segmentationPixelValue)) {
                        // the pixel value from the current segmentation exists among the trimming region keys of the current segmentation
                        segmentationPixelValue = ForestCoverChangeConstants.COMMON_VALUE;//255;
                    } else {
                        segmentationPixelValue = ForestCoverChangeConstants.PREVIOUS_VALUE;//50;
                    }
                } else {
                    // the pixel value from the previous segmentation does not exist among the trimming region keys of the previous segmentation

                    // get the pixel value from the current segmentation
                    segmentationPixelValue = this.currentSegmentationMatrix.getValueAt(y, x);

                    // check if the pixel value from the current segmentation exists among the trimming region keys of the current segmentation
                    if (this.currentSegmentationTrimmingRegionKeys.contains(segmentationPixelValue)) {
                        // the pixel value from the current segmentation exists among the trimming region keys of the current segmentation
                        segmentationPixelValue = ForestCoverChangeConstants.CURRENT_VALUE;//100;
                    } else {
                        segmentationPixelValue = ForestCoverChangeConstants.NO_DATA_VALUE; // 0
                    }
                }

                synchronized (this.resultMatrix) {
                    this.resultMatrix.setValueAt(y, x, segmentationPixelValue);
                }
            }
        }
    }

    public IntMatrix runTilesInParallel(int threadCount, Executor threadPool) throws Exception {
        super.executeInParallel(threadCount, threadPool);

        return this.resultMatrix;
    }
}
