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
package org.esa.s1tbx.insar.gpf.coregistration;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GcpDescriptor;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.coregistration.CPM;
import org.jlinda.core.coregistration.PolynomialModel;
import org.jlinda.core.coregistration.SimpleLUT;
import org.jlinda.nest.gpf.coregistration.GCPManager;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationTable;
import javax.media.jai.RenderedOp;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Image co-registration is fundamental for Interferometry SAR (InSAR) imaging and its applications, such as
 * DEM map generation and analysis. To obtain a high quality InSAR image, the individual complex images need
 * to be co-registered to sub-pixel accuracy. The co-registration is accomplished through an alignment of a
 * master image with a slave image.
 * <p>
 * To achieve the alignment of master and slave images, the first step is to generate a set of uniformly
 * spaced ground control points (GCPs) in the master image, along with the corresponding GCPs in the slave
 * image. Details of the generation of the GCP pairs are given in GCPSelectionOperator. The next step is to
 * construct a warp distortion function from the computed GCP pairs and generate co-registered slave image.
 * <p>
 * This operator computes the warp function from the master-slave GCP pairs for given polynomial order.
 * Basically coefficients of two polynomials are determined from the GCP pairs with each polynomial for
 * one coordinate of the image pixel. With the warp function determined, the co-registered image can be
 * obtained by mapping slave image pixels to master image pixels. In particular, for each pixel position in
 * the master image, warp function produces its corresponding pixel position in the slave image, and the
 * pixel value is computed through interpolation. The following interpolation methods are available:
 * <p>
 * 1. Nearest-neighbour interpolation
 * 2. Bilinear interpolation
 * 3. Bicubic interpolation
 * 4. Bicubic2 interpolation
 */

@OperatorMetadata(alias = "Warp",
        category = "Radar/Coregistration",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Create Warp Function And Get Co-registrated Images")
public class WarpOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "Confidence level for outlier detection procedure, lower value accepts more outliers",
            valueSet = {"0.001", "0.005", "0.05", "0.1"},
            defaultValue = "0.05",
            label = "Significance Level for Outlier Removal")
    private float rmsThreshold = 0.05f;
    private float cpmWtestCriticalValue;

    @Parameter(description = "The order of WARP polynomial function", valueSet = {"1", "2", "3"}, defaultValue = "2",
            label = "Warp Polynomial Order")
    private int warpPolynomialOrder = 2;

    @Parameter(valueSet = {NEAREST_NEIGHBOR, BILINEAR, BICUBIC, BICUBIC2,
            TRI, CC4P, CC6P, TS6P, TS8P, TS16P}, defaultValue = CC6P, label = "Interpolation Method")
    private String interpolationMethod = CC6P;

    @Parameter(description = "Refine estimated offsets using a-priori DEM",
            defaultValue = "false",
            label = "Offset Refinement Based on DEM")
    private Boolean cpmDemRefinement = false;

    @Parameter(description = "The digital elevation model.",
            defaultValue = "SRTM 3Sec",
            label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    @Parameter(defaultValue = "false")
    private boolean excludeMaster = false;

    private Interpolation interp = null;
    private InterpolationTable interpTable = null;

    @Parameter(description = "Show the Residuals file in a text viewer", defaultValue = "false", label = "Show Residuals")
    private Boolean openResidualsFile = false;

    private Band masterBand = null;
    private Band masterBand2 = null;
    private boolean complexCoregistration = false;
    private boolean warpDataAvailable = false;

    public static final String NEAREST_NEIGHBOR = "Nearest-neighbor interpolation";
    public static final String BILINEAR = "Bilinear interpolation";
    public static final String BICUBIC = "Bicubic interpolation";
    public static final String BICUBIC2 = "Bicubic2 interpolation";
    public static final String TRI = SimpleLUT.TRI;
    public static final String CC4P = SimpleLUT.CC4P;
    public static final String CC6P = SimpleLUT.CC6P;
    public static final String TS6P = SimpleLUT.TS6P;
    public static final String TS8P = SimpleLUT.TS8P;
    public static final String TS16P = SimpleLUT.TS16P;

    private final Map<Band, Band> sourceRasterMap = new HashMap<>(10);
    private final Map<Band, Band> complexSrcMap = new HashMap<>(10);
    private final Map<Band, PolynomialModel> warpDataMap = new HashMap<>(10);

    private String processedSlaveBand;
    private String[] masterBandNames = null;

    // DEM refinement
    private static final int ORBIT_INTERP_DEGREE = 3;
    float demNoDataValue = 0;
    private ElevationModel dem = null;

    private int maxIterations = 20;

    private boolean inSAROptimized = true;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public WarpOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        try {
            // clear any old residual file
            final File residualsFile = getResidualsFile(sourceProduct);
            if (residualsFile.exists()) {
                residualsFile.delete();
            }

            getMasterBands();

            if (rmsThreshold == 0.001f) {
                cpmWtestCriticalValue = 3.2905267314919f;
            } else if (rmsThreshold == 0.05f) {
                cpmWtestCriticalValue = 1.95996398454005f;
            } else if (rmsThreshold == 0.1f) {
                cpmWtestCriticalValue = 1.64485362695147f;
            } else {
                cpmWtestCriticalValue = 1.95996398454005f;
            }

            // The following code is temporary
            if (complexCoregistration) {

                switch (interpolationMethod) {
                    case CC4P:
                        constructInterpolationTable(CC4P);
                        break;
                    case CC6P:
                        constructInterpolationTable(CC6P);
                        break;
                    case TS6P:
                        constructInterpolationTable(TS6P);
                        break;
                    case TS8P:
                        constructInterpolationTable(TS8P);
                        break;
                    case TS16P:
                        constructInterpolationTable(TS16P);
                        break;
                    default:
                        interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                        break;
                }
            } else { // detected products

                switch (interpolationMethod) {
                    case NEAREST_NEIGHBOR:
                        interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
                        break;
                    case BILINEAR:
                        interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
                        break;
                    case BICUBIC:
                        interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
                        break;
                    case BICUBIC2:
                        interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
                        break;
                }
            }

            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            if (absRoot != null) {
                processedSlaveBand = absRoot.getAttributeString("processed_slave");
            }

            createTargetProduct();

            if (cpmDemRefinement == null)
                cpmDemRefinement = false;

        } catch (Throwable e) {
            openResidualsFile = true;
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getMasterBands() {
        String mstBandName = sourceProduct.getBandAt(0).getName();

        // find co-pol bands
        final String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
        for (String bandName : masterBandNames) {
            final String mstPol = OperatorUtils.getPolarizationFromBandName(bandName);
            if (mstPol != null && (mstPol.equals("hh") || mstPol.equals("vv"))) {
                mstBandName = bandName;
                break;
            }
        }
        masterBand = sourceProduct.getBand(mstBandName);
        if (masterBand.getUnit() != null && masterBand.getUnit().equals(Unit.REAL)) {
            int mstIdx = sourceProduct.getBandIndex(mstBandName);
            if (sourceProduct.getNumBands() > mstIdx + 1) {
                masterBand2 = sourceProduct.getBandAt(mstIdx + 1);
                complexCoregistration = true;
            }
        }
    }

    private void addSlaveGCPs(final PolynomialModel warpData, final String bandName) {

        final GeoCoding targetGeoCoding = targetProduct.getSceneGeoCoding();
        final ProductNodeGroup<Placemark> targetGCPGroup = GCPManager.instance().getGcpGroup(targetProduct.getBand(bandName));
        targetGCPGroup.removeAll();

        final List<Placemark> slaveGCPList = warpData.getSlaveGCPList();
        for (final Placemark sPin : slaveGCPList) {
            final Placemark tPin = Placemark.createPointPlacemark(GcpDescriptor.getInstance(),
                                                                  sPin.getName(),
                                                                  sPin.getLabel(),
                                                                  sPin.getDescription(),
                                                                  sPin.getPixelPos(),
                                                                  sPin.getGeoPos(),
                                                                  targetGeoCoding);

            targetGCPGroup.add(tPin);
        }
    }

    private String formatName(final Band srcBand) {
        String name = srcBand.getName();
        if (excludeMaster) {  // multi-output without master
            String newName = StackUtils.getBandNameWithoutDate(name);
            if (name.equals(processedSlaveBand)) {
                processedSlaveBand = newName;
            }
            return newName;
        }
        return name;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        masterBandNames = StackUtils.getMasterBandNames(sourceProduct);

        final int numSrcBands = sourceProduct.getNumBands();
        int inc = 1;
        if (complexCoregistration)
            inc = 2;
        for (int i = 0; i < numSrcBands; i += inc) {
            final Band srcBand = sourceProduct.getBandAt(i);
            Band targetBand;
            if (srcBand == masterBand || srcBand == masterBand2 ||
                    StringUtils.contains(masterBandNames, srcBand.getName())) {
                if (excludeMaster) {
                    continue;
                }
                targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
                targetBand.setSourceImage(srcBand.getSourceImage());
            } else {
                targetBand = targetProduct.addBand(formatName(srcBand), ProductData.TYPE_FLOAT32);
                ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
            }
            sourceRasterMap.put(targetBand, srcBand);

            if (complexCoregistration) {
                final Band srcBandQ = sourceProduct.getBandAt(i + 1);
                Band targetBandQ;
                if (srcBand == masterBand || srcBand == masterBand2 ||
                        StringUtils.contains(masterBandNames, srcBand.getName())) {
                    targetBandQ = ProductUtils.copyBand(srcBandQ.getName(), sourceProduct, targetProduct, false);
                    targetBandQ.setSourceImage(srcBandQ.getSourceImage());
                } else {
                    targetBandQ = targetProduct.addBand(formatName(srcBandQ), ProductData.TYPE_FLOAT32);
                    ProductUtils.copyRasterDataNodeProperties(srcBandQ, targetBandQ);
                }
                sourceRasterMap.put(targetBandQ, srcBandQ);

                complexSrcMap.put(srcBandQ, srcBand);
                String suffix = "";
                if (excludeMaster) { // multi-output without master
                    String pol = OperatorUtils.getPolarizationFromBandName(srcBand.getName());
                    if (pol != null && !pol.isEmpty()) {
                        suffix = '_' + pol.toUpperCase();
                    }
                } else {
                    suffix = '_' + OperatorUtils.getSuffixFromBandName(srcBand.getName());
                }
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetBand, targetBandQ, suffix);
            }
        }

        // co-registered image should have the same geo-coding as the master image
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        updateTargetProductMetadata();
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

        if (excludeMaster) {
            final String[] slaveNames = StackUtils.getSlaveProductNames(sourceProduct);
            absTgt.setAttributeString(AbstractMetadata.PRODUCT, slaveNames[0]);

            final ProductData.UTC[] times = StackUtils.getProductTimes(sourceProduct);
            targetProduct.setStartTime(times[1]);

            double lineTimeInterval = absTgt.getAttributeDouble(AbstractMetadata.line_time_interval);
            int height = sourceProduct.getSceneRasterHeight();
            ProductData.UTC endTime = new ProductData.UTC(times[1].getMJD() + (lineTimeInterval * height) / Constants.secondsInDay);
            targetProduct.setEndTime(endTime);

        } else {
            // only if its a full coregistered stack including master band
            AbstractMetadata.setAttribute(absTgt, AbstractMetadata.coregistered_stack, 1);
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle targetRectangle = targetTile.getRectangle();
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        //System.out.println("WARPOperator: x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            if (!warpDataAvailable) {
                if (cpmDemRefinement) {
                    createDEM();
                }

                getWarpData(targetRectangle);
            }

            final Band srcBand = sourceRasterMap.get(targetBand);
            if (srcBand == null)
                return;
            Band realSrcBand = complexSrcMap.get(srcBand);
            if (realSrcBand == null)
                realSrcBand = srcBand;

            // create source image
            final Tile sourceRaster = getSourceTile(srcBand, targetRectangle);

            if (pm.isCanceled())
                return;

            final PolynomialModel warpData = warpDataMap.get(realSrcBand);
            if (!warpData.isValid())
                return;

            final RenderedImage srcImage = sourceRaster.getRasterDataNode().getSourceImage();

            // get warped image
            final RenderedOp warpedImage = JAIFunctions.createWarpImage(warpData.getJAIWarp(), srcImage,
                                                                        interp, interpTable);

            // copy warped image data to target
            final float[] dataArray = warpedImage.getData(targetRectangle).getSamples(x0, y0, w, h, 0, (float[]) null);

            targetTile.setRawSamples(ProductData.createInstance(dataArray));

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private synchronized void createDEM() throws IOException {

        final Resampling resampling = ResamplingFactory.createResampling(ResamplingFactory.BILINEAR_INTERPOLATION_NAME);

        if (dem != null) return;

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(demName);

        if (demDescriptor == null) {
            throw new OperatorException("The DEM '" + demName + "' is not supported.");
        }

        dem = demDescriptor.createDem(resampling);
        if (dem == null) {
            throw new OperatorException("The DEM '" + demName + "' has not been installed.");
        }
        demNoDataValue = demDescriptor.getNoDataValue();
    }

    private synchronized void getWarpData(final Rectangle targetRectangle) throws Exception {

        if (warpDataAvailable) {
            return;
        }

        // find first real slave band
        final Band targetBand = targetProduct.getBand(processedSlaveBand);
        // force getSourceTile to computeTiles on GCPSelection
        final Tile sourceRaster = getSourceTile(sourceRasterMap.get(targetBand), targetRectangle);

        // for all slave bands or band pairs compute a warp
        final int numSrcBands = sourceProduct.getNumBands();
        int inc = 1;
        if (complexCoregistration)
            inc = 2;

        final ProductNodeGroup<Placemark> masterGCPGroup = GCPManager.instance().getGcpGroup(masterBand);
        final org.jlinda.core.Window masterWindow = new org.jlinda.core.Window(0, sourceProduct.getSceneRasterHeight(), 0, sourceProduct.getSceneRasterWidth());

        // setup master metadata
        SLCImage masterMeta = null;
        Orbit masterOrbit = null;
        if (cpmDemRefinement) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
            masterMeta = new SLCImage(absRoot);
            masterOrbit = new Orbit(absRoot, ORBIT_INTERP_DEGREE);
        }

        boolean appendFlag = false;
        int slaveMetaCnt = 0;
        for (int i = 0; i < numSrcBands; i += inc) {

            final Band srcBand = sourceProduct.getBandAt(i);
            if (srcBand == masterBand || srcBand == masterBand2 ||
                    StringUtils.contains(masterBandNames, srcBand.getName()))
                continue;

            ProductNodeGroup<Placemark> slaveGCPGroup = GCPManager.instance().getGcpGroup(srcBand);
            if (slaveGCPGroup.getNodeCount() < 3) {
                // find others for same slave product
                final String slvProductName = StackUtils.getSlaveProductName(sourceProduct, srcBand, null);
                for (Band band : sourceProduct.getBands()) {
                    if (band != srcBand) {
                        final String productName = StackUtils.getSlaveProductName(sourceProduct, band, null);
                        if (slvProductName != null && slvProductName.equals(productName)) {
                            slaveGCPGroup = GCPManager.instance().getGcpGroup(band);
                            if (slaveGCPGroup.getNodeCount() >= 3)
                                break;
                        }
                    }
                }
            }

            if (inSAROptimized) {
                final CPM cpm = new CPM(warpPolynomialOrder, maxIterations, cpmWtestCriticalValue,
                                        masterWindow, masterGCPGroup, slaveGCPGroup);
                warpDataMap.put(srcBand, cpm);

                final int nodeCount = slaveGCPGroup.getNodeCount();
                if (nodeCount < 3) {
                    cpm.noRedundancy = true;
                    continue;
                }

                // setup slave metadata
                if (cpmDemRefinement && !cpm.noRedundancy) {

                    // get height for corresponding points
                    double[] heightArray = new double[nodeCount];
                    final List<Placemark> slaveGCPList = new ArrayList<>();

                    for (int j = 0; j < nodeCount; j++) {

                        // work only with windows that survived threshold for this slave
                        slaveGCPList.add(slaveGCPGroup.get(j));
                        final Placemark sPin = slaveGCPList.get(j);
                        final Placemark mPin = masterGCPGroup.get(sPin.getName());
                        final PixelPos mGCPPos = mPin.getPixelPos();

                        double[] phiLamPoint = masterOrbit.lph2ell(mGCPPos.y, mGCPPos.x, 0, masterMeta);
                        PixelPos demIndexPoint = dem.getIndex(new GeoPos((phiLamPoint[0] * org.jlinda.core.Constants.RTOD), (phiLamPoint[1] * org.jlinda.core.Constants.RTOD)));

                        double height = dem.getSample(demIndexPoint.x, demIndexPoint.y);

                        if (Double.isNaN(height) || height == demNoDataValue) {
                            height = demNoDataValue;
                        }

                        heightArray[j] = height;
                    }

                    final MetadataElement slaveRoot = targetProduct.getMetadataRoot().getElement(AbstractMetadata.SLAVE_METADATA_ROOT).getElementAt(slaveMetaCnt);
                    final SLCImage slaveMeta = new SLCImage(slaveRoot);
                    final Orbit slaveOrbit = new Orbit(slaveRoot, ORBIT_INTERP_DEGREE);
                    cpm.setDemNoDataValue(demNoDataValue);
                    cpm.setUpDEMRefinement(masterMeta, masterOrbit, slaveMeta, slaveOrbit, heightArray);
                    cpm.setUpDemOffset();
                }

                cpm.computeCPM();
                cpm.computeEstimationStats();
                cpm.wrapJaiWarpPolynomial();

                if (cpm.noRedundancy) {
                    continue;
                }

                if (!appendFlag) {
                    appendFlag = true;
                }

                //outputCoRegistrationInfo(sourceProduct, warpPolynomialOrder, cpm, appendFlag, srcBand.getName());

                addSlaveGCPs(cpm, srcBand.getName());
            } else {

                final WarpData warpData = new WarpData(slaveGCPGroup);
                warpDataMap.put(srcBand, warpData);

                if (slaveGCPGroup.getNodeCount() < 3) {
                    warpData.notEnoughGCPs = true;
                    continue;
                }

                computeWARPPolynomialFromGCPs(sourceProduct, srcBand, warpPolynomialOrder, masterGCPGroup, maxIterations,
                                              rmsThreshold, appendFlag, warpData);

                if (warpData.notEnoughGCPs) {
                    continue;
                }

                if (!appendFlag) {
                    appendFlag = true;
                }

                addSlaveGCPs(warpData, targetBand.getName());
            }
        }

        announceGCPWarning();

        GCPManager.instance().removeAllGcpGroups();

        if (openResidualsFile) {
            final File residualsFile = getResidualsFile(sourceProduct);
            if (Desktop.isDesktopSupported() && residualsFile.exists()) {
                try {
                    Desktop.getDesktop().open(residualsFile);
                } catch (Exception e) {
                    System.out.println("Error opening residuals file " + e.getMessage());
                    // do nothing
                }
            }
        }

        // update metadata
        writeWarpDataToMetadata();

        warpDataAvailable = true;
    }

    public static void computeWARPPolynomialFromGCPs(
            final Product sourceProduct, final Band srcBand, final int warpPolynomialOrder,
            final ProductNodeGroup<Placemark> masterGCPGroup, final int maxIterations, final float rmsThreshold,
            final boolean appendFlag, WarpData warpData) {

        boolean append;
        float threshold = 0.0f;
        for (int iter = 0; iter < maxIterations; iter++) {

            if (iter == 0) {
                append = appendFlag;
            } else {
                append = true;

                if (iter < maxIterations - 1 && warpData.rmsMean > rmsThreshold) {
                    threshold = (float) (warpData.rmsMean + warpData.rmsStd);
                } else {
                    threshold = rmsThreshold;
                }
            }

            if (threshold > 0.0f) {
                eliminateGCPsBasedOnRMS(warpData, threshold);
            }

            computeWARPPolynomial(warpData, warpPolynomialOrder, masterGCPGroup);

            outputCoRegistrationInfo(
                    sourceProduct, warpPolynomialOrder, warpData, append, threshold, iter, srcBand.getName());

            if (warpData.notEnoughGCPs || iter > 0 && threshold <= rmsThreshold) {
                break;
            }
        }
    }

    /**
     * Compute WARP polynomial function using master and slave GCP pairs.
     *
     * @param warpData            Stores the warp information per band.
     * @param warpPolynomialOrder The WARP polynimal order.
     * @param masterGCPGroup      The master GCPs.
     */
    public static void computeWARPPolynomial(
            final WarpData warpData, final int warpPolynomialOrder, final ProductNodeGroup<Placemark> masterGCPGroup) {

        getNumOfValidGCPs(warpData, warpPolynomialOrder);

        getMasterAndSlaveGCPCoordinates(warpData, masterGCPGroup);
        if (warpData.notEnoughGCPs) return;

        warpData.computeWARP(warpPolynomialOrder);

        computeRMS(warpData, warpPolynomialOrder);
    }

    private void writeWarpDataToMetadata() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        final Set<Band> bandSet = warpDataMap.keySet();

        for (Band band : bandSet) {
            final MetadataElement bandElem = AbstractMetadata.getBandAbsMetadata(absRoot, band.getName(), true);

            MetadataElement warpDataElem = bandElem.getElement("WarpData");
            if (warpDataElem == null) {
                warpDataElem = new MetadataElement("WarpData");
                bandElem.addElement(warpDataElem);
            } else {
                // empty out element
                final MetadataAttribute[] attribList = warpDataElem.getAttributes();
                for (MetadataAttribute attrib : attribList) {
                    warpDataElem.removeAttribute(attrib);
                }
            }

            final PolynomialModel warpData = warpDataMap.get(band);
            if (warpData.getNumObservations() > 0) {
                for (int i = 0; i < warpData.getNumObservations(); i++) {
                    final MetadataElement gcpElem = new MetadataElement("GCP" + i);
                    warpDataElem.addElement(gcpElem);

                    gcpElem.setAttributeDouble("mst_x", warpData.getXMasterCoord(i));
                    gcpElem.setAttributeDouble("mst_y", warpData.getYMasterCoord(i));

                    gcpElem.setAttributeDouble("slv_x", warpData.getXSlaveCoord(i));
                    gcpElem.setAttributeDouble("slv_y", warpData.getYSlaveCoord(i));

                    if (warpData.isValid()) {
                        gcpElem.setAttributeDouble("rms", warpData.getRMS(i));
                    }
                }
            }
        }
    }

    /**
     * Get the number of valid GCPs.
     *
     * @param warpData            Stores the warp information per band.
     * @param warpPolynomialOrder The WARP polynimal order.
     * @throws OperatorException The exceptions.
     */
    private static void getNumOfValidGCPs(
            final WarpData warpData, final int warpPolynomialOrder) throws OperatorException {

        warpData.numValidGCPs = warpData.slaveGCPList.size();
        final int requiredGCPs = (warpPolynomialOrder + 2) * (warpPolynomialOrder + 1) / 2;
        if (warpData.numValidGCPs < requiredGCPs) {
            warpData.notEnoughGCPs = true;
            //throw new OperatorException("Order " + warpPolynomialOrder + " requires " + requiredGCPs +
            //        " GCPs, valid GCPs are " + warpData.numValidGCPs + ", try a larger RMS threshold.");
        }
    }

    /**
     * Get GCP coordinates for master and slave bands.
     *
     * @param warpData       Stores the warp information per band.
     * @param masterGCPGroup The master GCPs.
     */
    private static void getMasterAndSlaveGCPCoordinates(
            final WarpData warpData, final ProductNodeGroup<Placemark> masterGCPGroup) {

        warpData.masterGCPCoords = new float[2 * warpData.numValidGCPs];
        warpData.slaveGCPCoords = new float[2 * warpData.numValidGCPs];

        for (int i = 0; i < warpData.numValidGCPs; ++i) {

            final Placemark sPin = warpData.slaveGCPList.get(i);
            final PixelPos sGCPPos = sPin.getPixelPos();
            //System.out.println("WARP: slave gcp[" + i + "] = " + "(" + sGCPPos.x + "," + sGCPPos.y + ")");

            final Placemark mPin = masterGCPGroup.get(sPin.getName());
            final PixelPos mGCPPos = mPin.getPixelPos();
            //System.out.println("WARP: master gcp[" + i + "] = " + "(" + mGCPPos.x + "," + mGCPPos.y + ")");

            final int j = 2 * i;
            warpData.masterGCPCoords[j] = (float) mGCPPos.x;
            warpData.masterGCPCoords[j + 1] = (float) mGCPPos.y;
            warpData.slaveGCPCoords[j] = (float) sGCPPos.x;
            warpData.slaveGCPCoords[j + 1] = (float) sGCPPos.y;
        }
    }

    /**
     * Compute root mean square error of the warped GCPs for given WARP function and given GCPs.
     *
     * @param warpData            Stores the warp information per band.
     * @param warpPolynomialOrder The WARP polynimal order.
     */
    private static void computeRMS(final WarpData warpData, final int warpPolynomialOrder) {

        // compute RMS for all valid GCPs
        warpData.rms = new float[warpData.numValidGCPs];
        warpData.colResiduals = new float[warpData.numValidGCPs];
        warpData.rowResiduals = new float[warpData.numValidGCPs];
        final PixelPos slavePos = new PixelPos(0.0f, 0.0f);
        for (int i = 0; i < warpData.rms.length; i++) {
            final int i2 = 2 * i;
            getWarpedCoords(warpData,
                            warpPolynomialOrder,
                            warpData.masterGCPCoords[i2],
                            warpData.masterGCPCoords[i2 + 1],
                            slavePos);
            final double dX = slavePos.x - warpData.slaveGCPCoords[i2];
            final double dY = slavePos.y - warpData.slaveGCPCoords[i2 + 1];
            warpData.colResiduals[i] = (float) dX;
            warpData.rowResiduals[i] = (float) dY;
            warpData.rms[i] = (float) Math.sqrt(dX * dX + dY * dY);
        }

        // compute some statistics
        warpData.rmsMean = 0.0;
        warpData.rowResidualMean = 0.0;
        warpData.colResidualMean = 0.0;
        double rms2Mean = 0.0;
        double rowResidual2Mean = 0.0;
        double colResidual2Mean = 0.0;

        for (int i = 0; i < warpData.rms.length; i++) {
            warpData.rmsMean += warpData.rms[i];
            rms2Mean += warpData.rms[i] * warpData.rms[i];
            warpData.rowResidualMean += warpData.rowResiduals[i];
            rowResidual2Mean += warpData.rowResiduals[i] * warpData.rowResiduals[i];
            warpData.colResidualMean += warpData.colResiduals[i];
            colResidual2Mean += warpData.colResiduals[i] * warpData.colResiduals[i];
        }
        warpData.rmsMean /= warpData.rms.length;
        rms2Mean /= warpData.rms.length;
        warpData.rowResidualMean /= warpData.rms.length;
        rowResidual2Mean /= warpData.rms.length;
        warpData.colResidualMean /= warpData.rms.length;
        colResidual2Mean /= warpData.rms.length;

        warpData.rmsStd = Math.sqrt(rms2Mean - warpData.rmsMean * warpData.rmsMean);
        warpData.rowResidualStd = Math.sqrt(rowResidual2Mean - warpData.rowResidualMean * warpData.rowResidualMean);
        warpData.colResidualStd = Math.sqrt(colResidual2Mean - warpData.colResidualMean * warpData.colResidualMean);
    }

    private void constructInterpolationTable(String interpolationMethod) {

        // construct interpolation LUT
        SimpleLUT lut = new SimpleLUT(interpolationMethod);
        lut.constructLUT();

        int kernelLength = lut.getKernelLength();

        // get LUT and cast it to float for JAI
        double[] lutArrayDoubles = lut.getKernelAsArray();
        float lutArrayFloats[] = new float[lutArrayDoubles.length];
        int i = 0;
        for (double lutElement : lutArrayDoubles) {
            lutArrayFloats[i++] = (float) lutElement;
        }

        // construct interpolation table for JAI resampling
        final int subsampleBits = 7;
        final int precisionBits = 32;
        int padding = kernelLength / 2 - 1;

        interpTable = new InterpolationTable(padding, kernelLength, subsampleBits, precisionBits, lutArrayFloats);
    }

    /**
     * Eliminate master and slave GCP pairs that have root mean square error greater than given threshold.
     *
     * @param warpData  Stores the warp information per band.
     * @param threshold Threshold for eliminating GCPs.
     * @return True if some GCPs are eliminated, false otherwise.
     */
    public static boolean eliminateGCPsBasedOnRMS(final WarpData warpData, final float threshold) {

        final List<Placemark> pinList = new ArrayList<>();
        if (warpData.slaveGCPList.size() < warpData.rms.length) {
            warpData.notEnoughGCPs = true;
            return true;
        }
        for (int i = 0; i < warpData.rms.length; i++) {
            if (warpData.rms[i] >= threshold) {
                pinList.add(warpData.slaveGCPList.get(i));
                //System.out.println("WARP: slave gcp[" + i + "] is eliminated");
            }
        }

        for (Placemark aPin : pinList) {
            warpData.slaveGCPList.remove(aPin);
        }

        return !pinList.isEmpty();
    }

    /**
     * Compute warped GCPs.
     *
     * @param warpData            The WARP polynomial.
     * @param warpPolynomialOrder The WARP polynomial order.
     * @param mX                  The x coordinate of master GCP.
     * @param mY                  The y coordinate of master GCP.
     * @param slavePos            The warped GCP position.
     * @throws OperatorException The exceptions.
     */
    public static void getWarpedCoords(final WarpData warpData, final int warpPolynomialOrder,
                                       final double mX, final double mY, final PixelPos slavePos)
            throws OperatorException {

        final double[] xCoeffs = warpData.xCoef;
        final double[] yCoeffs = warpData.yCoef;
        if (xCoeffs.length != yCoeffs.length) {
            throw new OperatorException("WARP has different number of coefficients for X and Y");
        }

        final int numOfCoeffs = xCoeffs.length;
        switch (warpPolynomialOrder) {
            case 1: {
                if (numOfCoeffs != 3) {
                    throw new OperatorException("Number of WARP coefficients do not match WARP degree");
                }
                slavePos.x = xCoeffs[0] + xCoeffs[1] * mX + xCoeffs[2] * mY;

                slavePos.y = yCoeffs[0] + yCoeffs[1] * mX + yCoeffs[2] * mY;
                break;
            }
            case 2: {
                if (numOfCoeffs != 6) {
                    throw new OperatorException("Number of WARP coefficients do not match WARP degree");
                }
                final double mXmX = mX * mX;
                final double mXmY = mX * mY;
                final double mYmY = mY * mY;

                slavePos.x = xCoeffs[0] + xCoeffs[1] * mX + xCoeffs[2] * mY +
                        xCoeffs[3] * mXmX + xCoeffs[4] * mXmY + xCoeffs[5] * mYmY;

                slavePos.y = yCoeffs[0] + yCoeffs[1] * mX + yCoeffs[2] * mY +
                        yCoeffs[3] * mXmX + yCoeffs[4] * mXmY + yCoeffs[5] * mYmY;
                break;
            }
            case 3: {
                if (numOfCoeffs != 10) {
                    throw new OperatorException("Number of WARP coefficients do not match WARP degree");
                }
                final double mXmX = mX * mX;
                final double mXmY = mX * mY;
                final double mYmY = mY * mY;

                slavePos.x = xCoeffs[0] + xCoeffs[1] * mX + xCoeffs[2] * mY +
                        xCoeffs[3] * mXmX + xCoeffs[4] * mXmY + xCoeffs[5] * mYmY +
                        xCoeffs[6] * mXmX * mX + xCoeffs[7] * mX * mXmY + xCoeffs[8] * mXmY * mY + xCoeffs[9] * mYmY * mY;

                slavePos.y = yCoeffs[0] + yCoeffs[1] * mX + yCoeffs[2] * mY +
                        yCoeffs[3] * mXmX + yCoeffs[4] * mXmY + yCoeffs[5] * mYmY +
                        yCoeffs[6] * mXmX * mX + yCoeffs[7] * mX * mXmY + yCoeffs[8] * mXmY * mY + yCoeffs[9] * mYmY * mY;
                break;
            }
            default:
                throw new OperatorException("Incorrect WARP degree");
        }
    }

    /**
     * Output co-registration information to file.
     *
     * @param sourceProduct       The source product.
     * @param warpPolynomialOrder The order of Warp polinomial.
     * @param warpData            Stores the warp information per band.
     * @param appendFlag          Boolean flag indicating if the information is output to file in appending mode.
     * @param threshold           The threshold for elinimating GCPs.
     * @param parseIndex          Index for parsing GCPs.
     * @param bandName            the band name
     * @throws OperatorException The exceptions.
     */
    public static void outputCoRegistrationInfo(final Product sourceProduct, final int warpPolynomialOrder,
                                                final WarpData warpData, final boolean appendFlag,
                                                final float threshold, final int parseIndex, final String bandName)
            throws OperatorException {

        final File residualFile = getResidualsFile(sourceProduct);
        PrintStream p = null; // declare a print stream object

        try {
            final FileOutputStream out = new FileOutputStream(residualFile.getAbsolutePath(), appendFlag);

            // Connect print stream to the output stream
            p = new PrintStream(out);

            p.println();

            if (!appendFlag) {
                p.println();
                p.format("Transformation degree = %d", warpPolynomialOrder);
                p.println();
            }

            p.println();
            p.print("------------------------ Band: " + bandName + " (Parse " + parseIndex + ") ------------------------");
            p.println();

            if (!warpData.notEnoughGCPs) {
                p.println();
                p.println("WARP coefficients:");
                for (double xCoeff : warpData.xCoef) {
                    p.print((float) xCoeff + ", ");
                }

                p.println();
                for (double yCoeff : warpData.yCoef) {
                    p.print((float) yCoeff + ", ");
                }
                p.println();
            }

            if (appendFlag) {
                p.println();
                p.format("RMS Threshold: %5.3f", threshold);
                p.println();
            }

            p.println();
            if (appendFlag) {
                p.print("Valid GCPs after parse " + parseIndex + " :");
            } else {
                p.print("Initial Valid GCPs:");
            }
            p.println();

            if (!warpData.notEnoughGCPs) {

                p.println();
                p.println("  No.  | Master GCP x | Master GCP y | Slave GCP x  | Slave GCP y  | Row Residual | Col Residual |        RMS        |");
                p.println("----------------------------------------------------------------------------------------------------------------------");
                for (int i = 0; i < warpData.rms.length; i++) {
                    p.format("%6d |%13.3f |%13.3f |%13.3f |%13.3f |%13.8f |%13.8f |%18.12f |",
                             i, warpData.masterGCPCoords[2 * i], warpData.masterGCPCoords[2 * i + 1],
                             warpData.slaveGCPCoords[2 * i], warpData.slaveGCPCoords[2 * i + 1],
                             warpData.rowResiduals[i], warpData.colResiduals[i], warpData.rms[i]);
                    p.println();
                }

                p.println();
                p.print("Row residual mean = " + warpData.rowResidualMean);
                p.println();
                p.print("Row residual std = " + warpData.rowResidualStd);
                p.println();

                p.println();
                p.print("Col residual mean = " + warpData.colResidualMean);
                p.println();
                p.print("Col residual std = " + warpData.colResidualStd);
                p.println();

                p.println();
                p.print("RMS mean = " + warpData.rmsMean);
                p.println();
                p.print("RMS std = " + warpData.rmsStd);
                p.println();

            } else {

                p.println();
                p.println("No. | Master GCP x | Master GCP y | Slave GCP x | Slave GCP y |");
                p.println("---------------------------------------------------------------");
                for (int i = 0; i < warpData.numValidGCPs; i++) {
                    p.format("%2d  |%13.3f |%13.3f |%12.3f |%12.3f |",
                             i, warpData.masterGCPCoords[2 * i], warpData.masterGCPCoords[2 * i + 1],
                             warpData.slaveGCPCoords[2 * i], warpData.slaveGCPCoords[2 * i + 1]);
                    p.println();
                }
            }
            p.println();
            p.println();

        } catch (IOException exc) {
            throw new OperatorException(exc);
        } finally {
            if (p != null)
                p.close();
        }
    }

    private static File getResidualsFile(final Product sourceProduct) {
        final String fileName = sourceProduct.getName() + "_residual.txt";
        return new File(ResourceUtils.getReportFolder(), fileName);
    }


    private void announceGCPWarning() {
        String msg = "";
        for (Band srcBand : sourceProduct.getBands()) {
            final PolynomialModel warpData = warpDataMap.get(srcBand);
            if (warpData != null && !warpData.isValid()) {
                msg += srcBand.getName() + " does not have enough valid GCPs for the warp\n";
                openResidualsFile = true;
            }
        }
        if (!msg.isEmpty()) {
            System.out.println(msg);
            //if (VisatApp.getApp() != null) {
            //AutoCloseOptionPane.showWarningDialog("Some bands did not coregister", msg);
            //}
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(WarpOp.class);
        }
    }

}
