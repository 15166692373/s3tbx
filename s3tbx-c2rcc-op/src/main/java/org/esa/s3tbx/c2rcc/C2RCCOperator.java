package org.esa.s3tbx.c2rcc;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.pointop.PixelOperator;
import org.esa.snap.framework.gpf.pointop.ProductConfigurer;
import org.esa.snap.framework.gpf.pointop.Sample;
import org.esa.snap.framework.gpf.pointop.SampleConfigurer;
import org.esa.snap.framework.gpf.pointop.WritableSample;
import org.esa.snap.util.BitRaster;
import org.esa.snap.util.ProductUtils;
import org.esa.snap.util.converters.BooleanExpressionConverter;

import java.io.IOException;


// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs.
// todo (nf) - Add min/max values of NN inputs and outputs to metadata

/**
 * The Case 2 Regional / CoastColour Operator. Computes AC-reflectances and IOPs from MERIS L1b data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra
 */
@OperatorMetadata(alias = "C2RCC", version = "0.1a",
        authors = "Roland Doerffer, Norman Fomferra (Brockmann Consult)",
        category = "Optical Processing/Thematic Water Processing",
        copyright = "Copyright (C) 2015 by Brockmann Consult",
        description = "Performs atmospheric correction and IOP retrieval on MERIS L1b data products.")
public class C2RCCOperator extends PixelOperator {

    // MERIS bands
    public static final int CONC_APIG_IX = C2RCCAlgorithm.merband12_ix.length;
    public static final int CONC_ADET_IX = C2RCCAlgorithm.merband12_ix.length + 1;
    public static final int CONC_AGELB_IX = C2RCCAlgorithm.merband12_ix.length + 2;
    public static final int CONC_BPART_IX = C2RCCAlgorithm.merband12_ix.length + 3;
    public static final int CONC_BWIT_IX = C2RCCAlgorithm.merband12_ix.length + 4;

    public static final int BAND_COUNT = 15;
    public static final int DEM_ALT_IX = BAND_COUNT;
    public static final int SUN_ZEN_IX = BAND_COUNT + 1;
    public static final int SUN_AZI_IX = BAND_COUNT + 2;
    public static final int VIEW_ZEN_IX = BAND_COUNT + 3;
    public static final int VIEW_AZI_IX = BAND_COUNT + 4;
    public static final int ATM_PRESS_IX = BAND_COUNT + 5;
    public static final int OZONE_IX = BAND_COUNT + 6;

    @SourceProduct(label = "MERIS L1b product", description = "MERIS L1b source product.")
    private Product source;

    @Parameter(label = "Valid pixel expression", defaultValue = "!l1_flags.INVALID && !l1_flags.LAND_OCEAN",
            converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "DU", interval = "(0, 100)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(-50, 50)")
    private double temperature;

    private C2RCCAlgorithm algorithm;
    private BitRaster mask;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        if (mask.isSet(x, y)) {

            double[] radiances = new double[BAND_COUNT];
            for (int i = 0; i < BAND_COUNT; i++) {
                radiances[i] = sourceSamples[i].getDouble();
            }

            GeoPos geoPos = source.getGeoCoding().getGeoPos(new PixelPos(x + 0.5f, y + 0.5f), null);
            C2RCCAlgorithm.Result result = algorithm.processPixel(x, y, geoPos.getLat(), geoPos.getLon(),
                                                                  radiances,
                                                                  sourceSamples[SUN_ZEN_IX].getDouble(),
                                                                  sourceSamples[SUN_AZI_IX].getDouble(),
                                                                  sourceSamples[VIEW_AZI_IX].getDouble(),
                                                                  sourceSamples[VIEW_ZEN_IX].getDouble(),
                                                                  sourceSamples[DEM_ALT_IX].getDouble(),
                                                                  sourceSamples[ATM_PRESS_IX].getDouble(),
                                                                  sourceSamples[OZONE_IX].getDouble()
            );

            for (int i = 0; i < result.rw.length; i++) {
                targetSamples[i].set(result.rw[i]);
            }

            for (int i = 0; i < result.iops.length; i++) {
                targetSamples[result.rw.length + i].set(result.iops[i]);
            }

        } else {
            for (WritableSample targetSample : targetSamples) {
                targetSample.set(Float.NaN);
            }
        }
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        for (int i = 0; i < BAND_COUNT; i++) {
            sampleConfigurer.defineSample(i, "radiance_" + (i + 1));
        }
        sampleConfigurer.defineSample(DEM_ALT_IX, "dem_alt");
        sampleConfigurer.defineSample(SUN_ZEN_IX, "sun_zenith");
        sampleConfigurer.defineSample(SUN_AZI_IX, "sun_azimuth");
        sampleConfigurer.defineSample(VIEW_ZEN_IX, "view_zenith");
        sampleConfigurer.defineSample(VIEW_AZI_IX, "view_azimuth");
        sampleConfigurer.defineSample(ATM_PRESS_IX, "atm_press");
        sampleConfigurer.defineSample(OZONE_IX, "ozone");
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        for (int i = 0; i < C2RCCAlgorithm.merband12_ix.length; i++) {
            sampleConfigurer.defineSample(i, "reflec_" + C2RCCAlgorithm.merband12_ix[i]);
        }
        sampleConfigurer.defineSample(CONC_APIG_IX, "conc_apig");
        sampleConfigurer.defineSample(CONC_ADET_IX, "conc_adet");
        sampleConfigurer.defineSample(CONC_AGELB_IX, "conc_agelb");
        sampleConfigurer.defineSample(CONC_BPART_IX, "conc_bpart");
        sampleConfigurer.defineSample(CONC_BWIT_IX, "conc_bwit");
    }


    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();

        Product targetProduct = productConfigurer.getTargetProduct();

        ProductUtils.copyFlagBands(source, targetProduct, true);

        for (int index : C2RCCAlgorithm.merband12_ix) {
            Band reflecBand = targetProduct.addBand("reflec_" + index, ProductData.TYPE_FLOAT32);
            ProductUtils.copySpectralBandProperties(source.getBand("radiance_" + index), reflecBand);
            reflecBand.setUnit("1");
        }

        //output  1 is log_conc_apig in [-13.170000,1.671000]
        //output  2 is log_conc_adet in [-9.903000,1.782000]
        //output  3 is log_conc_agelb in [-9.903000,0.000000]
        //output  4 is log_conc_bpart in [-6.908000,4.081000]
        //output  5 is log_conc_bwit in [-6.908000,4.076000]
        addBand(targetProduct, "conc_apig", "m^-1", "Pigment absorption coefficient");
        addBand(targetProduct, "conc_adet", "m^-1", "Pigment absorption");
        addBand(targetProduct, "conc_agelb", "m^-1", "Yellow substance absorption coefficient");
        addBand(targetProduct, "conc_bpart", "m^-1", "");
        addBand(targetProduct, "conc_bwit", "m^-1", "Backscattering of suspended particulate matter");

        addVirtualBand(targetProduct, "tsm", "(conc_bpart + conc_bwit) * 1.7", "g m^-3", "Total suspended matter dry weight concentration");
        addVirtualBand(targetProduct, "atot", "conc_apig + conc_adet + conc_agelb", "m^-1", "Total absorption coefficient of all water constituents");
        addVirtualBand(targetProduct, "chl", "pow(conc_apig, 1.04) * 20.0", "m^-1", "Chlorophyll concentration");
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        for (int i = 0; i < BAND_COUNT; i++) {
            assertSourceBand("radiance_" + (i + 1));
        }
        assertSourceBand("l1_flags");

        if (source.getGeoCoding() == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }
        if (!source.isCompatibleBandArithmeticExpression(validPixelExpression)) {
            throw new OperatorException("The given valid-pixel expression can not be used with the given source product.");
        }

        try {
            mask = source.createValidMask(validPixelExpression, ProgressMonitor.NULL);
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        double[] solflux = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            solflux[i] = source.getBand("radiance_" + (i + 1)).getSolarFlux();
        }

        try {
            algorithm = new C2RCCAlgorithm();
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);
        if(isSolfluxValid(solflux)) {
            algorithm.setSolflux(solflux);
        }
    }

    private boolean isSolfluxValid(double[] solflux) {
        for (double v : solflux) {
            if(v == 0) {
                return false;
            }
        }
        return true;
    }

    private void assertSourceBand(String name) {
        if (source.getBand(name) == null) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    private void addBand(Product targetProduct, String name, String unit, String description) {
        Band targetBand = targetProduct.addBand(name, ProductData.TYPE_FLOAT32);
        targetBand.setUnit(unit);
        targetBand.setDescription(description);
        targetBand.setNoDataValue(Double.NaN);
        targetBand.setNoDataValueUsed(true);
    }

    private void addVirtualBand(Product targetProduct, String name, String expression, String unit, String description) {
        Band band = targetProduct.addBand(name, expression);
        band.setUnit(unit);
        band.setDescription(description);
        band.getSourceImage();
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2RCCOperator.class);
        }
    }

}
