package org.esa.s2tbx.fcc.intern;

import java.util.*;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.ProductUtils;

/**
 * @author Razvan Dumitrascu
 * @since 5.0.6
 */
public class BandsExtractor {

    public static Product resampleAllBands(Product sourceProduct) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("targetWidth", sourceProduct.getSceneRasterWidth());
        parameters.put("targetHeight", sourceProduct.getSceneRasterHeight());
        Product targetProduct = GPF.createProduct("Resample", parameters, sourceProduct);
        targetProduct.setName(sourceProduct.getName());
        return targetProduct;
    }

    public static Product generateBandsDifference(Product firstSourceProduct, Product secondSourceProduct) {
        Product[] products = new Product[] {firstSourceProduct, secondSourceProduct};
        Map<String, Object> parameters = new HashMap<>();
        return GPF.createProduct("BandsDifferenceOp", parameters, products, null);
    }

    public static Product generateBandsExtractor(Product firstSourceProduct, int[] indexes) {
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Product> sourceProducts = new HashMap<>();
        sourceProducts.put("sourceProduct", firstSourceProduct);
        parameters.put("indexes", indexes);
        return GPF.createProduct("BandsExtractorOp", parameters, sourceProducts, null);
    }

    public static Product generateBandsCompositing(Product firstSourceProduct, Product secondSourceProduct, Product thirdSourceProduct) {
        Product targetProduct = new Product("BandsCompositing", firstSourceProduct.getProductType(),
                                            firstSourceProduct.getSceneRasterWidth(), firstSourceProduct.getSceneRasterHeight());

        copyBands(firstSourceProduct, targetProduct, "first");
        copyBands(secondSourceProduct, targetProduct, "second");
        copyBands(thirdSourceProduct, targetProduct, "third");

        return targetProduct;
    }

    private static void copyBands(Product sourceProduct, Product targetProduct, String prefixTargetBandNames) {
        int bandCount = sourceProduct.getBandGroup().getNodeCount();
        for (int i=0; i<bandCount; i++) {
            Band sourceBand = sourceProduct.getBandAt(i);
            String sourceBandName = sourceBand.getName();
            String targetBandName = prefixTargetBandNames + sourceBandName;
            ProductUtils.copyBand(sourceBandName, sourceProduct, targetBandName, targetProduct, true);

            Band targetBand = targetProduct.getBand(sourceBandName);
            ProductUtils.copyGeoCoding(sourceBand, targetBand);
        }
    }

    public static Product computeNDVIBands(Product sourceProduct, String redSourceBand, float redFactor, String nirSourceBand, float nirFactor) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("redFactor", redFactor);
        parameters.put("redSourceBand", redSourceBand);
        parameters.put("nirFactor", nirFactor);
        parameters.put("nirSourceBand", nirSourceBand);
        return GPF.createProduct("NdviOp", parameters, sourceProduct);
    }

    public static Product computeNDWIBands(Product sourceProduct, String mirSourceBand, float mirFactor, String nirSourceBand, float nirFactor) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("mirFactor", mirFactor);
        parameters.put("mirSourceBand", mirSourceBand);
        parameters.put("nirFactor", nirFactor);
        parameters.put("nirSourceBand", nirSourceBand);
        return GPF.createProduct("NdwiOp", parameters, sourceProduct);
    }
}
