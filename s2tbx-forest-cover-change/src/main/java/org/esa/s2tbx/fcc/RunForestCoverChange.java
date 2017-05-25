package org.esa.s2tbx.fcc;

import org.esa.s2tbx.fcc.intern.BandsExtractor;
import org.esa.s2tbx.landcover.dataio.CCILandCoverModelDescriptor;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;

import java.io.File;
import org.esa.snap.landcover.dataio.LandCoverModelRegistry;

/**
 * @author Jean Coravu
 */
public class RunForestCoverChange {

    public static void main(String arg[]) {
        try {
            Class<?> sentinelReaderPlugInClass = Class.forName("org.esa.snap.core.dataio.dimap.DimapProductReaderPlugIn");
            ProductReaderPlugIn productReaderPlugIn = (ProductReaderPlugIn)sentinelReaderPlugInClass.newInstance();

            File file1 = new File("\\\\cv-dev-srv01\\Satellite_Imagery\\Forest_Mapping\\S2A_USER_MTD_SAFL2A_PDMC_20151207T180211_R108_V20151207T103733_20151207T103733_resampled.dim");
            Product firstInputProduct = productReaderPlugIn.createReaderInstance().readProductNodes(file1, null);

            File file2 = new File("\\\\cv-dev-srv01\\Satellite_Imagery\\Forest_Mapping\\S2A_USER_MTD_SAFL2A_PDMC_20160408T183838_R108_V20150829T103028_20150829T103028_resampled.dim");
            Product secondInputProduct = productReaderPlugIn.createReaderInstance().readProductNodes(file2, null);

            LandCoverModelRegistry landCoverModelRegistry = LandCoverModelRegistry.getInstance();
            landCoverModelRegistry.addDescriptor(new CCILandCoverModelDescriptor());

            System.out.println("firstInputProduct="+firstInputProduct);
            System.out.println("secondInputProduct="+secondInputProduct);

            int[] indexes = new int[] {2, 3, 7, 11};

            Product firstProduct = BandsExtractor.generateBandsExtractor(firstInputProduct, indexes);
            firstProduct = BandsExtractor.resampleAllBands(firstProduct);

            Product secondProduct = BandsExtractor.generateBandsExtractor(secondInputProduct, indexes);
            secondProduct = BandsExtractor.resampleAllBands(secondProduct);

            Product bandsDifferenceProduct = BandsExtractor.generateBandsDifference(firstProduct, secondProduct);

            System.out.println("firstProduct="+firstProduct);
            System.out.println("secondProduct="+secondProduct);
            System.out.println("bandsDifferenceProduct="+bandsDifferenceProduct);

            Product bandsCompositingProduct = BandsExtractor.generateBandsCompositing(firstProduct, secondProduct, bandsDifferenceProduct);
            System.out.println("bandsCompositingProduct="+bandsCompositingProduct);

            Product ndviFirstProduct = BandsExtractor.computeNDVIBands(firstProduct, "B3", 1.0f, "B4", 1.0f);
            Product ndviSecondProduct = BandsExtractor.computeNDVIBands(secondProduct, "B3", 1.0f, "B4", 1.0f);

            Product ndwiFirstProduct = BandsExtractor.computeNDWIBands(firstProduct, "B3", 1.0f, "B4", 1.0f);
            Product ndwiSecondProduct = BandsExtractor.computeNDWIBands(secondProduct, "B3", 1.0f, "B4", 1.0f);

            Product landCoverProduct = BandsExtractor.computeObjectSelection(firstProduct);

            System.out.println("ndviFirstProduct="+ndviFirstProduct);
            System.out.println("ndviSecondProduct="+ndviSecondProduct);
            System.out.println("ndwiFirstProduct="+ndwiFirstProduct);
            System.out.println("ndwiSecondProduct="+ndwiSecondProduct);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
