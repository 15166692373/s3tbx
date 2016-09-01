/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.smilecorr;

import com.bc.ceres.core.ProgressMonitor;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import org.esa.s3tbx.olci.radiometry.gaseousabsorption.GaseousAbsorptionAux;
import org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighAux;
import org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighCorrAlgorithm;
import org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighCorrectionOp;
import org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighOutput;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.RsMathUtils;


import static org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighCorrectionOp.ALTITUDE;
import static org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighCorrectionOp.OAA;
import static org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighCorrectionOp.SAA;
import static org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighCorrectionOp.SEA_LEVEL_PRESSURE;
import static org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighCorrectionOp.TOTAL_OZONE;
import static org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighCorrectionOp.TP_LATITUDE;
import static org.esa.s3tbx.olci.radiometry.rayleighcorrection.RayleighCorrectionOp.TP_LONGITUDE;


/**
 * @author muhammad.bc
 */
@OperatorMetadata(alias = "Olci.SmileCorrection",
        description = "Performs radiometric corrections on OLCI L1b data products.",
        authors = " Marco Peters ,Muhammad Bala (Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")
public class SmileCorretionOp extends Operator {


    public static final int NUM_BANDS = 21;
    public static final String BAND_NAME_PATTERN = "Oa%02d_radiance";
    public static final String WATER_EXPRESSION = "not quality_flags_land";
    public static final String SZA = "SZA";
    private static final String LAMBDA0_BAND_NAME_PATTERN = "lambda0_band_%d";
    private static final String SOLAR_FLUX_BAND_NAME_PATTERN = "solar_flux_band_%d";
    private static final String OA_RADIANCE_BAND_NAME_PATTERN = "Oa%02d_radiance";
    private static final String OA_RADIANCE_ERR_BAND_NAME_PATTERN = "Oa%02d_radiance_err";
    public static final String FWHM_BAND_PATTERN = "FWHM_band_%d";
    public static final String ALTITUDE_BAND = "altitude";
    public static final String LATITUDE_BAND = "latitude";
    public static final String LONGITUDE_BAND = "longitude";
    public static final String DETECTOR_INDEX_BAND = "detector_index";
    public static final String FRAME_OFFSET_BAND = "frame_offset";
    public static final String BEFORE_BAND = "before";
    public static final String OLCI_SENSOR = "OLCI";
    public static final int DO_NOT_CORRECT_BAND = -1;
    private static SmileCorrectionAuxdata smileAuxdata = new SmileCorrectionAuxdata();
    private Mask waterMask;

    @SourceProduct(alias = "source", label = "Name", description = "The source product.")
    private Product sourceProduct;

    @Parameter(defaultValue = "false", description = "Applies Rayleigh correction", label = "Apply Rayleigh correction")
    private boolean applyRayleigh;

    private RayleighCorrAlgorithm rayleighCorrAlgorithm;
    private double[] absorpOzones;
    private double[] crossSectionSigma;
    private Map<Integer, double[]> thicknessAllBand;

    @Override
    public void initialize() throws OperatorException {
        if (!sourceProduct.isCompatibleBandArithmeticExpression(WATER_EXPRESSION)) {
            throw new OperatorException("Can not evaluate expression'" + WATER_EXPRESSION + "' on source product");
        }

        if (applyRayleigh) {
            RayleighAux.initDefaultAuxiliary();
            rayleighCorrAlgorithm = new RayleighCorrAlgorithm();
            absorpOzones = new GaseousAbsorptionAux().absorptionOzone(OLCI_SENSOR);
        }
        // Configure the target
        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        boolean[] landRefCorrectionSwitches = smileAuxdata.getLandRefCorrectionSwitches();
        boolean[] waterRefCorrectionSwitches = smileAuxdata.getWaterRefCorrectionSwitches();
        float[] refCentralWaveLengths = smileAuxdata.getRefCentralWaveLengths();

        createTargetBands(targetProduct, OA_RADIANCE_BAND_NAME_PATTERN, landRefCorrectionSwitches, waterRefCorrectionSwitches, refCentralWaveLengths);
        createTargetLambda(targetProduct, LAMBDA0_BAND_NAME_PATTERN, landRefCorrectionSwitches, waterRefCorrectionSwitches, refCentralWaveLengths);
        createTargetBands(targetProduct, SOLAR_FLUX_BAND_NAME_PATTERN, landRefCorrectionSwitches, waterRefCorrectionSwitches, refCentralWaveLengths);

        copyTargetBandsImage(targetProduct, OA_RADIANCE_ERR_BAND_NAME_PATTERN);
        copyTargetBandsImage(targetProduct, FWHM_BAND_PATTERN);

        copyTargetBandImage(targetProduct, ALTITUDE_BAND);
        copyTargetBandImage(targetProduct, LATITUDE_BAND);
        copyTargetBandImage(targetProduct, LONGITUDE_BAND);
        copyTargetBandImage(targetProduct, DETECTOR_INDEX_BAND);
        copyTargetBandImage(targetProduct, FRAME_OFFSET_BAND);
        copyTargetBandImage(targetProduct, BEFORE_BAND);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        setTargetProduct(targetProduct);

        waterMask = Mask.BandMathsType.create("__water_mask", null,
                getSourceProduct().getSceneRasterWidth(),
                getSourceProduct().getSceneRasterHeight(),
                WATER_EXPRESSION,
                Color.GREEN, 0.0);
        waterMask.setOwner(getSourceProduct());
    }

    private void createTargetLambda(Product targetProduct, String lambdaBandNamePattern, boolean[] landRefCorrectionSwitches, boolean[] waterRefCorrectionSwitches, float[] refCentralWaveLengths) {
        for (int i = 0; i < refCentralWaveLengths.length; i++) {
            String sourceBandName = String.format(lambdaBandNamePattern, i + 1);
            if (landRefCorrectionSwitches[i] && waterRefCorrectionSwitches[i]) {
                RenderedOp image = ConstantDescriptor.create((float) sourceProduct.getSceneRasterWidth(), (float) sourceProduct.getSceneRasterHeight(), new Float[]{refCentralWaveLengths[i]}, null);
                ProductUtils.copyBand(sourceBandName, sourceProduct, targetProduct, false);
                targetProduct.getBand(sourceBandName).setSourceImage(image);
            } else if (!landRefCorrectionSwitches[i] && !waterRefCorrectionSwitches[i]) {
                ProductUtils.copyBand(sourceBandName, sourceProduct, targetProduct, true);
            } else {
                createTargetBand(targetProduct, sourceBandName);
            }
        }
    }

    private void copyTargetBandsImage(Product targetProduct, String bandNamePattern) {
        for (int i = 1; i <= 21; i++) {
            String sourceBandName = String.format(bandNamePattern, i);
            copyTargetBandImage(targetProduct, sourceBandName);
        }
    }

    private void copyTargetBandImage(Product targetProduct, String bandName) {
        if (sourceProduct.containsBand(bandName)) {
            ProductUtils.copyBand(bandName, sourceProduct, targetProduct, true);
        }
    }

    private void createTargetBands(Product targetProduct, String bandNamePattern, boolean[] landRefCorrectionSwitches, boolean[] waterRefCorrectionSwitches, float[] refCentralWaveLengths) {
        for (int i = 0; i < refCentralWaveLengths.length; i++) {
            String sourceBandName = String.format(bandNamePattern, i + 1);
            if (landRefCorrectionSwitches[i] || waterRefCorrectionSwitches[i]) {
                createTargetBand(targetProduct, sourceBandName);
            } else if (!landRefCorrectionSwitches[i] && !waterRefCorrectionSwitches[i]) {
                ProductUtils.copyBand(sourceBandName, sourceProduct, targetProduct, true);
            }
        }
    }

    private void createTargetBand(Product targetProduct, String bandNamePattern) {
        Band targetBand = targetProduct.addBand(bandNamePattern, ProductData.TYPE_FLOAT32);
        Band sourceBand = sourceProduct.getBand(bandNamePattern);
        targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
        targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
        targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
        targetBand.setNoDataValueUsed(true);
        targetBand.setNoDataValue(Double.NaN);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        String targetBandName = targetBand.getName();
        final float[] refCentralWaveLengths = smileAuxdata.getRefCentralWaveLengths();

        if (targetBandName.matches("Oa\\d{2}_radiance")) {
            String extractBandIndex = targetBandName.substring(2, 4);
            correctRad(targetTile, targetBandName, extractBandIndex, refCentralWaveLengths, pm);
        }

        if (targetBandName.matches("lambda0_band_\\d+")) {
            correctLambda(targetTile, targetBandName, refCentralWaveLengths, pm);
        }

        if (targetBandName.matches("solar_flux_band_\\d+")) {
            correctSolarFlux(targetTile, targetBandName, refCentralWaveLengths, pm);
        }
    }


    private void correctSolarFlux(Tile targetTile, String targetBandName, float[] refCentralWavelength, ProgressMonitor pm) {
        Rectangle rectangle = targetTile.getRectangle();

        String extractBandIndex = targetBandName.substring(16, targetBandName.length());
        int targetBandIndex = Integer.parseInt(extractBandIndex) - 1;

        boolean correctLand = smileAuxdata.getLandRefCorrectionSwitches()[targetBandIndex];
        boolean correctWater = smileAuxdata.getWaterRefCorrectionSwitches()[targetBandIndex];

        Band lambdaSourceBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, targetBandIndex + 1));
        Band effectiveSolarIrradianceBand = sourceProduct.getBand(targetBandName);

        Tile sourceLambdaTile = getSourceTile(lambdaSourceBand, rectangle);
        Tile solarIrradianceTile = getSourceTile(effectiveSolarIrradianceBand, rectangle);
        Tile waterMaskTile = getSourceTile(waterMask, rectangle);

        float refCentralWaveLength = refCentralWavelength[targetBandIndex];
        for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
            if (pm.isCanceled()) {
                return;
            }
            for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                float solarIrradianceSample = solarIrradianceTile.getSampleFloat(x, y);
                float sourceTargetLambda = sourceLambdaTile.getSampleFloat(x, y);
                if (sourceTargetLambda == -1 || solarIrradianceSample == -1) {
                    continue;
                }
                if (waterMaskTile.getSampleBoolean(x, y)) {
                    if (correctWater) {
                        float shiftedSolarIrradiance = shiftSolarIrradiance(solarIrradianceSample, sourceTargetLambda, refCentralWaveLength);
                        targetTile.setSample(x, y, shiftedSolarIrradiance);
                    } else {
                        targetTile.setSample(x, y, solarIrradianceSample);
                    }
                } else {
                    if (correctLand) {
                        float shiftedSolarIrradiance = shiftSolarIrradiance(solarIrradianceSample, sourceTargetLambda, refCentralWaveLength);
                        targetTile.setSample(x, y, shiftedSolarIrradiance);
                    } else {
                        targetTile.setSample(x, y, solarIrradianceSample);
                    }
                }
            }
        }
    }

    private void correctLambda(Tile targetTile, String targetBandName, float[] refCentralWaveLengths, ProgressMonitor pm) {
        Rectangle rectangle = targetTile.getRectangle();
        Tile sourceLambdaTile = getSourceTile(sourceProduct.getBand(targetBandName), rectangle);
        String extractBandIndex = targetBandName.substring(13, targetBandName.length());
        int targetBandIndex = Integer.parseInt(extractBandIndex) - 1;
        boolean correctLand = smileAuxdata.getLandRefCorrectionSwitches()[targetBandIndex];
        boolean correctWater = smileAuxdata.getWaterRefCorrectionSwitches()[targetBandIndex];

        float refCentralWaveLength = refCentralWaveLengths[targetBandIndex];
        final Tile waterMaskTile = getSourceTile(waterMask, rectangle);
        for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
            if (pm.isCanceled()) {
                return;
            }
            for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                if (waterMaskTile.getSampleBoolean(x, y)) {
                    if (correctWater) {
                        targetTile.setSample(x, y, refCentralWaveLength);
                    } else {
                        targetTile.setSample(x, y, sourceLambdaTile.getSampleFloat(x, y));
                    }
                } else {
                    if (correctLand) {
                        targetTile.setSample(x, y, refCentralWaveLength);
                    } else {
                        targetTile.setSample(x, y, sourceLambdaTile.getSampleFloat(x, y));
                    }
                }
            }
        }
    }

    private void correctRad(Tile targetTile, String targetBandName, String substring, float[] refCentralWaveLengths, ProgressMonitor pm) {
        Rectangle rectangle = targetTile.getRectangle();
        int targetBandIndex = Integer.parseInt(substring) - 1;
        Tile szaTile = getSourceTile(sourceProduct.getTiePointGrid(SZA), rectangle);
        Tile sourceRadianceTile = getSourceTile(sourceProduct.getBand(targetBandName), rectangle);

        Band effectiveSolarIrradianceBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, targetBandIndex + 1));
        Tile solarIrradianceTile = getSourceTile(effectiveSolarIrradianceBand, rectangle);

        int waterLowerBandIndex = smileAuxdata.getWaterLowerBands()[targetBandIndex];
        int waterUpperBandIndex = smileAuxdata.getWaterUpperBands()[targetBandIndex];
        int landLowerBandIndex = smileAuxdata.getLandLowerBands()[targetBandIndex];
        int landUpperBandIndex = smileAuxdata.getLandUpperBands()[targetBandIndex];

        Band lambdaWaterLowerBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, waterLowerBandIndex));
        Band radianceWaterLowerBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, waterLowerBandIndex));
        Band solarIrradianceWaterLowerBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, waterLowerBandIndex));
        Band lambdaWaterUpperBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, waterUpperBandIndex));
        Band radianceWaterUpperBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, waterUpperBandIndex));
        Band solarIrradianceWaterUpperBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, waterUpperBandIndex));
        Band lambdaLandLowerBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, landLowerBandIndex));
        Band radianceLandLowerBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, landLowerBandIndex));
        Band solarIrradianceLandLowerBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, landLowerBandIndex));
        Band lambdaLandUpperBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, landUpperBandIndex));
        Band radianceLandUpperBand = sourceProduct.getBand(String.format(OA_RADIANCE_BAND_NAME_PATTERN, landUpperBandIndex));
        Band solarIrradianceLandUpperBand = sourceProduct.getBand(String.format(SOLAR_FLUX_BAND_NAME_PATTERN, landUpperBandIndex));


        Band lambdaSourceBand = sourceProduct.getBand(String.format(LAMBDA0_BAND_NAME_PATTERN, targetBandIndex + 1));

        boolean correctLand = smileAuxdata.getLandRefCorrectionSwitches()[targetBandIndex];
        boolean correctWater = smileAuxdata.getWaterRefCorrectionSwitches()[targetBandIndex];


        if (!correctLand && !correctWater) {
            float[] samplesFloat = sourceRadianceTile.getSamplesFloat();
            targetTile.setSamples(samplesFloat);
            return;
        }

        SmileTiles waterTiles = null;
        RayleighAux rayleighAux = null;

        if (correctWater) {
            waterTiles = new SmileTiles(lambdaWaterLowerBand, radianceWaterLowerBand, solarIrradianceWaterLowerBand, lambdaWaterUpperBand,
                    radianceWaterUpperBand, solarIrradianceWaterUpperBand, rectangle);

            if (applyRayleigh) {
                rayleighAux = prepareRayleighAux(rectangle);
                thicknessAllBand = getThicknessAllBands(rayleighAux);
            }
        }
        SmileTiles landTiles = null;
        if (correctLand) {
            landTiles = new SmileTiles(lambdaLandLowerBand, radianceLandLowerBand, solarIrradianceLandLowerBand, lambdaLandUpperBand,
                    radianceLandUpperBand, solarIrradianceLandUpperBand, rectangle);
        }

        Tile lambdaSourceTile = getSourceTile(lambdaSourceBand, rectangle);

        final Tile waterMaskTile = getSourceTile(waterMask, rectangle);
        float refCentralWaveLength = refCentralWaveLengths[targetBandIndex];

        for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
            if (pm.isCanceled()) {
                return;
            }
            for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                if (!sourceRadianceTile.isSampleValid(x, y)) {
                    continue;
                }
                float sourceRadiance = sourceRadianceTile.getSampleFloat(x, y);
                if (waterMaskTile.getSampleBoolean(x, y)) {
                    if (correctWater) {
                        float correctedRadiance = correctForSmileEffect(sourceRadiance, solarIrradianceTile, lambdaSourceTile,
                                refCentralWaveLength, waterTiles,
                                szaTile, rayleighAux, targetBandIndex, x, y);
                        targetTile.setSample(x, y, correctedRadiance);
                    } else {
                        targetTile.setSample(x, y, sourceRadiance);
                    }
                } else {
                    if (correctLand) {
                        float correctedRadiance = correctForSmileEffect(sourceRadiance, solarIrradianceTile, lambdaSourceTile,
                                refCentralWaveLength, landTiles,
                                szaTile, rayleighAux, targetBandIndex, x, y);
                        targetTile.setSample(x, y, correctedRadiance);
                    } else {
                        targetTile.setSample(x, y, sourceRadiance);
                    }
                }

            }
        }
    }

    private Map<Integer, double[]> getThicknessAllBands(RayleighAux rayleighAux) {
        double[] crossSectionSigma = rayleighCorrAlgorithm.getCrossSectionSigma(getSourceProduct(), NUM_BANDS, BAND_NAME_PATTERN);
        Map<Integer, double[]> thicknessPerBand = new HashMap<>();
        for (int bandIndex = 1; bandIndex <= NUM_BANDS; bandIndex++) {
            double[] rayleighThickness = rayleighCorrAlgorithm.getRayleighThickness(rayleighAux, crossSectionSigma, bandIndex);
            thicknessPerBand.put(bandIndex, rayleighThickness);
        }
        return thicknessPerBand;
    }

    private float correctForSmileEffect(float radiance, Tile solarIrradianceTile, Tile effectWavelengthTargetTile,
                                        float refCentralWaveLength, SmileTiles smileTiles,
                                        Tile szaTile, RayleighAux rayleighAux, int targetBandIndx, int x, int y) {
        float sza = szaTile.getSampleFloat(x, y);
        float sourceTargetLambda = effectWavelengthTargetTile.getSampleFloat(x, y);

        float solarIrradiance = solarIrradianceTile.getSampleFloat(x, y);

        float sourceRefl = convertRadToRefl(radiance, solarIrradiance, sza);

        float lowerRefl = convertRadToRefl(smileTiles.getLowerRadianceTile().getSampleFloat(x, y),
                smileTiles.getLowerSolarIrradianceTile().getSampleFloat(x, y),
                sza);
        float upperRefl = convertRadToRefl(smileTiles.getUpperRadianceTile().getSampleFloat(x, y),
                smileTiles.getUpperSolarIrradianceTile().getSampleFloat(x, y), sza);


        if (applyRayleigh) {
            int lowerWaterIndx = smileAuxdata.getWaterLowerBands()[targetBandIndx] - 1;
            int upperWaterIndx = smileAuxdata.getWaterUpperBands()[targetBandIndx] - 1;

            if (lowerWaterIndx != DO_NOT_CORRECT_BAND && upperWaterIndx != DO_NOT_CORRECT_BAND) {
                int minY = solarIrradianceTile.getMinY();
                int minX = solarIrradianceTile.getMinX();
                int maxY = solarIrradianceTile.getMaxY();
                //ref: org/esa/snap/core/gpf/DirectDriverTest.java:561
                int indexInArray = y * solarIrradianceTile.getWidth() + x;
                RayleighInput rayleighInputToCompute = new RayleighInput(sourceRefl, lowerRefl, upperRefl, targetBandIndx, lowerWaterIndx, upperWaterIndx);
                RayleighOutput computedRayleighOutput = rayleighCorrAlgorithm.getRayleighReflectance(rayleighInputToCompute, rayleighAux, absorpOzones, thicknessAllBand, indexInArray);
                sourceRefl = computedRayleighOutput.getSourceRayRefl();
                lowerRefl = computedRayleighOutput.getLowerRayRefl();
                upperRefl = computedRayleighOutput.getUpperRayRefl();
            }
        }

        float lowerLambda = smileTiles.getLowerLambdaTile().getSampleFloat(x, y);
        float upperLambda = smileTiles.getUpperLambdaTile().getSampleFloat(x, y);

        float correctedReflectance = SmileCorrectionAlgorithm.correctWithReflectance(sourceRefl, lowerRefl,
                upperRefl, sourceTargetLambda, lowerLambda, upperLambda, refCentralWaveLength);

        float shiftedSolarIrradiance = shiftSolarIrradiance(solarIrradiance, sourceTargetLambda, refCentralWaveLength);
        return convertReflToRad(correctedReflectance, sza, shiftedSolarIrradiance);
    }

    private RayleighAux prepareRayleighAux(Rectangle rectangle) {
        RayleighAux rayleighAux = new RayleighAux();
        rayleighAux.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(SZA), rectangle));
        rayleighAux.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid(RayleighCorrectionOp.OZA), rectangle));
        rayleighAux.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(SAA), rectangle));
        rayleighAux.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid(OAA), rectangle));
        rayleighAux.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid(SEA_LEVEL_PRESSURE), rectangle));
        rayleighAux.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid(TOTAL_OZONE), rectangle));
        rayleighAux.setLatitudes(getSourceTile(sourceProduct.getTiePointGrid(TP_LATITUDE), rectangle));
        rayleighAux.setLongitude(getSourceTile(sourceProduct.getTiePointGrid(TP_LONGITUDE), rectangle));
        rayleighAux.setAltitudes(getSourceTile(sourceProduct.getBand(ALTITUDE), rectangle));
        return rayleighAux;
    }


    private float shiftSolarIrradiance(float solarIrradiance, float sourceTargetLambda, float refCentralWaveLength) {
//        poly =  2.329521314*10^(-10)* x^5 - 8.883158295*10^(-7)* x^4 + 1.341545977*10^(-3)*x^3 - 1.001512583* x^2 + 366.3249385* x - 50292.30277
//        dy/dx = 5 * 2.329521314*10^(-10)* x^4 - 4 * 8.883158295*10^(-7)* x^3 + 3 * 1.341545977*10^(-3)*x^2 - 2 * 1.001512583* x + 366.3249385
//        double forthDegree  = 5 * 2.329521314e-10 = 1.164760657E-9;
//        double thirdDegree  = 4 * 8.883158295e-7  = 3.553263318E-6;
//        double secondDegree = 3 * 1.341545977e-3 = 0.004024637931;
//        double firstDegree  = 2 * 1.001512583 = 2.003025166;
        double m = 1.164760657E-9 * Math.pow(sourceTargetLambda, 4) - 3.553263318E-6 * Math.pow(sourceTargetLambda, 3) + 0.004024637931 * Math.pow(sourceTargetLambda, 2) - 2.003025166 * Math.pow(sourceTargetLambda, 1) + 366.3249385;
        return (float) (solarIrradiance + m * (refCentralWaveLength - sourceTargetLambda));
    }


    private float convertRadToRefl(float radiance, float solarIrradiance, float sza) {
        return RsMathUtils.radianceToReflectance(radiance, sza, solarIrradiance);
    }

    private float convertReflToRad(float refl, float sza, float solarIrradiance) {
        return RsMathUtils.reflectanceToRadiance(refl, sza, solarIrradiance);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SmileCorretionOp.class);
        }
    }

    private class SmileTiles {

        private Tile lowerLambdaTile;
        private Tile upperLambdaTile;
        private Tile lowerRadianceTile;
        private Tile upperRadianceTile;
        private Tile lowerSolarIrradianceTile;
        private Tile upperSolarIrradianceTile;

        public SmileTiles(Band lowerLambdaBand, Band lowerRadianceBand, Band lowerSolarIrradianceBand,
                          Band upperLambdaBand, Band upperRadianceBand, Band upperSolarIrradianceBand,
                          Rectangle rectangle) {
            lowerLambdaTile = getSourceTile(lowerLambdaBand, rectangle);
            upperLambdaTile = getSourceTile(upperLambdaBand, rectangle);
            lowerRadianceTile = getSourceTile(lowerRadianceBand, rectangle);
            upperRadianceTile = getSourceTile(upperRadianceBand, rectangle);
            lowerSolarIrradianceTile = getSourceTile(lowerSolarIrradianceBand, rectangle);
            upperSolarIrradianceTile = getSourceTile(upperSolarIrradianceBand, rectangle);
        }

        public Tile getLowerLambdaTile() {
            return lowerLambdaTile;
        }

        public Tile getUpperLambdaTile() {
            return upperLambdaTile;
        }

        public Tile getLowerRadianceTile() {
            return lowerRadianceTile;
        }

        public Tile getUpperRadianceTile() {
            return upperRadianceTile;
        }

        public Tile getLowerSolarIrradianceTile() {
            return lowerSolarIrradianceTile;
        }

        public Tile getUpperSolarIrradianceTile() {
            return upperSolarIrradianceTile;
        }

    }


}
