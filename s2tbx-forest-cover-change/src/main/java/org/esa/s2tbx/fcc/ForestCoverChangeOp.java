package org.esa.s2tbx.fcc;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.esa.s2tbx.fcc.annotation.ParameterGroup;
import org.esa.s2tbx.fcc.common.AveragePixelsSourceBands;
import org.esa.s2tbx.fcc.common.BandsExtractorOp;
import org.esa.s2tbx.fcc.common.PixelSourceBands;
import org.esa.s2tbx.fcc.trimming.*;
import org.esa.s2tbx.fcc.common.ForestCoverChangeConstans;
import org.esa.s2tbx.grm.GenericRegionMergingOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.internal.OperatorExecutor;
import org.esa.snap.core.util.ProductUtils;

import javax.media.jai.JAI;
import java.awt.Dimension;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Razvan Dumitrascu
 * @author Jean Coravu
 * @since 5.0.6
 */
@OperatorMetadata(
        alias = "ForestCoverChangeOp",
        version="1.0",
        category = "Raster",
        description = "Generates Forest Cover Change product from L2a Sentinel 2 products ",
        authors = "Razvan Dumitrascu, Jean Coravu",
        copyright = "Copyright (C) 2017 by CS ROMANIA")
public class ForestCoverChangeOp extends Operator {
    private static final Logger logger = Logger.getLogger(ForestCoverChangeOp.class.getName());

    @SourceProduct(alias = "Current Source Product", description = "The source product to be modified.")
    private Product currentSourceProduct;
    @SourceProduct(alias = "Previous Source Product", description = "The source product to be modified.")
    private Product previousSourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "95.0", label = "Forest cover percentage", itemAlias = "percentage", description = "Specifies the percentage of forest cover per segment")
    private float forestCoverPercentage;

    @ParameterGroup(alias = "Segmentation")
    @Parameter(label = "Merging cost criterion",
            defaultValue = GenericRegionMergingOp.BAATZ_SCHAPE_MERGING_COST_CRITERION,
            description = "The method to compute the region merging.",
            valueSet = {GenericRegionMergingOp.SPRING_MERGING_COST_CRITERION, GenericRegionMergingOp.BAATZ_SCHAPE_MERGING_COST_CRITERION, GenericRegionMergingOp.FULL_LANDA_SCHEDULE_MERGING_COST_CRITERION})
    private String mergingCostCriterion;

    @ParameterGroup(alias = "Segmentation")
    @Parameter(label = "Region merging criterion",
            defaultValue = GenericRegionMergingOp.LOCAL_MUTUAL_BEST_FITTING_REGION_MERGING_CRITERION,
            description = "The method to check the region merging.",
            valueSet = {GenericRegionMergingOp.BEST_FITTING_REGION_MERGING_CRITERION, GenericRegionMergingOp.LOCAL_MUTUAL_BEST_FITTING_REGION_MERGING_CRITERION})
    private String regionMergingCriterion;

    @ParameterGroup(alias = "Segmentation")
    @Parameter(label = "Total iterations",
            defaultValue = "10",
            description = "The total number of iterations.")
    private int totalIterationsForSecondSegmentation;

    @ParameterGroup(alias = "Segmentation")
    @Parameter(label = "Threshold", defaultValue = "5.0", description = "The threshold.")
    private float threshold;

    @ParameterGroup(alias = "Segmentation")
    @Parameter(label = "Spectral weight", defaultValue = "0.5", description = "The spectral weight.")
    private float spectralWeight;

    @ParameterGroup(alias = "Segmentation")
    @Parameter(label = "Shape weight", defaultValue = "0.5" , description = "The shape weight.")
    private float shapeWeight;

    public ForestCoverChangeOp() {
    }

    @Override
    public void initialize() throws OperatorException {
        int sceneWidth = this.currentSourceProduct.getSceneRasterWidth();
        int sceneHeight = this.currentSourceProduct.getSceneRasterHeight();
        Dimension tileSize = JAI.getDefaultTileSize();

        this.targetProduct = new Product("forestCoverChange", this.currentSourceProduct.getProductType(), sceneWidth, sceneHeight);
        this.targetProduct.setPreferredTileSize(tileSize);
        ProductUtils.copyGeoCoding(this.currentSourceProduct, this.targetProduct);
        Band targetBand = new Band("band_1", ProductData.TYPE_INT32, sceneWidth, sceneHeight);
        this.targetProduct.addBand(targetBand);

        //TODO Jean remove
        Logger logger = Logger.getLogger("org.esa.s2tbx.fcc");
        logger.setLevel(Level.FINE);
    }

//    @Override
    public void doExecuteNew(ProgressMonitor pm) throws OperatorException {
        long startTime = System.currentTimeMillis();

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Start Forest Cover Change: imageWidth: "+this.targetProduct.getSceneRasterWidth()+", imageHeight: "+this.targetProduct.getSceneRasterHeight() + ", start time: " + new Date(startTime));
        }

        // reset the source inmage of the target product
        this.targetProduct.getBandAt(0).setSourceImage(null);

        String[] sourceBandNames = new String[] {"B4", "B8", "B11", "B12"}; // int[] indexes = new int[] {3, 4, 10, 11};
        Dimension tileSize = JAI.getDefaultTileSize();
        int[] trimmingSourceProductBandIndices = new int[] {0, 1, 2};
        int threadCount = Runtime.getRuntime().availableProcessors();
        Executor threadPool = Executors.newCachedThreadPool();

        try {
            ProductTrimmingResult currentResult = runTrimming(threadCount, threadPool, this.currentSourceProduct, sourceBandNames, trimmingSourceProductBandIndices, tileSize);
            Product currentProduct = currentResult.getProduct();
            IntSet currentSegmentationTrimmingRegionKeys = currentResult.getTrimmingRegionKeys();
            Product currentProductColorFill = currentResult.getSegmentationProductColorFill();

            // reset the reference
            currentResult = null;

            ProductTrimmingResult previousResult = runTrimming(threadCount, threadPool, this.previousSourceProduct, sourceBandNames, trimmingSourceProductBandIndices, tileSize);
            Product previousProduct = previousResult.getProduct();
            IntSet previousSegmentationTrimmingRegionKeys = previousResult.getTrimmingRegionKeys();
            Product previousProductColorFill = previousResult.getSegmentationProductColorFill();

            // reset the reference
            previousResult = null;

            Product unionMaskProduct = runUnionMasksOp(threadCount, threadPool, currentSegmentationTrimmingRegionKeys, currentProductColorFill,
                                                       previousSegmentationTrimmingRegionKeys, previousProductColorFill, tileSize);

            // reset the references
            currentSegmentationTrimmingRegionKeys = null;
            currentProductColorFill = null;
            previousSegmentationTrimmingRegionKeys = null;
            previousProductColorFill = null;

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, ""); // add an empty line
                logger.log(Level.FINE, "Start segmentation for difference bands.");
            }

            Product differenceSegmentationProduct = GenericRegionMergingOp.runSegmentation(currentProduct, previousProduct, sourceBandNames, mergingCostCriterion, regionMergingCriterion,
                                                                                           totalIterationsForSecondSegmentation, threshold, spectralWeight, shapeWeight);

            // reset the references
            currentProduct = null;
            previousProduct = null;

            IntSet differenceTrimmingSet = computeDifferenceTrimmingSet(threadCount, threadPool, differenceSegmentationProduct,
                                                                        unionMaskProduct, trimmingSourceProductBandIndices, tileSize);

            runFinalMaskOp(threadCount, threadPool, differenceSegmentationProduct, unionMaskProduct, differenceTrimmingSet, tileSize);

            if (logger.isLoggable(Level.FINE)) {
                long finishTime = System.currentTimeMillis();
                long totalSeconds = (finishTime - startTime) / 1000;
                logger.log(Level.FINE, ""); // add an empty line
                logger.log(Level.FINE, "Finish Forest Cover Change: imageWidth: "+this.targetProduct.getSceneRasterWidth()+", imageHeight: "+this.targetProduct.getSceneRasterHeight()+", total seconds: "+totalSeconds+", finish time: "+new Date(finishTime));
            }
        } catch (Exception ex) {
            throw new OperatorException(ex);
        }
    }

    private IntSet computeDifferenceTrimmingSet(int threadCount, Executor threadPool, Product differenceSegmentationProduct,
                                                Product unionMaskProduct, int[] sourceBandIndices, Dimension tileSize)
                                                throws Exception {

        DifferenceRegionComputingHelper helper = new DifferenceRegionComputingHelper(differenceSegmentationProduct, currentSourceProduct, previousSourceProduct,
                                                                                     unionMaskProduct, sourceBandIndices, tileSize.width, tileSize.height);
        IntSet differenceTrimmingSet = helper.computeRegionsInParallel(threadCount, threadPool);

        helper = null;
        System.gc();

        return differenceTrimmingSet;
    }

    private ProductTrimmingResult runTrimming(int threadCount, Executor threadPool, Product sourceProduct,
                                              String[] sourceBandNames, int[] trimmingSourceProductBandIndices, Dimension tileSize)
                                              throws Exception {

        Product product = generateBandsExtractor(sourceProduct, sourceBandNames);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Start generate color fill for source product '" + sourceProduct.getName()+"'");
        }

        Product productColorFill = generateColorFill(threadCount, threadPool, product, tileSize);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Start trimming for source product '" + sourceProduct.getName()+"'");
        }

        IntSet segmentationTrimmingRegionKeys = TrimmingHelper.computeTrimmingStatistics(threadCount, threadPool, productColorFill, product, trimmingSourceProductBandIndices, tileSize);
        return new ProductTrimmingResult(product, segmentationTrimmingRegionKeys, productColorFill);
    }

    private Product generateColorFill(int threadCount, Executor threadPool, Product sourceProduct, Dimension tileSize) throws Exception {
        String[] sourceBandNames = buildBandNamesArray(sourceProduct);
        Product segmentationProduct = GenericRegionMergingOp.runSegmentation(sourceProduct, sourceBandNames, mergingCostCriterion, regionMergingCriterion,
                                                                             totalIterationsForSecondSegmentation, threshold, spectralWeight, shapeWeight);

        return runColorFillerOp(threadCount, threadPool, segmentationProduct, forestCoverPercentage, tileSize);
    }

    private static String[] buildBandNamesArray(Product sourceProduct) {
        ProductNodeGroup<Band> bandGroup = sourceProduct.getBandGroup();
        int bandCount = bandGroup.getNodeCount();
        String[] sourceBandNames = new String[bandCount];
        for (int i=0; i<bandCount; i++) {
            sourceBandNames[i] = bandGroup.get(i).getName();
        }
        return sourceBandNames;
    }

    private static Product runColorFillerOp(int threadCount, Executor threadPool, Product sourceProduct, float percentagePixels, Dimension tileSize)
                                            throws Exception {

        IntSet validRegions = runObjectsSelectionOp(threadCount, threadPool, sourceProduct, percentagePixels);

        ColorFillerHelper helper = new ColorFillerHelper(sourceProduct, validRegions, tileSize.width, tileSize.height);
        return helper.computeRegionsInParallel(threadCount, threadPool);
    }

    private static IntSet runObjectsSelectionOp(int threadCount, Executor threadPool, Product sourceProduct, float percentagePixels)
                                                throws Exception {

        Product landCover = buildLandCoverProduct(sourceProduct);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("landCoverNames", ForestCoverChangeConstans.LAND_COVER_NAME);
        Product landCoverProduct = GPF.createProduct("AddLandCover", parameters, landCover);

        Dimension tileSize = JAI.getDefaultTileSize();
        ObjectsSelectionHelper helper = new ObjectsSelectionHelper(sourceProduct, landCoverProduct, tileSize.width, tileSize.height);
        Int2ObjectMap<ObjectsSelectionOp.PixelStatistic> statistics = helper.computeRegionsInParallel(threadCount, threadPool);

        IntSet validRegions = new IntOpenHashSet();
        ObjectIterator<Int2ObjectMap.Entry<ObjectsSelectionOp.PixelStatistic>> it = statistics.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            Int2ObjectMap.Entry<ObjectsSelectionOp.PixelStatistic> entry = it.next();
            ObjectsSelectionOp.PixelStatistic value = entry.getValue();
            float percent = ((float)value.getPixelsInRange()/(float)value.getTotalNumberPixels()) * 100;
            if (percent >= percentagePixels) {
                validRegions.add(entry.getIntKey());
            }
        }
        return validRegions;
    }

    private static Product buildLandCoverProduct(Product sourceProduct) {
        Product landCoverProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                               sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        landCoverProduct.setStartTime(sourceProduct.getStartTime());
        landCoverProduct.setEndTime(sourceProduct.getEndTime());
        landCoverProduct.setNumResolutionsMax(sourceProduct.getNumResolutionsMax());

        ProductUtils.copyMetadata(sourceProduct,  landCoverProduct);
        ProductUtils.copyGeoCoding(sourceProduct,  landCoverProduct);
        ProductUtils.copyTiePointGrids(sourceProduct,  landCoverProduct);
        ProductUtils.copyVectorData(sourceProduct,  landCoverProduct);
        return landCoverProduct;
    }

    private static Product generateBandsExtractor(Product sourceProduct, String[] sourceBandNames) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Extract "+sourceBandNames.length+" bands for source product '" + sourceProduct.getName()+"'");
        }

        Product targetProduct = BandsExtractorOp.extractBands(sourceProduct, sourceBandNames);
        return resampleAllBands(targetProduct);
    }

    private static Product resampleAllBands(Product sourceProduct) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Resample the bands for source product '" + sourceProduct.getName()+"'");
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("targetWidth", sourceProduct.getSceneRasterWidth());
        parameters.put("targetHeight", sourceProduct.getSceneRasterHeight());
        Product targetProduct = GPF.createProduct("Resample", parameters, sourceProduct);
        targetProduct.setName(sourceProduct.getName());
        return targetProduct;
    }

    private static Product runUnionMasksOp(int threadCount, Executor threadPool, IntSet currentSegmentationTrimmingRegionKeys,
                                           Product currentSegmentationSourceProduct, IntSet previousSegmentationTrimmingRegionKeys,
                                           Product previousSegmentationSourceProduct, Dimension tileSize)
                                           throws Exception {

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, ""); // add an empty line
            logger.log(Level.FINE, "Start running union mask");
        }

        UnionMasksHelper helper = new UnionMasksHelper(currentSegmentationSourceProduct, previousSegmentationSourceProduct, currentSegmentationTrimmingRegionKeys,
                                                       previousSegmentationTrimmingRegionKeys, tileSize.width, tileSize.height);
        ProductData productData = helper.computeRegionsInParallel(threadCount, threadPool);
        int sceneRasterWidth = currentSegmentationSourceProduct.getSceneRasterWidth();
        int sceneRasterHeight = currentSegmentationSourceProduct.getSceneRasterHeight();
        Product targetProduct = new Product("forestCoverChange", currentSegmentationSourceProduct.getProductType(), sceneRasterWidth, sceneRasterHeight);
        targetProduct.setPreferredTileSize(tileSize);
        Band targetBand = new Band("band_1", ProductData.TYPE_INT32, sceneRasterWidth, sceneRasterHeight);
        targetBand.setData(productData);
        targetProduct.addBand(targetBand);
        return targetProduct;
    }

    private Product runFinalMaskOp(int threadCount, Executor threadPool, Product differenceSegmentationProduct, Product unionMaskProduct,
                                   IntSet differenceTrimmingSet, Dimension tileSize)
                                   throws Exception {

        FinalMasksHelper helper = new FinalMasksHelper(differenceSegmentationProduct, unionMaskProduct, differenceTrimmingSet, tileSize.width, tileSize.height);
        ProductData productData = helper.computeRegionsInParallel(threadCount, threadPool);
        this.targetProduct.getBandAt(0).setData(productData);
        return this.targetProduct;
    }

    private static class ProductTrimmingResult {
        private final Product product;
        private final IntSet trimmingRegionKeys;
        private final Product segmentationProductColorFill;

        ProductTrimmingResult(Product product, IntSet trimmingRegionKeys, Product segmentationProductColorFill) {
            this.product = product;
            this.trimmingRegionKeys = trimmingRegionKeys;
            this.segmentationProductColorFill = segmentationProductColorFill;
        }

        public Product getProduct() {
            return product;
        }

        public IntSet getTrimmingRegionKeys() {
            return trimmingRegionKeys;
        }

        public Product getSegmentationProductColorFill() {
            return segmentationProductColorFill;
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ForestCoverChangeOp.class);
        }
    }
}
