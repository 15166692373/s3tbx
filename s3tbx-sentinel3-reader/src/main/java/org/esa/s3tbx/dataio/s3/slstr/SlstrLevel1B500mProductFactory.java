package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.ProductUtils;

import java.awt.image.RenderedImage;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class SlstrLevel1B500mProductFactory extends SlstrLevel1ProductFactory {

    public SlstrLevel1B500mProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected Product findMasterProduct() {
        final List<Product> productList = getOpenProductList();
        Product masterProduct = new Product("dummy", "type", 1, 1);
        for (Product product : productList) {
            if (product.getSceneRasterWidth() > masterProduct.getSceneRasterWidth() &&
                    product.getSceneRasterHeight() > masterProduct.getSceneRasterHeight() &&
                    !product.getName().contains("flags") &&
                    !product.getName().endsWith("in") &&
                    !product.getName().endsWith("io")
                    ) {
                masterProduct = product;
            }
        }
        return masterProduct;
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        final int sourceBandNameLength = sourceBandName.length();
        String gridIndex = sourceBandName;
        if(sourceBandNameLength > 1) {
            gridIndex = sourceBandName.substring(sourceBandNameLength - 2);
        }
        final Double sourceStartOffset = getStartOffset(gridIndex);
        final Double sourceTrackOffset = getTrackOffset(gridIndex);
        if (sourceStartOffset != null && sourceTrackOffset != null) {
            final short[] sourceResolutions = getResolutions(gridIndex);
            if (gridIndex.startsWith("t")) {
                return copyTiePointGrid(sourceBand, targetProduct, sourceStartOffset, sourceTrackOffset, sourceResolutions);
            } else {
                final Band targetBand = new Band(sourceBandName, sourceBand.getDataType(),
                                                 targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight());
                targetProduct.addBand(targetBand);
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                final float[] offsets = getOffsets(sourceStartOffset, sourceTrackOffset, sourceResolutions);
                final RenderedImage sourceImage = createSourceImage(masterProduct, sourceBand, offsets, targetBand,
                                                                    sourceResolutions);
                targetBand.setSourceImage(sourceImage);
                return targetBand;
            }
        }
        return sourceBand;
    }

    @Override
    protected void configureDescription(Band sourceBand, RasterDataNode targetNode) {
    }

    @Override
    protected void changeTargetProductName(Product targetProduct) {
        targetProduct.setName(targetProduct.getName() + "_500m");
    }

    @Override
    protected void setSceneTransforms(Product product) {
    }

    @Override
    protected void setBandGeoCodings(Product product) {
    }
}
