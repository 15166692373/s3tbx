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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.TileIndex;
import org.esa.snap.util.Maths;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * De-Burst a Sentinel-1 TOPSAR product
 */
@OperatorMetadata(alias = "TOPSAR-Deburst",
        category = "SAR Processing/SENTINEL-1",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Debursts a Sentinel-1 TOPSAR product")
public final class TOPSARDeburstOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of polarisations", label = "Polarisations")
    private String[] selectedPolarisations;

    private MetadataElement absRoot = null;
    private String acquisitionMode = null;
    private String productType = null;
    private int numOfSubSwath = 0;
    private int subSwathIndex = 0;
    private int targetWidth = 0;
    private int targetHeight = 0;

    private double targetFirstLineTime = 0;
    private double targetLastLineTime = 0;
    private double targetLineTimeInterval = 0;
    private double targetSlantRangeTimeToFirstPixel = 0;
    private double targetSlantRangeTimeToLastPixel = 0;
    private double targetDeltaSlantRangeTime = 0;
    private SubSwathEffectStartEndPixels[] subSwathEffectStartEndPixels = null;

    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private boolean absoluteCalibrationPerformed = false;
    private boolean inputSigmaBand = false;
    private boolean inputBetaBand = false;
    private boolean inputGammaBand = false;
    private boolean inputDNBand = false;
    private boolean isSplitProduct = false;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public TOPSARDeburstOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

            getMission();

            getProductType();

            getAcquisitionMode();

            getCalibrationFlag();

            su = new Sentinel1Utils(sourceProduct);
            subSwath = su.getSubSwath();

            checkIfSplitProduct();

            if (selectedPolarisations == null || selectedPolarisations.length == 0) {
                selectedPolarisations = su.getPolarizations();
            }

            computeTargetStartEndTime();

            computeTargetSlantRangeTimeToFirstAndLastPixels();

            computeTargetWidthAndHeight();

            createTargetProduct();

            if (!isSplitProduct) {
                computeSubSwathEffectStartEndPixels();
            }

            updateTargetProductMetadata();

        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    /**
     * Get product mission from abstracted metadata.
     */
    private void getMission() {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.startsWith("SENTINEL-1")) {
            throw new OperatorException(mission + " is not a valid mission for Sentinel1 product");
        }
    }

    /**
     * Get product type from abstracted metadata.
     */
    private void getProductType() {
        productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        if (!productType.equals("SLC")) {
            throw new OperatorException(productType + " is not a SLC product");
        }
    }

    /**
     * Get acquisition mode from abstracted metadata.
     */
    private void getAcquisitionMode() {

        acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);

        switch (acquisitionMode) {
            case "IW":
                numOfSubSwath = 3;
                break;
            case "EW":
                numOfSubSwath = 5;
                break;
            default:
                throw new OperatorException("Acquisition mode is not IW or EW");
        }
    }

    /**
     * Get calibration flag from abstract metadata.
     */
    private void getCalibrationFlag() {

        absoluteCalibrationPerformed =
                absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().getElemBoolean();

        if (absoluteCalibrationPerformed) {
            final String[] sourceBandNames = sourceProduct.getBandNames();
            for (String bandName : sourceBandNames) {
                if (bandName.contains("Sigma0")) {
                    inputSigmaBand = true;
                } else if (bandName.contains("Gamma0")) {
                    inputGammaBand = true;
                } else if (bandName.contains("Beta0")) {
                    inputBetaBand = true;
                } else if (bandName.contains("DN")) {
                    inputDNBand = true;
                }
            }
        }
    }

    private void checkIfSplitProduct() {

        final String[] subSwathNames = su.getSubSwathNames();
        if (subSwathNames.length == 1) {
            isSplitProduct = true;
            subSwathIndex = Integer.parseInt(subSwathNames[0].substring(subSwathNames[0].length()-1));
        }
    }

    /**
     * Compute azimuth time for the first and last line in the target product.
     */
    private void computeTargetStartEndTime() {

        if (isSplitProduct) {

            targetFirstLineTime = subSwath[subSwathIndex - 1].firstLineTime;
            targetLastLineTime = subSwath[subSwathIndex - 1].lastLineTime;
            targetLineTimeInterval = subSwath[subSwathIndex - 1].azimuthTimeInterval; // days

        } else {

            targetFirstLineTime = subSwath[0].firstLineTime;
            targetLastLineTime = subSwath[0].lastLineTime;
            for (int i = 1; i < numOfSubSwath; i++) {
                if (targetFirstLineTime > subSwath[i].firstLineTime) {
                    targetFirstLineTime = subSwath[i].firstLineTime;
                }

                if (targetLastLineTime < subSwath[i].lastLineTime) {
                    targetLastLineTime = subSwath[i].lastLineTime;
                }
            }
            targetLineTimeInterval = subSwath[0].azimuthTimeInterval; // days
        }
    }

    /**
     * Compute slant range time to the first and last pixels in the target product.
     */
    private void computeTargetSlantRangeTimeToFirstAndLastPixels() {

        if (isSplitProduct) {
            targetSlantRangeTimeToFirstPixel = subSwath[subSwathIndex - 1].slrTimeToFirstPixel;
            targetSlantRangeTimeToLastPixel = subSwath[subSwathIndex - 1].slrTimeToLastPixel;
            targetDeltaSlantRangeTime = subSwath[subSwathIndex - 1].rangePixelSpacing / Constants.lightSpeed;
        } else {
            targetSlantRangeTimeToFirstPixel = subSwath[0].slrTimeToFirstPixel;
            targetSlantRangeTimeToLastPixel = subSwath[numOfSubSwath - 1].slrTimeToLastPixel;
            targetDeltaSlantRangeTime = subSwath[0].rangePixelSpacing / Constants.lightSpeed;
        }
    }

    /**
     * Compute target product dimension.
     */
    private void computeTargetWidthAndHeight() {

        targetHeight = (int)((targetLastLineTime - targetFirstLineTime) / targetLineTimeInterval);

        targetWidth = (int)((targetSlantRangeTimeToLastPixel - targetSlantRangeTimeToFirstPixel) /
                targetDeltaSlantRangeTime);
    }

    private void computeSubSwathEffectStartEndPixels() {

        subSwathEffectStartEndPixels = new SubSwathEffectStartEndPixels[numOfSubSwath];
        for (int i = 0; i < numOfSubSwath; i++) {
            subSwathEffectStartEndPixels[i] = new SubSwathEffectStartEndPixels();

            if (i == 0) {
                subSwathEffectStartEndPixels[i].xMin = 0;
            } else {
                final double midTime = (subSwath[i - 1].slrTimeToLastPixel + subSwath[i].slrTimeToFirstPixel) / 2.0;

                subSwathEffectStartEndPixels[i].xMin = (int)Math.round((midTime - subSwath[i].slrTimeToFirstPixel) /
                        targetDeltaSlantRangeTime);
            }

            if (i < numOfSubSwath - 1) {
                final double midTime = (subSwath[i].slrTimeToLastPixel + subSwath[i + 1].slrTimeToFirstPixel) / 2.0;

                subSwathEffectStartEndPixels[i].xMax = (int)Math.round((midTime - subSwath[i].slrTimeToFirstPixel) /
                        targetDeltaSlantRangeTime);
            } else {
                subSwathEffectStartEndPixels[i].xMax = (int)Math.round(
                        (subSwath[i].slrTimeToLastPixel - subSwath[i].slrTimeToFirstPixel) / targetDeltaSlantRangeTime);
            }
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), targetWidth, targetHeight);

        final Band[] sourceBands = sourceProduct.getBands();

        if (!absoluteCalibrationPerformed) {

            for (String pol : selectedPolarisations) {
                final Band srcBand = sourceBands[0];
                final Band trgI = targetProduct.addBand("i_" + pol, srcBand.getDataType());
                trgI.setUnit(Unit.REAL);
                trgI.setNoDataValueUsed(true);
                trgI.setNoDataValue(srcBand.getNoDataValue());

                final Band trgQ = targetProduct.addBand("q_" + pol, srcBand.getDataType());
                trgQ.setUnit(Unit.IMAGINARY);
                trgQ.setNoDataValueUsed(true);
                trgQ.setNoDataValue(srcBand.getNoDataValue());

                ReaderUtils.createVirtualIntensityBand(targetProduct, trgI, trgQ, '_' + pol);
            }

        } else {

            for (String pol : selectedPolarisations) {
                final Band srcBand = sourceBands[0];

                if (inputSigmaBand) {
                    final Band trgSigma = targetProduct.addBand("Sigma0_" + pol, srcBand.getDataType());
                    trgSigma.setUnit(Unit.INTENSITY);
                    trgSigma.setNoDataValueUsed(true);
                    trgSigma.setNoDataValue(srcBand.getNoDataValue());
                }

                if (inputBetaBand) {
                    final Band trgBeta = targetProduct.addBand("Beta0_" + pol, srcBand.getDataType());
                    trgBeta.setUnit(Unit.INTENSITY);
                    trgBeta.setNoDataValueUsed(true);
                    trgBeta.setNoDataValue(srcBand.getNoDataValue());
                }

                if (inputGammaBand) {
                    final Band trgGamma = targetProduct.addBand("Gamma0_" + pol, srcBand.getDataType());
                    trgGamma.setUnit(Unit.INTENSITY);
                    trgGamma.setNoDataValueUsed(true);
                    trgGamma.setNoDataValue(srcBand.getNoDataValue());
                }

                if (inputDNBand) {
                    final Band trgDN = targetProduct.addBand("DN_" + pol, srcBand.getDataType());
                    trgDN.setUnit(Unit.INTENSITY);
                    trgDN.setNoDataValueUsed(true);
                    trgDN.setNoDataValue(srcBand.getNoDataValue());
                }
            }
        }

        copyMetaData(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        targetProduct.setStartTime(new ProductData.UTC(targetFirstLineTime));
        targetProduct.setEndTime(new ProductData.UTC(targetLastLineTime));
        targetProduct.setDescription(sourceProduct.getDescription());

        createTiePointGrids();

        targetProduct.setPreferredTileSize(500, 50);
    }

    /**
     * Copy source product metadata to target product.
     *
     * @param source Source product root metadata element.
     * @param target Target product root metadata element.
     */
    private static void copyMetaData(final MetadataElement source, final MetadataElement target) {

        for (final MetadataElement element : source.getElements()) {
            target.addElement(element.createDeepClone());
        }

        for (final MetadataAttribute attribute : source.getAttributes()) {
            target.addAttribute(attribute.createDeepClone());
        }
    }

    /**
     * Create target product tie point grid.
     */
    private void createTiePointGrids() {

        final int gridWidth = 20;
        final int gridHeight = 5;

        final int subSamplingX = targetWidth / gridWidth;
        final int subSamplingY = targetHeight / gridHeight;

        final float[] latList = new float[gridWidth * gridHeight];
        final float[] lonList = new float[gridWidth * gridHeight];
        final float[] slrtList = new float[gridWidth * gridHeight];
        final float[] incList = new float[gridWidth * gridHeight];

        int k = 0;
        for (int i = 0; i < gridHeight; i++) {
            final int y = i * subSamplingY;
            final double azTime = targetFirstLineTime + y * targetLineTimeInterval;
            for (int j = 0; j < gridWidth; j++) {
                final int x = j * subSamplingX;
                final double slrTime = targetSlantRangeTimeToFirstPixel + x * targetDeltaSlantRangeTime;
                latList[k] = (float) su.getLatitude(azTime, slrTime);
                lonList[k] = (float) su.getLongitude(azTime, slrTime);
                slrtList[k] = (float) (su.getSlantRangeTime(azTime, slrTime) * 2 * Constants.oneBillion); // 2-way ns
                incList[k] = (float) su.getIncidenceAngle(azTime, slrTime);
                k++;
            }
        }

        final TiePointGrid latGrid = new TiePointGrid(
                OperatorUtils.TPG_LATITUDE, gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, latList);

        final TiePointGrid lonGrid = new TiePointGrid(
                OperatorUtils.TPG_LONGITUDE, gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, lonList);

        final TiePointGrid slrtGrid = new TiePointGrid(
                OperatorUtils.TPG_SLANT_RANGE_TIME, gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, slrtList);

        final TiePointGrid incGrid = new TiePointGrid(
                OperatorUtils.TPG_INCIDENT_ANGLE, gridWidth, gridHeight, 0, 0, subSamplingX, subSamplingY, incList);

        latGrid.setUnit(Unit.DEGREES);
        lonGrid.setUnit(Unit.DEGREES);
        slrtGrid.setUnit(Unit.NANOSECONDS);
        incGrid.setUnit(Unit.DEGREES);

        targetProduct.addTiePointGrid(latGrid);
        targetProduct.addTiePointGrid(lonGrid);
        targetProduct.addTiePointGrid(slrtGrid);
        targetProduct.addTiePointGrid(incGrid);
    }

    /**
     * Update target product metadata.
     */
    private void updateTargetProductMetadata() {

        updateAbstractMetadata();
        updateOriginalMetadata();
    }

    private void updateAbstractMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_output_lines, targetHeight);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.num_samples_per_line, targetWidth);
        absTgt.setAttributeUTC(AbstractMetadata.first_line_time, new ProductData.UTC(targetFirstLineTime));
        absTgt.setAttributeUTC(AbstractMetadata.last_line_time, new ProductData.UTC(targetLastLineTime));
        absTgt.setAttributeDouble(AbstractMetadata.line_time_interval, targetLineTimeInterval * Constants.secondsInDay); // days to s

        TiePointGrid latGrid = targetProduct.getTiePointGrid(OperatorUtils.TPG_LATITUDE);
        TiePointGrid lonGrid = targetProduct.getTiePointGrid(OperatorUtils.TPG_LONGITUDE);

        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_near_lat, latGrid.getPixelFloat(0, 0));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_near_long, lonGrid.getPixelFloat(0, 0));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_far_lat, latGrid.getPixelFloat(targetWidth, 0));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_far_long, lonGrid.getPixelFloat(targetWidth, 0));

        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_near_lat, latGrid.getPixelFloat(0, targetHeight));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_near_long, lonGrid.getPixelFloat(0, targetHeight));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_far_lat, latGrid.getPixelFloat(targetWidth, targetHeight));
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_far_long, lonGrid.getPixelFloat(targetWidth, targetHeight));
    }

    private void updateOriginalMetadata() {

        if (!isSplitProduct) {
            updateCalibrationVector();
            //updateNoiseVector(); //todo: not implemented yet
        }
    }

    private void updateCalibrationVector() {

        final String[] selectedPols = Sentinel1Utils.getProductPolarizations(absRoot);
        final MetadataElement srcCalibration = AbstractMetadata.getOriginalProductMetadata(sourceProduct).
                getElement("calibration");
        final MetadataElement bandCalibration = srcCalibration.getElementAt(0).getElement("calibration");

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(targetProduct);
        origProdRoot.removeElement(origProdRoot.getElement("calibration"));
        final MetadataElement calibration = new MetadataElement("calibration");
        for (String pol : selectedPols) {
            final String elemName = "s1a-" + acquisitionMode + "-" + productType + "-" + pol;
            final MetadataElement elem = new MetadataElement(elemName);
            final MetadataElement calElem = bandCalibration.createDeepClone();
            final MetadataElement calibrationVectorListElem = calElem.getElement("calibrationVectorList");
            final MetadataElement[] list = calibrationVectorListElem.getElements();
            int vectorIndex = 0;
            final String mergedPixelStr = getMergedPixels(pol);
            final StringTokenizer tokenizer = new StringTokenizer(mergedPixelStr, " ");
            final int count = tokenizer.countTokens();
            for (MetadataElement calibrationVectorElem : list) {

                final MetadataElement pixelElem = calibrationVectorElem.getElement("pixel");
                pixelElem.setAttributeString("pixel", mergedPixelStr);
                pixelElem.setAttributeString("count", Integer.toString(count));

                final MetadataElement sigmaNoughtElem = calibrationVectorElem.getElement("sigmaNought");
                final String mergedSigmaNoughtStr = getMergedVector("SigmaNought", pol, vectorIndex);
                sigmaNoughtElem.setAttributeString("sigmaNought", mergedSigmaNoughtStr);
                sigmaNoughtElem.setAttributeString("count", Integer.toString(count));


                final MetadataElement betaNoughtElem = calibrationVectorElem.getElement("betaNought");
                final String mergedBetaNoughtStr = getMergedVector("betaNought", pol, vectorIndex);
                betaNoughtElem.setAttributeString("betaNought", mergedBetaNoughtStr);
                betaNoughtElem.setAttributeString("count", Integer.toString(count));

                final MetadataElement gammaNoughtElem = calibrationVectorElem.getElement("gamma");
                final String mergedGammaNoughtStr = getMergedVector("gamma", pol, vectorIndex);
                gammaNoughtElem.setAttributeString("gamma", mergedGammaNoughtStr);
                gammaNoughtElem.setAttributeString("count", Integer.toString(count));

                final MetadataElement dnElem = calibrationVectorElem.getElement("dn");
                final String mergedDNStr = getMergedVector("dn", pol, vectorIndex);
                dnElem.setAttributeString("dn", mergedDNStr);
                dnElem.setAttributeString("count", Integer.toString(count));
                vectorIndex++;
            }
            elem.addElement(calElem);
            calibration.addElement(elem);
        }
        origProdRoot.addElement(calibration);
    }

    private String getMergedPixels(final String pol) {

        String mergedPixelStr = "";
        for (int s = 0; s < numOfSubSwath; s++) {
            final int[] pixelArray = su.getCalibrationPixel(s+1, pol, 0);
            for (int p:pixelArray) {
                if (p >= subSwathEffectStartEndPixels[s].xMin && p < subSwathEffectStartEndPixels[s].xMax) {
                    final double slrt = subSwath[s].slrTimeToFirstPixel + p * targetDeltaSlantRangeTime;

                    final int targetPixelIdx = (int)((slrt - targetSlantRangeTimeToFirstPixel) /
                            targetDeltaSlantRangeTime);

                    mergedPixelStr += targetPixelIdx + " ";
                }
            }
        }
        return mergedPixelStr;
    }

    private String getMergedVector(final String vectorName, final String pol, final int vectorIndex) {

        final StringBuilder mergedVectorStr = new StringBuilder();
        for (int s = 0; s < numOfSubSwath; s++) {
            final int[] pixelArray = su.getCalibrationPixel(s+1, pol, vectorIndex);
            final float[] vectorArray = su.getCalibrationVector(s+1, pol, vectorIndex, vectorName);
            for (int i = 0; i < pixelArray.length; i++) {
                if (pixelArray[i] >= subSwathEffectStartEndPixels[s].xMin &&
                        pixelArray[i] < subSwathEffectStartEndPixels[s].xMax) {

                    mergedVectorStr.append(vectorArray[i]).append(" ");
                }
            }
        }
        return mergedVectorStr.toString();
    }


    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles The current tiles to be computed for each target band.
     * @param pm          A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        try {
            final int tx0 = targetRectangle.x;
            final int ty0 = targetRectangle.y;
            final int tw = targetRectangle.width;
            final int th = targetRectangle.height;
            final double tileSlrtToFirstPixel = targetSlantRangeTimeToFirstPixel + tx0 * targetDeltaSlantRangeTime;
            final double tileSlrtToLastPixel = targetSlantRangeTimeToFirstPixel + (tx0 + tw - 1) * targetDeltaSlantRangeTime;
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            // determine subswaths covered by the tile
            int firstSubSwathIndex = 0;
            int lastSubSwathIndex = 0;

            if (isSplitProduct) {
                firstSubSwathIndex = subSwathIndex;
                lastSubSwathIndex = subSwathIndex;

            } else {

                for (int i = 0; i < numOfSubSwath; i++) {
                    if (tileSlrtToFirstPixel >= subSwath[i].slrTimeToFirstPixel &&
                            tileSlrtToFirstPixel <= subSwath[i].slrTimeToLastPixel) {
                        firstSubSwathIndex = i + 1;
                        break;
                    }
                }

                if (firstSubSwathIndex == numOfSubSwath) {
                    lastSubSwathIndex = firstSubSwathIndex;
                } else {
                    for (int i = 0; i < numOfSubSwath; i++) {
                        if (tileSlrtToLastPixel >= subSwath[i].slrTimeToFirstPixel &&
                                tileSlrtToLastPixel <= subSwath[i].slrTimeToLastPixel) {
                            lastSubSwathIndex = i + 1;
                        }
                    }
                }
            }

            final int numOfSourceTiles = lastSubSwathIndex - firstSubSwathIndex + 1;
            final boolean tileInOneSubSwath = (numOfSourceTiles == 1);

            final Rectangle[] sourceRectangle = new Rectangle[numOfSourceTiles];
            int k = 0;
            for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
                sourceRectangle[k++] = getSourceRectangle(tx0, ty0, tw, th, i);
            }

            final BurstInfo burstInfo = new BurstInfo();
            final int txMax = tx0 + tw;
            final int tyMax = ty0 + th;

            if (!absoluteCalibrationPerformed) {

                final String bandNameI = "i_" + acquisitionMode;
                final String bandNameQ = "q_" + acquisitionMode;

                for (String pol : selectedPolarisations) {
                    if (pol != null) {
                        final Tile targetTileI = targetTiles.get(targetProduct.getBand("i_" + pol));
                        final Tile targetTileQ = targetTiles.get(targetProduct.getBand("q_" + pol));

                        if (tileInOneSubSwath) {
                            computeTileInOneSwath(tx0, ty0, txMax, tyMax, firstSubSwathIndex, pol,
                                    sourceRectangle, bandNameI, bandNameQ, targetTileI, targetTileQ, burstInfo);

                        } else {
                            computeMultipleSubSwaths(tx0, ty0, txMax, tyMax, firstSubSwathIndex, lastSubSwathIndex, pol,
                                    sourceRectangle, bandNameI, bandNameQ, targetTileI, targetTileQ, burstInfo);

                        }
                    }
                }

            } else {

                for (String pol : selectedPolarisations) {
                    if (pol != null) {

                        if (inputSigmaBand) {
                            final String bandName = "Sigma0_" + acquisitionMode;
                            final Tile targetTile = targetTiles.get(targetProduct.getBand("Sigma0_" + pol));
                            if (tileInOneSubSwath) {
                                computeTileInOneSwath(tx0, ty0, txMax, tyMax, firstSubSwathIndex, pol,
                                        sourceRectangle, bandName, targetTile, burstInfo);
                            } else {
                                computeMultipleSubSwaths(tx0, ty0, txMax, tyMax, firstSubSwathIndex, lastSubSwathIndex,
                                        pol, sourceRectangle, bandName, targetTile, burstInfo);
                            }
                        }

                        if (inputBetaBand) {
                            final String bandName = "Beta0_" + acquisitionMode;
                            final Tile targetTile = targetTiles.get(targetProduct.getBand("Beta0_" + pol));
                            if (tileInOneSubSwath) {
                                computeTileInOneSwath(tx0, ty0, txMax, tyMax, firstSubSwathIndex, pol,
                                        sourceRectangle, bandName, targetTile, burstInfo);
                            } else {
                                computeMultipleSubSwaths(tx0, ty0, txMax, tyMax, firstSubSwathIndex, lastSubSwathIndex,
                                        pol, sourceRectangle, bandName, targetTile, burstInfo);
                            }
                        }

                        if (inputGammaBand) {
                            final String bandName = "Gamma0_" + acquisitionMode;
                            final Tile targetTile = targetTiles.get(targetProduct.getBand("Gamma0_" + pol));
                            if (tileInOneSubSwath) {
                                computeTileInOneSwath(tx0, ty0, txMax, tyMax, firstSubSwathIndex, pol,
                                        sourceRectangle, bandName, targetTile, burstInfo);
                            } else {
                                computeMultipleSubSwaths(tx0, ty0, txMax, tyMax, firstSubSwathIndex, lastSubSwathIndex,
                                        pol, sourceRectangle, bandName, targetTile, burstInfo);
                            }
                        }

                        if (inputDNBand) {
                            final String bandName = "DN_" + acquisitionMode;
                            final Tile targetTile = targetTiles.get(targetProduct.getBand("DN_" + pol));
                            if (tileInOneSubSwath) {
                                computeTileInOneSwath(tx0, ty0, txMax, tyMax, firstSubSwathIndex, pol,
                                        sourceRectangle, bandName, targetTile, burstInfo);
                            } else {
                                computeMultipleSubSwaths(tx0, ty0, txMax, tyMax, firstSubSwathIndex, lastSubSwathIndex,
                                        pol, sourceRectangle, bandName, targetTile, burstInfo);
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            throw new OperatorException(e.getMessage());
        }
    }

    // For original SLC product
    private void computeTileInOneSwath(final int tx0, final int ty0, final int txMax, final int tyMax,
                                       final int firstSubSwathIndex, final String pol,
                                       final Rectangle[] sourceRectangle,
                                       final String bandNameI, final String bandNameQ,
                                       final Tile targetTileI, final Tile targetTileQ,
                                       final BurstInfo burstInfo) {

        final int yMin = computeYMin(subSwath[firstSubSwathIndex - 1]);
        final int yMax = computeYMax(subSwath[firstSubSwathIndex - 1]);
        final int firstY = Math.max(ty0, yMin);
        final int lastY = Math.min(tyMax, yMax + 1);

        if (firstY >= lastY)
            return;

        final Band srcBandI = sourceProduct.getBand(bandNameI + firstSubSwathIndex + '_' + pol);
        final Band srcBandQ = sourceProduct.getBand(bandNameQ + firstSubSwathIndex + '_' + pol);
        final Tile sourceRasterI = getSourceTile(srcBandI, sourceRectangle[0]);
        final Tile sourceRasterQ = getSourceTile(srcBandQ, sourceRectangle[0]);
        final TileIndex srcTileIndex = new TileIndex(sourceRasterI);
        final TileIndex tgtIndex = new TileIndex(targetTileI);

        final short[] srcArrayI = (short[]) sourceRasterI.getDataBuffer().getElems();
        final short[] srcArrayQ = (short[]) sourceRasterQ.getDataBuffer().getElems();
        final short[] tgtArrayI = (short[]) targetTileI.getDataBuffer().getElems();
        final short[] tgtArrayQ = (short[]) targetTileQ.getDataBuffer().getElems();

        for (int y = firstY; y < lastY; y++) {

            if (!getLineIndicesInSourceProduct(y, subSwath[firstSubSwathIndex - 1], burstInfo)) {
                continue;
            }

            final int tgtOffset = tgtIndex.calculateStride(y);
            final Sentinel1Utils.SubSwathInfo firstSubSwath = subSwath[firstSubSwathIndex - 1];
            int offset;
            if (burstInfo.sy1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
                offset = srcTileIndex.calculateStride(burstInfo.sy1);
            } else {
                offset = srcTileIndex.calculateStride(burstInfo.sy0);
            }

            final int sx = (int) Math.round(((targetSlantRangeTimeToFirstPixel + tx0 * targetDeltaSlantRangeTime)
                    - firstSubSwath.slrTimeToFirstPixel) / targetDeltaSlantRangeTime);

            System.arraycopy(srcArrayI, sx - offset, tgtArrayI, tx0 - tgtOffset, txMax - tx0);
            System.arraycopy(srcArrayQ, sx - offset, tgtArrayQ, tx0 - tgtOffset, txMax - tx0);
        }
    }

    // For original SLC product
    private void computeMultipleSubSwaths(final int tx0, final int ty0, final int txMax, final int tyMax,
                                          final int firstSubSwathIndex, final int lastSubSwathIndex, final String pol,
                                          final Rectangle[] sourceRectangle,
                                          final String bandNameI, final String bandNameQ,
                                          final Tile targetTileI, final Tile targetTileQ,
                                          final BurstInfo burstInfo) {
        final int numOfSourceTiles = lastSubSwathIndex - firstSubSwathIndex + 1;
        final TileIndex tgtIndex = new TileIndex(targetTileI);
        final Tile[] srcTiles = new Tile[numOfSourceTiles];

        final short[][] srcArrayI = new short[numOfSourceTiles][];
        final short[][] srcArrayQ = new short[numOfSourceTiles][];
        final short[] tgtArrayI = (short[]) targetTileI.getDataBuffer().getElems();
        final short[] tgtArrayQ = (short[]) targetTileQ.getDataBuffer().getElems();

        int k = 0;
        for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
            final Band srcBandI = sourceProduct.getBand(bandNameI + i + '_' + pol);
            final Band srcBandQ = sourceProduct.getBand(bandNameQ + i + '_' + pol);
            final Tile sourceRasterI = getSourceTile(srcBandI, sourceRectangle[k]);
            final Tile sourceRasterQ = getSourceTile(srcBandQ, sourceRectangle[k]);
            srcTiles[k] = sourceRasterI;

            srcArrayI[k] = (short[]) sourceRasterI.getDataBuffer().getElems();
            srcArrayQ[k] = (short[]) sourceRasterQ.getDataBuffer().getElems();
            k++;
        }

        int sy;
        for (int y = ty0; y < tyMax; y++) {
            final int tgtOffset = tgtIndex.calculateStride(y);

            for (int x = tx0; x < txMax; x++) {

                int subswathIndex = getSubSwathIndex(x, y, firstSubSwathIndex, lastSubSwathIndex, pol, burstInfo);
                if (subswathIndex == -1) {
                    continue;
                }
                if (!getLineIndicesInSourceProduct(y, subSwath[subswathIndex - 1], burstInfo)) {
                    continue;
                }

                short iVal = 0, qVal = 0;
                k = subswathIndex - firstSubSwathIndex;

                int sx = getSampleIndexInSourceProduct(x, subSwath[subswathIndex - 1]);
                if (burstInfo.sy1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
                    sy = burstInfo.sy1;
                } else {
                    sy = burstInfo.sy0;
                }
                int idx = srcTiles[k].getDataBufferIndex(sx, sy);

                if (idx >= 0) {
                    iVal = srcArrayI[k][idx];
                    qVal = srcArrayQ[k][idx];
                }

                double intensity = iVal * iVal + qVal * qVal;
                //if(burstInfo.swath1 != -1 && iVal == 0 && qVal == 0) {
                if (burstInfo.swath1 != -1 && intensity < 300) {
                    // edge of swaths found therefore use other swath
                    if (subswathIndex == burstInfo.swath0)
                        subswathIndex = burstInfo.swath1;
                    else
                        subswathIndex = burstInfo.swath0;

                    getLineIndicesInSourceProduct(y, subSwath[subswathIndex - 1], burstInfo);

                    k = subswathIndex - firstSubSwathIndex;

                    sx = getSampleIndexInSourceProduct(x, subSwath[subswathIndex - 1]);
                    if (burstInfo.sy1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
                        sy = burstInfo.sy1;
                    } else {
                        sy = burstInfo.sy0;
                    }
                    idx = srcTiles[k].getDataBufferIndex(sx, sy);

                    if (idx >= 0 && !(srcArrayI[k][idx] == 0 && srcArrayQ[k][idx] == 0)) {
                        iVal = srcArrayI[k][idx];
                        qVal = srcArrayQ[k][idx];
                    }
                }
                tgtArrayI[x - tgtOffset] = iVal;
                tgtArrayQ[x - tgtOffset] = qVal;
            }
        }
    }

    // For calibrated product
    private void computeTileInOneSwath(final int tx0, final int ty0, final int txMax, final int tyMax,
                                       final int firstSubSwathIndex, final String pol,
                                       final Rectangle[] sourceRectangle,
                                       final String bandName, final Tile targetTile,
                                       final BurstInfo burstInfo) {

        final int yMin = computeYMin(subSwath[firstSubSwathIndex - 1]);
        final int yMax = computeYMax(subSwath[firstSubSwathIndex - 1]);
        final int firstY = Math.max(ty0, yMin);
        final int lastY = Math.min(tyMax, yMax + 1);

        if (firstY >= lastY)
            return;

        final Band srcBand = sourceProduct.getBand(bandName + firstSubSwathIndex + '_' + pol);
        final Tile sourceRaster = getSourceTile(srcBand, sourceRectangle[0]);
        final TileIndex srcTileIndex = new TileIndex(sourceRaster);
        final TileIndex tgtIndex = new TileIndex(targetTile);

        final float[] srcArray = (float[]) sourceRaster.getDataBuffer().getElems();
        final float[] tgtArray = (float[]) targetTile.getDataBuffer().getElems();

        for (int y = firstY; y < lastY; y++) {

            if (!getLineIndicesInSourceProduct(y, subSwath[firstSubSwathIndex - 1], burstInfo)) {
                continue;
            }

            final int tgtOffset = tgtIndex.calculateStride(y);
            final Sentinel1Utils.SubSwathInfo firstSubSwath = subSwath[firstSubSwathIndex - 1];
            int offset;
            if (burstInfo.sy1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
                offset = srcTileIndex.calculateStride(burstInfo.sy1);
            } else {
                offset = srcTileIndex.calculateStride(burstInfo.sy0);
            }

            final int sx = (int) Math.round(((targetSlantRangeTimeToFirstPixel + tx0 * targetDeltaSlantRangeTime)
                    - firstSubSwath.slrTimeToFirstPixel) / targetDeltaSlantRangeTime);

            System.arraycopy(srcArray, sx - offset, tgtArray, tx0 - tgtOffset, txMax - tx0);
        }
    }

    // For calibrated product
    private void computeMultipleSubSwaths(final int tx0, final int ty0, final int txMax, final int tyMax,
                                          final int firstSubSwathIndex, final int lastSubSwathIndex, final String pol,
                                          final Rectangle[] sourceRectangle, final String bandName,
                                          final Tile targetTile, final BurstInfo burstInfo) {

        final int numOfSourceTiles = lastSubSwathIndex - firstSubSwathIndex + 1;
        final TileIndex tgtIndex = new TileIndex(targetTile);
        final Tile[] srcTiles = new Tile[numOfSourceTiles];

        final float[][] srcArray = new float[numOfSourceTiles][];
        final float[] tgtArray = (float[]) targetTile.getDataBuffer().getElems();

        int k = 0;
        double noDataValue = 0.0;
        for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
            final Band srcBand = sourceProduct.getBand(bandName + i + '_' + pol);
            final Tile sourceRaster = getSourceTile(srcBand, sourceRectangle[k]);
            srcTiles[k] = sourceRaster;
            srcArray[k] = (float[]) sourceRaster.getDataBuffer().getElems();
            if (k == 0) {
                noDataValue = srcBand.getNoDataValue();
            }
            k++;
        }

        int sy;
        for (int y = ty0; y < tyMax; y++) {
            final int tgtOffset = tgtIndex.calculateStride(y);

            for (int x = tx0; x < txMax; x++) {

                int subswathIndex = getSubSwathIndex(x, y, firstSubSwathIndex, lastSubSwathIndex, pol, burstInfo);
                if (subswathIndex == -1) {
                    continue;
                }
                if (!getLineIndicesInSourceProduct(y, subSwath[subswathIndex - 1], burstInfo)) {
                    continue;
                }

                float intensity = 0.0f;
                k = subswathIndex - firstSubSwathIndex;

                int sx = getSampleIndexInSourceProduct(x, subSwath[subswathIndex - 1]);
                if (burstInfo.sy1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
                    sy = burstInfo.sy1;
                } else {
                    sy = burstInfo.sy0;
                }
                int idx = srcTiles[k].getDataBufferIndex(sx, sy);

                if (idx >= 0) {
                    intensity = srcArray[k][idx];
                }

                if (burstInfo.swath1 != -1 && intensity == noDataValue) {
                    // edge of swaths found therefore use other swath
                    if (subswathIndex == burstInfo.swath0) {
                        subswathIndex = burstInfo.swath1;
                    } else {
                        subswathIndex = burstInfo.swath0;
                    }
                    getLineIndicesInSourceProduct(y, subSwath[subswathIndex - 1], burstInfo);

                    k = subswathIndex - firstSubSwathIndex;

                    sx = getSampleIndexInSourceProduct(x, subSwath[subswathIndex - 1]);
                    if (burstInfo.sy1 != -1 && burstInfo.targetTime > burstInfo.midTime) {
                        sy = burstInfo.sy1;
                    } else {
                        sy = burstInfo.sy0;
                    }
                    idx = srcTiles[k].getDataBufferIndex(sx, sy);

                    if (idx >= 0 && srcArray[k][idx] != noDataValue) {
                        intensity = srcArray[k][idx];
                    }
                }
                tgtArray[x - tgtOffset] = intensity;
            }
        }
    }

    /**
     * Get source tile rectangle.
     *
     * @param tx0           X coordinate for the upper left corner pixel in the target tile.
     * @param ty0           Y coordinate for the upper left corner pixel in the target tile.
     * @param tw            The target tile width.
     * @param th            The target tile height.
     * @param subSwathIndex The subswath index.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceRectangle(
            final int tx0, final int ty0, final int tw, final int th, final int subSwathIndex) {

        final Sentinel1Utils.SubSwathInfo sw = subSwath[subSwathIndex - 1];
        final int x0 = getSampleIndexInSourceProduct(tx0, sw);
        final int xMax = getSampleIndexInSourceProduct(tx0 + tw - 1, sw);

        final BurstInfo burstTimes = new BurstInfo();
        getLineIndicesInSourceProduct(ty0, sw, burstTimes);
        int y0;
        if (burstTimes.sy0 == -1 && burstTimes.sy1 == -1) {
            y0 = 0;
        } else {
            y0 = burstTimes.sy0;
        }

        getLineIndicesInSourceProduct(ty0 + th - 1, sw, burstTimes);
        int yMax;
        if (burstTimes.sy0 == -1 && burstTimes.sy1 == -1) {
            yMax = sw.numOfLines - 1;
        } else {
            yMax = Math.max(burstTimes.sy0, burstTimes.sy1);
        }

        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;

        return new Rectangle(x0, y0, w, h);
    }

    private int getSampleIndexInSourceProduct(final int tx, final Sentinel1Utils.SubSwathInfo subSwath) {
        final int sx = (int)((((targetSlantRangeTimeToFirstPixel + tx * targetDeltaSlantRangeTime)
                - subSwath.slrTimeToFirstPixel) / targetDeltaSlantRangeTime)+0.5);
        return sx < 0 ? 0 : sx > subSwath.numOfSamples - 1 ? subSwath.numOfSamples - 1 : sx;
    }

    private boolean getLineIndicesInSourceProduct(
            final int ty, final Sentinel1Utils.SubSwathInfo subSwath, final BurstInfo burstTimes) {

        final double targetLineTime = targetFirstLineTime + ty * targetLineTimeInterval;
        burstTimes.targetTime = targetLineTime;
        burstTimes.sy0 = -1;
        burstTimes.sy1 = -1;
        int k = 0;
        for (int i = 0; i < subSwath.numOfBursts; i++) {
            if (targetLineTime >= subSwath.burstFirstLineTime[i] && targetLineTime < subSwath.burstLastLineTime[i]) {
                final int sy = i * subSwath.linesPerBurst +
                        (int)(((targetLineTime - subSwath.burstFirstLineTime[i]) / subSwath.azimuthTimeInterval)+0.5);
                if (k == 0) {
                    burstTimes.sy0 = sy;
                    burstTimes.burstNum0 = i;
                } else {
                    burstTimes.sy1 = sy;
                    burstTimes.burstNum1 = i;
                    break;
                }
                ++k;
            }
        }

        if (burstTimes.sy0 != -1 && burstTimes.sy1 != -1) {
            // find time between bursts midTime
            // use first burst if targetLineTime is before midTime
            burstTimes.midTime = (subSwath.burstLastLineTime[burstTimes.burstNum0] +
                    subSwath.burstFirstLineTime[burstTimes.burstNum1]) / 2.0;
        }
        return burstTimes.sy0 != -1 || burstTimes.sy1 != -1;
    }

    private int computeYMin(final Sentinel1Utils.SubSwathInfo subSwath) {

        return (int) ((subSwath.firstLineTime - targetFirstLineTime) / targetLineTimeInterval);
    }

    private int computeYMax(final Sentinel1Utils.SubSwathInfo subSwath) {

        return (int) ((subSwath.lastLineTime - targetFirstLineTime) / targetLineTimeInterval);
    }

    private int getSubSwathIndex(final int tx, final int ty, final int firstSubSwathIndex, final int lastSubSwathIndex,
                                 final String pol, final BurstInfo burstInfo) {

        final double targetSampleSlrTime = targetSlantRangeTimeToFirstPixel + tx * targetDeltaSlantRangeTime;
        final double targetLineTime = targetFirstLineTime + ty * targetLineTimeInterval;

        burstInfo.swath0 = -1;
        burstInfo.swath1 = -1;
        int cnt = 0;
        Sentinel1Utils.SubSwathInfo info;
        for (int i = firstSubSwathIndex; i <= lastSubSwathIndex; i++) {
            int i_1 = i - 1;
            info = subSwath[i_1];
            if (targetLineTime >= info.firstLineTime &&
                    targetLineTime <= info.lastLineTime &&
                    targetSampleSlrTime >= info.slrTimeToFirstPixel &&
                    targetSampleSlrTime <= info.slrTimeToLastPixel) {

                if (cnt == 0) {
                    burstInfo.swath0 = i;
                } else {
                    burstInfo.swath1 = i;
                    break;
                }
                ++cnt;
            }
        }

        if (burstInfo.swath1 != -1) {

            final double middleTime = (subSwath[burstInfo.swath0 - 1].slrTimeToLastPixel +
                    subSwath[burstInfo.swath1 - 1].slrTimeToFirstPixel) / 2.0;

            if (targetSampleSlrTime > middleTime) {
                return burstInfo.swath1;
            }

            /* commented out because swaths are merged at the middle of the swath overlap
            final double noise0 = getSubSwathNoise(tx, targetLineTime, subSwath[burstInfo.swath0 - 1], pol);
            final double noise1 = getSubSwathNoise(tx, targetLineTime, subSwath[burstInfo.swath1 - 1], pol);
            if (noise0 > noise1) {
                return burstInfo.swath1;
            }
            */
        }
        return burstInfo.swath0;
    }

    private double getSubSwathNoise(final int tx, final double targetLineTime,
                                    final Sentinel1Utils.SubSwathInfo sw, final String pol) {

        final Sentinel1Utils.NoiseVector[] vectorList = sw.noise.get(pol);

        final int sx = getSampleIndexInSourceProduct(tx, sw);
        final int sy = (int) ((targetLineTime - vectorList[0].timeMJD) / targetLineTimeInterval);

        int l0 = -1, l1 = -1;
        int vectorIdx0 = -1, vectorIdxInc = 0;
        if (sy < vectorList[0].line) {

            l0 = vectorList[0].line;
            l1 = l0;
            vectorIdx0 = 0;

        } else if (sy >= vectorList[vectorList.length - 1].line) {

            l0 = vectorList[vectorList.length - 1].line;
            l1 = l0;
            vectorIdx0 = vectorList.length - 1;

        } else {
            vectorIdxInc = 1;
            int max = vectorList.length - 1;
            for (int i = 0; i < max; i++) {
                if (sy >= vectorList[i].line && sy < vectorList[i + 1].line) {
                    l0 = vectorList[i].line;
                    l1 = vectorList[i + 1].line;
                    vectorIdx0 = i;
                    break;
                }
            }
        }

        final int[] pixels = vectorList[vectorIdx0].pixels;
        int p0 = -1, p1 = -1;
        int pixelIdx0 = -1, pixelIdxInc = 0;
        if (sx < pixels[0]) {

            p0 = pixels[0];
            p1 = p0;
            pixelIdx0 = 0;

        } else if (sx >= pixels[pixels.length - 1]) {

            p0 = pixels[pixels.length - 1];
            p1 = p0;
            pixelIdx0 = pixels.length - 1;

        } else {

            pixelIdxInc = 1;
            int max = pixels.length - 1;
            for (int i = 0; i < max; i++) {
                if (sx >= pixels[i] && sx < pixels[i + 1]) {
                    p0 = pixels[i];
                    p1 = pixels[i + 1];
                    pixelIdx0 = i;
                    break;
                }
            }
        }

        final float[] noiseLUT0 = vectorList[vectorIdx0].noiseLUT;
        final float[] noiseLUT1 = vectorList[vectorIdx0 + vectorIdxInc].noiseLUT;
        double dx;
        if (p0 == p1) {
            dx = 0;
        } else {
            dx = (sx - p0) / (p1 - p0);
        }

        double dy;
        if (l0 == l1) {
            dy = 0;
        } else {
            dy = (sy - l0) / (l1 - l0);
        }

        return Maths.interpolationBiLinear(noiseLUT0[pixelIdx0], noiseLUT0[pixelIdx0 + pixelIdxInc],
                noiseLUT1[pixelIdx0], noiseLUT1[pixelIdx0 + pixelIdxInc],
                dx, dy);
    }

    private static class BurstInfo {
        public int sy0 = -1;
        public int sy1 = -1;
        public int swath0;
        public int swath1;
        public int burstNum0 = 0;
        public int burstNum1 = 0;

        public double targetTime;
        public double midTime;

        public BurstInfo() {
        }
    }

    private static class SubSwathEffectStartEndPixels {
        public int xMin;
        public int xMax;

        public SubSwathEffectStartEndPixels() {
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(TOPSARDeburstOp.class);
        }
    }
}