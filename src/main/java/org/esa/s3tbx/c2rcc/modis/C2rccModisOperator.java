package org.esa.s3tbx.c2rcc.modis;

import org.esa.s3tbx.c2rcc.C2rccConfigurable;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdata;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataBuilder;
import org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import java.io.IOException;

import static org.esa.s3tbx.c2rcc.C2rccCommons.*;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.*;
import static org.esa.s3tbx.c2rcc.modis.C2rccModisAlgorithm.*;
import static org.esa.s3tbx.c2rcc.util.TargetProductPreparer.*;

// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs (https://github.com/bcdev/s3tbx-c2rcc/issues/2)
// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for MODIS.
 * <p/>
 * Computes AC-reflectances and IOPs from MODIS L1C_LAC data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra, Sabine Embacher
 */
@OperatorMetadata(alias = "modis.c2rcc", version = "0.9.6",
            authors = "Wolfgang Schoenfeld (HZG), Sabine Embacher, Norman Fomferra (Brockmann Consult)",
            category = "Optical Processing/Thematic Water Processing",
            copyright = "Copyright (C) 2015 by Brockmann Consult",
            description = "Performs atmospheric correction and IOP retrieval on MODIS L1C_LAC data products.")
public class C2rccModisOperator extends PixelOperator implements C2rccConfigurable {
    /*
        c2rcc ops have been removed from Graph Builder. In the layer xml they are disabled
        see https://senbox.atlassian.net/browse/SNAP-395
    */

    // Modis bands
    private static final int SOURCE_BAND_COUNT = NN_INPUT_REFLEC_WAVELENGTHS.length;
    private static final int SUN_ZEN_IX = SOURCE_BAND_COUNT;
    private static final int SUN_AZI_IX = SOURCE_BAND_COUNT + 1;
    private static final int VIEW_ZEN_IX = SOURCE_BAND_COUNT + 2;
    private static final int VIEW_AZI_IX = SOURCE_BAND_COUNT + 3;

    // Modis Targets
    private static final int REFLEC_BAND_COUNT = NN_INPUT_REFLEC_WAVELENGTHS.length;

    private static final int IOP_APIG_IX = REFLEC_BAND_COUNT;
    private static final int IOP_ADET_IX = REFLEC_BAND_COUNT + 1;
    private static final int IOP_AGELB_IX = REFLEC_BAND_COUNT + 2;
    private static final int IOP_BPART_IX = REFLEC_BAND_COUNT + 3;
    private static final int IOP_BWIT_IX = REFLEC_BAND_COUNT + 4;

    private static final int RTOSA_RATIO_MIN_IX = REFLEC_BAND_COUNT + 5;
    private static final int RTOSA_RATIO_MAX_IX = REFLEC_BAND_COUNT + 6;
    private static final int C2RCC_FLAGS_IX = REFLEC_BAND_COUNT + 7;

    private static final int RTOSA_IN_1_IX = REFLEC_BAND_COUNT + 8;
    private static final int RTOSA_OUT_1_IX = RTOSA_IN_1_IX + REFLEC_BAND_COUNT;

    private static final String RASTER_NAME_SOLAR_ZENITH = "solz";
    private static final String RASTER_NAME_SOLAR_AZIMUTH = "sola";
    private static final String RASTER_NAME_VIEW_AZIMUTH = "sena";
    private static final String RASTER_NAME_VIEW_ZENITH = "senz";
    static final String[] GEOMETRY_ANGLE_NAMES = {RASTER_NAME_SOLAR_ZENITH, RASTER_NAME_SOLAR_AZIMUTH,
            RASTER_NAME_VIEW_ZENITH, RASTER_NAME_VIEW_AZIMUTH};
    static final String SOURCE_RADIANCE_NAME_PREFIX = "rhot_";
    static final String RASTER_NAME_L2_FLAGS = "l2_flags";

    @SourceProduct(label = "MODIS L1C product",
            description = "MODIS L1C source product.")
    private Product sourceProduct;


    @SourceProduct(description = "The first product providing ozone values for ozone interpolation. " +
            "Use either this in combination with other start- and end-products (tomsomiEndProduct, " +
            "ncepStartProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
            "aux data for calculations.",
            optional = true,
            label = "Ozone interpolation start product (TOMSOMI)")
    private Product tomsomiStartProduct;

    @SourceProduct(description = "The second product providing ozone values for ozone interpolation. " +
            "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
            "ncepStartProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
            "aux data for calculations.",
            optional = true,
            label = "Ozone interpolation end product (TOMSOMI)")
    private Product tomsomiEndProduct;

    @SourceProduct(description = "The first product providing air pressure values for pressure interpolation. " +
            "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
            "tomsomiEndProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
            "aux data for calculations.",
            optional = true,
            label = "Air pressure interpolation start product (NCEP)")
    private Product ncepStartProduct;

    @SourceProduct(description = "The second product providing air pressure values for pressure interpolation. " +
            "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
            "tomsomiEndProduct, ncepStartProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
            "aux data for calculations.",
            optional = true,
            label = "Air pressure interpolation end product (NCEP)")
    private Product ncepEndProduct;

    @Parameter(label = "Valid-pixel expression",
            defaultValue = "!(l2_flags.LAND ||  max(rhot_412,max(rhot_443,max(rhot_488,max(rhot_531,max(rhot_547,max(rhot_555,max(rhot_667,max(rhot_678,max(rhot_748,rhot_869)))))))))>0.25)",
            converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "PSU", interval = "(0.1, 43)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(0.1, 36)")
    private double temperature;

    @Parameter(defaultValue = "330", unit = "DU", interval = "(0, 1000)")
    private double ozone;

    @Parameter(defaultValue = "1000", unit = "hPa", interval = "(800, 1040)", label = "Air Pressure")
    private double press;

    @Parameter(description = "Path to the atmospheric auxiliary data directory. Use either this or tomsomiStartProduct, " +
            "tomsomiEndProduct, ncepStartProduct and ncepEndProduct to use ozone and air pressure aux data " +
            "for calculations. If the auxiliary data needed for interpolation not available in this " +
            "path, the data will automatically downloaded.")
    private String atmosphericAuxDataPath;

    @Parameter(defaultValue = "false", label = "Output top-of-standard-atmosphere (TOSA) reflectances")
    private boolean outputRtosa;

    @Parameter(defaultValue = "false", description =
            "Reflectance values in the target product shall be either written as remote sensing or water leaving reflectances",
            label = "Output AC reflectances as remote sensing or water leaving reflectances")
    private boolean outputAsRrs;

    @Parameter(defaultValue = "false", label = "Output the input angle bands sena, senz, sola and solz")
    private boolean outputAngles;

    private C2rccModisAlgorithm algorithm;
    private AtmosphericAuxdata atmosphericAuxdata;

    public static boolean isValidInput(Product product) {
        for (int wl : NN_INPUT_REFLEC_WAVELENGTHS) {
            if (!product.containsBand(SOURCE_RADIANCE_NAME_PREFIX + wl)) {
                return false;
            }
        }
        if (!product.containsBand(RASTER_NAME_L2_FLAGS)) {
            return false;
        }
        if (!product.containsBand(RASTER_NAME_SOLAR_ZENITH)) {
            return false;
        }
        if (!product.containsBand(RASTER_NAME_SOLAR_AZIMUTH)) {
            return false;
        }
        if (!product.containsBand(RASTER_NAME_VIEW_ZENITH)) {
            return false;
        }
        if (!product.containsBand(RASTER_NAME_VIEW_AZIMUTH)) {
            return false;
        }
        return true;
    }

    @Override
    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    @Override
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public void setOzone(double ozone) {
        this.ozone = ozone;
    }

    @Override
    public void setPress(double press) {
        this.press = press;
    }

    @Override
    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    @Override
    public void setAtmosphericAuxDataPath(String atmosphericAuxDataPath) {
        this.atmosphericAuxDataPath = atmosphericAuxDataPath;
    }

    @Override
    public void setTomsomiStartProduct(Product tomsomiStartProduct) {
        this.tomsomiStartProduct = tomsomiStartProduct;
    }

    @Override
    public void setTomsomiEndProduct(Product tomsomiEndProduct) {
        this.tomsomiEndProduct = tomsomiEndProduct;
    }

    @Override
    public void setNcepStartProduct(Product ncepStartProduct) {
        this.ncepStartProduct = ncepStartProduct;
    }

    @Override
    public void setNcepEndProduct(Product ncepEndProduct) {
        this.ncepEndProduct = ncepEndProduct;
    }

    @Override
    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtosa = outputRtosa;
    }

    @Override
    public void setOutputAsRrs(boolean asRrs) {
        outputAsRrs = asRrs;
    }

    public void setOutputAngles(boolean outputAngles) {
        this.outputAngles = outputAngles;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (atmosphericAuxdata != null) {
            atmosphericAuxdata.dispose();
            atmosphericAuxdata = null;
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double[] toa_ref = new double[SOURCE_BAND_COUNT];
        for (int i = 0; i < SOURCE_BAND_COUNT; i++) {
            toa_ref[i] = sourceSamples[i].getDouble();
        }

        GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
        PixelPos pixelPos = new PixelPos(x + 0.5, y + 0.5);
        double mjd = sourceProduct.getSceneTimeCoding().getMJD(pixelPos);
        GeoPos geoPos = geoCoding.getGeoPos(pixelPos, new GeoPos());

        double ozone = fetchOzone(atmosphericAuxdata, mjd, geoPos.lat, geoPos.lon);
        double atmPress = fetchSurfacePressure(atmosphericAuxdata, mjd, geoPos.lat, geoPos.lon);
        C2rccModisAlgorithm.Result result = algorithm.processPixel(
                toa_ref,
                sourceSamples[SUN_ZEN_IX].getDouble(),
                sourceSamples[SUN_AZI_IX].getDouble(),
                sourceSamples[VIEW_ZEN_IX].getDouble(),
                sourceSamples[VIEW_AZI_IX].getDouble(),
                atmPress,
                ozone
        );

        for (int i = 0; i < result.rw.length; i++) {
            targetSamples[i].set(outputAsRrs ? result.rw[i] * Math.PI : result.rw[i]);
        }

        for (int i = 0; i < result.iops.length; i++) {
            targetSamples[result.rw.length + i].set(result.iops[i]);
        }

        targetSamples[RTOSA_RATIO_MIN_IX].set(result.rtosa_ratio_min);
        targetSamples[RTOSA_RATIO_MAX_IX].set(result.rtosa_ratio_max);
        targetSamples[C2RCC_FLAGS_IX].set(result.flags);

        if (outputAngles) {
            final int targetStartIdx = C2RCC_FLAGS_IX + 1;
            for (int i = 0; i < GEOMETRY_ANGLE_NAMES.length; i++) {
                targetSamples[targetStartIdx + i].set(sourceSamples[SUN_ZEN_IX + i].getFloat());
            }
        }

        if (outputRtosa) {
            final int offset = outputAngles ? GEOMETRY_ANGLE_NAMES.length : 0;
            for (int i = 0; i < result.rtosa_in.length; i++) {
                targetSamples[RTOSA_IN_1_IX + offset + i].set(result.rtosa_in[i]);
            }
            for (int i = 0; i < result.rtosa_out.length; i++) {
                targetSamples[RTOSA_OUT_1_IX + offset + i].set(result.rtosa_out[i]);
            }
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        sc.setValidPixelMask(validPixelExpression);
        for (int i = 0; i < NN_INPUT_REFLEC_WAVELENGTHS.length; i++) {
            int wl = NN_INPUT_REFLEC_WAVELENGTHS[i];
            sc.defineSample(i, "rhot_" + wl);
        }
        sc.defineSample(SUN_ZEN_IX, RASTER_NAME_SOLAR_ZENITH);
        sc.defineSample(SUN_AZI_IX, RASTER_NAME_SOLAR_AZIMUTH);
        sc.defineSample(VIEW_ZEN_IX, RASTER_NAME_VIEW_ZENITH);
        sc.defineSample(VIEW_AZI_IX, RASTER_NAME_VIEW_AZIMUTH);
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sc) throws OperatorException {
        for (int i = 0; i < NN_INPUT_REFLEC_WAVELENGTHS.length; i++) {
            if (outputAsRrs) {
                sc.defineSample(i, "rrs_" + NN_INPUT_REFLEC_WAVELENGTHS[i]);
            }else {
                sc.defineSample(i, "rhow_" + NN_INPUT_REFLEC_WAVELENGTHS[i]);
            }
        }

        sc.defineSample(IOP_APIG_IX, "iop_apig");
        sc.defineSample(IOP_ADET_IX, "iop_adet");
        sc.defineSample(IOP_AGELB_IX, "iop_agelb");
        sc.defineSample(IOP_BPART_IX, "iop_bpart");
        sc.defineSample(IOP_BWIT_IX, "iop_bwit");

        sc.defineSample(RTOSA_RATIO_MIN_IX, "rtosa_ratio_min");
        sc.defineSample(RTOSA_RATIO_MAX_IX, "rtosa_ratio_max");
        sc.defineSample(C2RCC_FLAGS_IX, "c2rcc_flags");

        if (outputAngles) {
            final int startIndex = C2RCC_FLAGS_IX + 1;
            for (int i = 0; i < GEOMETRY_ANGLE_NAMES.length; i++) {
                String angleName = GEOMETRY_ANGLE_NAMES[i];
                sc.defineSample(startIndex + i, angleName);
            }
        }

        if (outputRtosa) {
            final int angleOffset = outputAngles ? GEOMETRY_ANGLE_NAMES.length : 0;
            for (int i = 0; i < NN_INPUT_REFLEC_WAVELENGTHS.length; i++) {
                int wl = NN_INPUT_REFLEC_WAVELENGTHS[i];
                sc.defineSample(RTOSA_IN_1_IX + angleOffset + i, "rtosa_in_" + wl);
            }
            for (int i = 0; i < NN_INPUT_REFLEC_WAVELENGTHS.length; i++) {
                int wl = NN_INPUT_REFLEC_WAVELENGTHS[i];
                sc.defineSample(RTOSA_OUT_1_IX + angleOffset + i, "rtosa_out_" + wl);
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();
        Product targetProduct = productConfigurer.getTargetProduct();
        prepareTargetProduct(targetProduct, sourceProduct, SOURCE_RADIANCE_NAME_PREFIX, NN_INPUT_REFLEC_WAVELENGTHS,
                             outputRtosa, outputAsRrs);

        if (outputAngles) {
            for (String angleName : GEOMETRY_ANGLE_NAMES) {
                final Band band = sourceProduct.getBand(angleName);
                addBand(targetProduct, angleName, band.getUnit(), band.getDescription());
            }
        }
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        for (int wl : NN_INPUT_REFLEC_WAVELENGTHS) {
            assertSourceBand(SOURCE_RADIANCE_NAME_PREFIX + wl);
        }
        assertSourceBand(RASTER_NAME_L2_FLAGS);

        if (sourceProduct.getSceneGeoCoding() == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }

        try {
            algorithm = new C2rccModisAlgorithm();
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);

        ensureTimeCoding_Fallback(sourceProduct);
        initAtmosphericAuxdata();
    }

    private void initAtmosphericAuxdata() {
        AtmosphericAuxdataBuilder auxdataBuilder = new AtmosphericAuxdataBuilder();
        auxdataBuilder.setOzone(ozone);
        auxdataBuilder.setSurfacePressure(press);
        auxdataBuilder.useAtmosphericAuxDataPath(atmosphericAuxDataPath);
        auxdataBuilder.useTomsomiProducts(tomsomiStartProduct, tomsomiEndProduct);
        auxdataBuilder.useNcepProducts(ncepStartProduct, ncepEndProduct);
        try {
            atmosphericAuxdata = auxdataBuilder.create();
        } catch (Exception e) {
            throw new OperatorException("Could not create provider for atmospheric auxdata", e);
        }
    }

    private void assertSourceBand(String name) {
        if (sourceProduct.getBand(name) == null) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccModisOperator.class);
        }
    }
}
