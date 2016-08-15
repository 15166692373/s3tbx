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

package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import com.bc.ceres.core.ProgressMonitor;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.esa.s3tbx.olci.radiometry.gaseousabsorption.GaseousAbsorptionAuxII;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

/**
 * @author muhammad.bc.
 */
@OperatorMetadata(alias = "Olci.RayleighCorrectionII",
        description = "Performs radiometric corrections on OLCI L1b data products.",
        authors = " Marco Peters, Muhammad Bala (Brockmann Consult)",
        copyright = "(c) 2015 by Brockmann Consult",
        category = "Optical/Pre-Processing",
        version = "1.2")
public class RayleighCorrectionOpII extends Operator {
    private static final String[] BAND_CATEGORIES = new String[]{
            "taur_%02d",
            "transSRay_%02d",
            "transVRay_%02d",
            "sARay_%02d",
            "rtoaRay_%02d",
            "rBRR_%02d",
            "sphericalAlbedoFactor_%02d",
            "RayleighSimple_%02d",
            "rtoa_ng_%02d",
            "taurS_%02d",
            "rtoa_%02d"
    };


    public static final String OLCI = "OLCI";
    @SourceProduct
    Product sourceProduct;

    private RayleighCorrAlgorithm algorithm;
    GaseousAbsorptionAuxII gaseousAbsorptionAuxII;
    private Sensor sensor;
    private double[] absorpOzone;


    @Override
    public void initialize() throws OperatorException {

        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        sensor = getSensorPattern();
        algorithm = new RayleighCorrAlgorithm();
        gaseousAbsorptionAuxII = new GaseousAbsorptionAuxII();
        absorpOzone = gaseousAbsorptionAuxII.absorptionOzone(sensor.toString());
        targetProduct.addBand("airmass", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("azidiff", ProductData.TYPE_FLOAT32);

        for (String bandCategory : BAND_CATEGORIES) {
            for (int i = 1; i <= sensor.getNumBands(); i++) {
                Band sourceBand = sourceProduct.getBand(String.format(sensor.getPattern, i));
                Band targetBand = targetProduct.addBand(String.format(bandCategory, i), ProductData.TYPE_FLOAT32);
                ProductUtils.copySpectralBandProperties(sourceBand, targetBand);
            }
        }


        ProductUtils.copyBand("altitude", sourceProduct, targetProduct, true);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping("RayleighSimple:taurS:rtoa:taur:transSRay:rtoa_ng:transVRay:sARay:rtoaRay:rBRR:sphericalAlbedoFactor");
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        Iterator<Band> iterator = targetTiles.keySet().iterator();
        HashMap<String, double[]> rhoBrrHashMap = new HashMap<>();
        HashMap<String, double[]> correctHashMap = new HashMap<>();

        while (iterator.hasNext()) {
            Band targetBand = iterator.next();
            Tile targetTile = targetTiles.get(targetBand);

            String targetBandName = targetBand.getName();
            float spectralWavelength = targetBand.getSpectralWavelength();
            String sourceBandName = null;
            if (spectralWavelength > 0) {
                sourceBandName = getSourceBand(spectralWavelength);
            }

            AuxiliaryValues auxiliaryValues = getRayleighValues(sensor, sourceBandName, targetRectangle);
            double[] sunZenithAngles = auxiliaryValues.getSunZenithAngles();
            double[] viewZenithAngles = auxiliaryValues.getViewZenithAngles();
            double[] sunAzimuthAngles = auxiliaryValues.getSunAzimuthAngles();
            double[] viewAzimuthAngles = auxiliaryValues.getViewAzimuthAngles();
            double[] altitudes = auxiliaryValues.getAltitudes();
            double[] latitudes = auxiliaryValues.getLatitudes();
            double[] seaLevels = auxiliaryValues.getSeaLevels();
            double[] solarFluxs = auxiliaryValues.getSolarFluxs();
            double[] lambdaSource = auxiliaryValues.getLambdaSource();
            double[] sourceSampleRad = auxiliaryValues.getSourceSampleRad();
            double[] totalOzones = auxiliaryValues.getTotalOzones();
            int sourceBandIndex = auxiliaryValues.getSourceBandIndex();


            if (targetBandName.equals("airmass")) {
                double[] massAirs = SmileUtils.getAirMass(sunZenithAngles, viewZenithAngles);
                targetTile.setSamples(massAirs);
                continue;
            } else if (targetBandName.equals("azidiff")) {
                double[] aziDifferences = SmileUtils.getAziDiff(sunAzimuthAngles, viewAzimuthAngles);
                targetTile.setSamples(aziDifferences);
                continue;
            }
            double[] sunZenithAngleRads = SmileUtils.convertDegreesToRadians(sunZenithAngles);
            double[] viewZenithAngleRads = SmileUtils.convertDegreesToRadians(viewZenithAngles);
            double[] reflectances = algorithm.convertRadsToRefls(sourceSampleRad, solarFluxs, sunZenithAngles);
            double[] crossSectionSigma = algorithm.getCrossSectionSigma(lambdaSource);
            double[] rayleighOpticalThickness = algorithm.getRayleighOpticalThicknessII(seaLevels, altitudes, latitudes, crossSectionSigma);

            boolean isRayleighSample = targetBandName.matches("RayleighSimple_\\d{2}");
            boolean isTaurS = targetBandName.matches("taurS_\\d{2}");

            if (isRayleighSample || isTaurS) {
                if (!correctHashMap.containsKey(targetBandName)) {
                    double[] phaseRaylMin = algorithm.getPhaseRaylMin(sunZenithAngleRads, sunAzimuthAngles, viewZenithAngleRads, viewAzimuthAngles);
                    correctHashMap = algorithm.correct(lambdaSource, seaLevels, altitudes, sunZenithAngleRads, viewZenithAngleRads, phaseRaylMin, sourceBandIndex);
                }
                if (isRayleighSample) {
                    setSamples(correctHashMap, targetTile);
                    continue;
                }
                if (isTaurS) {
                    setSamples(correctHashMap, targetTile);
                }

            } else if (targetBandName.matches("rtoa_\\d{2}")) {
                targetTile.setSamples(reflectances);
            } else if (targetBandName.matches("taur_\\d{2}")) {
                targetTile.setSamples(rayleighOpticalThickness);
            } else {
                if (Math.ceil(spectralWavelength) == 709) { // band 709
                    String bandNamePattern = sensor.getGetPattern();
                    int[] upperLowerBounds = sensor.getUpperLowerBounds();
                    double[] bWVRefTile = getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, upperLowerBounds[0])), targetRectangle).getSamplesDouble();
                    double[] bWVTile = getSourceTile(sourceProduct.getBand(String.format(bandNamePattern, upperLowerBounds[1])), targetRectangle).getSamplesDouble();
                    waterVaporCorrection709(reflectances, bWVRefTile, bWVTile);
                }
                double absorpO = absorpOzone[sourceBandIndex - 1];
                double[] corrOzoneRefl = algorithm.getCorrOzone(reflectances, totalOzones, sunZenithAngleRads, viewZenithAngleRads, absorpO);
                if (targetBandName.matches("rtoa_ng_\\d{2}")) {
                    targetTile.setSamples(corrOzoneRefl);
                    continue;
                }

                if (!rhoBrrHashMap.containsKey(targetBandName)) {
                    double[] viewAzimuthAngleRads = SmileUtils.convertDegreesToRadians(viewAzimuthAngles);
                    double[] sunAzimuthAngleRads = SmileUtils.convertDegreesToRadians(sunAzimuthAngles);
                    rhoBrrHashMap = algorithm.getRhoBrr(sunZenithAngles, viewZenithAngles, sunZenithAngleRads, viewZenithAngleRads, sunAzimuthAngleRads,
                            viewAzimuthAngleRads, rayleighOpticalThickness, corrOzoneRefl, sourceBandIndex);
                }
                setSamples(rhoBrrHashMap, targetTile);
            }

        }
    }


    private void setSamples(HashMap<String, double[]> rhoBrrHashMap, Tile targetTile) {
        double[] transSRays = rhoBrrHashMap.get(targetTile.getRasterDataNode().getName());
        targetTile.setSamples(transSRays);
    }

    private AuxiliaryValues getRayleighValues(Sensor sensor, String sourceBandName, Rectangle rectangle) {
        AuxiliaryValues auxiliaryValues;
        if (sensor.equals(Sensor.MERIS)) {
            auxiliaryValues = new AuxiliaryValues();
            auxiliaryValues.setAltitudes(getSourceTile(sourceProduct.getTiePointGrid("dem_alt"), rectangle).getSamplesDouble());
            auxiliaryValues.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid("sun_azimuth"), rectangle).getSamplesDouble());
            auxiliaryValues.setAltitudes(getSourceTile(sourceProduct.getTiePointGrid("dem_alt"), rectangle).getSamplesDouble());
            auxiliaryValues.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid("sun_zenith"), rectangle).getSamplesDouble());
            auxiliaryValues.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid("view_zenith"), rectangle).getSamplesDouble());
            auxiliaryValues.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid("view_azimuth"), rectangle).getSamplesDouble());
            auxiliaryValues.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid("atm_press"), rectangle).getSamplesDouble());
            auxiliaryValues.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid("ozone"), rectangle).getSamplesDouble());
            auxiliaryValues.setLatitudes(getSourceTile(sourceProduct.getTiePointGrid("latitude"), rectangle).getSamplesDouble());

            if (sourceBandName != null) {
                Band sourceBand = sourceProduct.getBand(sourceBandName);
                int size = rectangle.width * rectangle.height;
                double[] solarFluxs = new double[size];
                double[] lambdaSource = new double[size];
                Arrays.fill(solarFluxs, (double) sourceBand.getSolarFlux());
                Arrays.fill(lambdaSource, (double) sourceBand.getSpectralWavelength());

                auxiliaryValues.setSolarFluxs(solarFluxs);
                auxiliaryValues.setLambdaSource(lambdaSource);
                auxiliaryValues.setSourceBandIndex(Integer.parseInt(sourceBandName.substring(9, sourceBandName.length())));
                auxiliaryValues.setSourceSampleRad(getSourceTile(sourceBand, rectangle).getSamplesDouble());
            }
            return auxiliaryValues;

        } else if (sensor.equals(Sensor.OLCI)) {
            auxiliaryValues = new AuxiliaryValues();

            auxiliaryValues.setSunZenithAngles(getSourceTile(sourceProduct.getTiePointGrid("SZA"), rectangle).getSamplesDouble());
            auxiliaryValues.setViewZenithAngles(getSourceTile(sourceProduct.getTiePointGrid("OZA"), rectangle).getSamplesDouble());
            auxiliaryValues.setSunAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid("SAA"), rectangle).getSamplesDouble());
            auxiliaryValues.setViewAzimuthAngles(getSourceTile(sourceProduct.getTiePointGrid("OAA"), rectangle).getSamplesDouble());
            auxiliaryValues.setAltitudes(getSourceTile(sourceProduct.getBand("altitude"), rectangle).getSamplesDouble());
            auxiliaryValues.setSeaLevels(getSourceTile(sourceProduct.getTiePointGrid("sea_level_pressure"), rectangle).getSamplesDouble());
            auxiliaryValues.setTotalOzones(getSourceTile(sourceProduct.getTiePointGrid("total_ozone"), rectangle).getSamplesDouble());
            auxiliaryValues.setLatitudes(getSourceTile(sourceProduct.getTiePointGrid("TP_latitude"), rectangle).getSamplesDouble());

            if (sourceBandName != null) {
                String extractBandIndex = sourceBandName.substring(2, 4);
                int sourceBandIndex = Integer.parseInt(extractBandIndex);
                auxiliaryValues.setSourceBandIndex(sourceBandIndex);
                auxiliaryValues.setSolarFluxs(getSourceTile(sourceProduct.getBand(String.format("solar_flux_band_%d", sourceBandIndex)), rectangle).getSamplesDouble());
                auxiliaryValues.setLambdaSource(getSourceTile(sourceProduct.getBand(String.format("lambda0_band_%d", sourceBandIndex)), rectangle).getSamplesDouble());
                auxiliaryValues.setSourceSampleRad(getSourceTile(sourceProduct.getBand(sourceBandName), rectangle).getSamplesDouble());
            }
            return auxiliaryValues;
        }
        throw new IllegalArgumentException("Sensor is not supported");
    }

    private void waterVaporCorrection709(double[] reflectances, double[] bWVRefTile, double[] bWVTile) {
        double[] H2O_COR_POLY = new double[]{0.3832989, 1.6527957, -1.5635101, 0.5311913};  // Polynomial coefficients for WV transmission @ 709nm
        // in order to optimise performance we do:
        // trans709 = H2O_COR_POLY[0] + (H2O_COR_POLY[1] + (H2O_COR_POLY[2] + H2O_COR_POLY[3] * X2) * X2) * X2
        // when X2 = 1
        // trans709 = 0.3832989 + ( 1.6527957+ (-1.5635101+ 0.5311913*1)*1)*1
        double trans709 = 1.0037757999999999;
        for (int i = 0; i < bWVTile.length; i++) {
            if (bWVRefTile[i] > 0) {
                double X2 = bWVTile[i] / bWVRefTile[i];
                trans709 = H2O_COR_POLY[0] + (H2O_COR_POLY[1] + (H2O_COR_POLY[2] + H2O_COR_POLY[3] * X2) * X2) * X2;
            }
            reflectances[i] = reflectances[i] / trans709;
        }
    }

    //todo mba/** write a test
    private String getSourceBand(float spectralWavelength) {
        Band[] bands = sourceProduct.getBands();
        List<Band> collectBand = Arrays.stream(bands).filter(p -> p.getSpectralWavelength() == spectralWavelength &&
                !p.getName().contains("err")).collect(Collectors.toList());
        return collectBand.get(0).getName();
    }

    private Sensor getSensorPattern() {
        String[] bandNames = getSourceProduct().getBandNames();
        boolean isSensor = Stream.of(bandNames).anyMatch(p -> p.matches("Oa\\d+_radiance"));
        if (isSensor) {
            return Sensor.OLCI;
        }
        isSensor = Stream.of(bandNames).anyMatch(p -> p.matches("radiance_\\d+"));

        if (isSensor) {
            return Sensor.MERIS;
        }
        throw new OperatorException("The operator can't be applied on the sensor");
    }

    private enum Sensor {
        MERIS("radiance_%d", 15, new int[]{13, 14}),
        OLCI("Oa%02d_radiance", 21, new int[]{17, 18});

        public int[] getUpperLowerBounds() {
            return side;
        }

        private final int[] side;
        final int numBands;
        final String getPattern;


        public int getNumBands() {
            return numBands;
        }

        public String getGetPattern() {
            return getPattern;
        }

        Sensor(String getPattern, int numBands, int[] side) {
            this.numBands = numBands;
            this.getPattern = getPattern;
            this.side = side;
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(RayleighCorrectionOpII.class);
        }
    }

}
