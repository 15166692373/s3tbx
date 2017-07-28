package org.esa.s2tbx.fcc.trimming;

import org.esa.s2tbx.fcc.common.ForestCoverChangeConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.utils.matrix.IntMatrix;

import java.awt.Dimension;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jean Coravu
 */
public class DifferenceRegionTilesComputing extends AbstractRegionParallelComputing {
    private static final Logger logger = Logger.getLogger(DifferenceRegionTilesComputing.class.getName());

    private final Product differenceSegmentationProduct;
    private final Product currentSourceProduct;
    private final Product previousSourceProduct;
    private final IntMatrix unionMask;
    private final int[] sourceBandIndices;

    public DifferenceRegionTilesComputing(Product differenceSegmentationProduct, Product currentSourceProduct, Product previousSourceProduct,
                                          IntMatrix unionMask, int[] sourceBandIndices, Dimension tileSize) {

        super(differenceSegmentationProduct.getSceneRasterWidth(), differenceSegmentationProduct.getSceneRasterHeight(), tileSize.width, tileSize.height);

        this.differenceSegmentationProduct = differenceSegmentationProduct;
        this.currentSourceProduct = currentSourceProduct;
        this.previousSourceProduct = previousSourceProduct;
        this.unionMask = unionMask;
        this.sourceBandIndices = sourceBandIndices;
    }

    @Override
    protected void runTile(int tileLeftX, int tileTopY, int tileWidth, int tileHeight, int localRowIndex, int localColumnIndex) throws IOException, IllegalAccessException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Difference trimming statistics for tile region: row index: "+ localRowIndex+", column index: "+localColumnIndex+", bounds [x=" + tileLeftX+", y="+tileTopY+", width="+tileWidth+", height="+tileHeight+"]");
        }

        Band firstCurrentBand = this.currentSourceProduct.getBandAt(this.sourceBandIndices[0]);
        Band secondCurrentBand = this.currentSourceProduct.getBandAt(this.sourceBandIndices[1]);
        Band thirdCurrentBand = this.currentSourceProduct.getBandAt(this.sourceBandIndices[2]);

        Band firstPreviousBand = this.previousSourceProduct.getBandAt(this.sourceBandIndices[0]);
        Band secondPreviousBand = this.previousSourceProduct.getBandAt(this.sourceBandIndices[1]);
        Band thirdPreviousBand = this.previousSourceProduct.getBandAt(this.sourceBandIndices[2]);

        Band segmentationBand = this.differenceSegmentationProduct.getBandAt(0);
//        Band unionBand = this.unionMask.getBandAt(0);

        int tileBottomY = tileTopY + tileHeight;
        int tileRightX = tileLeftX + tileWidth;
        for (int y = tileTopY; y < tileBottomY; y++) {
            for (int x = tileLeftX; x < tileRightX; x++) {
                //if (unionBand.getSampleFloat(x, y) != ForestCoverChangeConstants.NO_DATA_VALUE) {
                if (this.unionMask.getValueAt(y, x) != ForestCoverChangeConstants.NO_DATA_VALUE) {
                    int segmentationPixelValue = segmentationBand.getSampleInt(x, y);

//                    float a = firstCurrentBand.getSampleFloat(x, y) - firstPreviousBand.getSampleFloat(x, y);
//                    float b = secondCurrentBand.getSampleFloat(x, y) - secondPreviousBand.getSampleFloat(x, y);
//                    float c = thirdCurrentBand.getSampleFloat(x, y) - thirdPreviousBand.getSampleFloat(x, y);

                    //TODO Jean remove
                    float a = firstCurrentBand.getSampleFloat(x, y);
                    a -= firstPreviousBand.getSampleFloat(x, y);

                    float b = secondCurrentBand.getSampleFloat(x, y);
                    b -= secondPreviousBand.getSampleFloat(x, y);

                    float c = thirdCurrentBand.getSampleFloat(x, y);
                    c -= thirdPreviousBand.getSampleFloat(x, y);

                    addPixelValuesBands(segmentationPixelValue, a, b, c);
                }
            }
        }
    }
}
