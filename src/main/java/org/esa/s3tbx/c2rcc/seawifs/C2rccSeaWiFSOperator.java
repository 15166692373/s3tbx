package org.esa.s3tbx.c2rcc.seawifs;

import org.esa.s3tbx.c2rcc.C2rccConfigurable;
import org.esa.s3tbx.c2rcc.ancillary.AncDataFormat;
import org.esa.s3tbx.c2rcc.ancillary.AncDownloader;
import org.esa.s3tbx.c2rcc.ancillary.AncRepository;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdata;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataDynamic;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataStatic;
import org.esa.s3tbx.c2rcc.util.TargetProductPreparer;
import org.esa.snap.core.datamodel.Band;
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
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import java.io.File;
import java.io.IOException;

import static org.esa.s3tbx.c2rcc.C2rccCommons.ensureTimeCoding_Fallback;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.ANC_DATA_URI;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.createOzoneFormat;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.createPressureFormat;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.fetchOzone;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.fetchSurfacePressure;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.ozone_default;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.pressure_default;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.salinity_default;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.seawifsWavelengths;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.temperature_default;

// todo (nf) - Add Thullier solar fluxes as default values to C2R-CC operator (https://github.com/bcdev/s3tbx-c2rcc/issues/1)
// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs (https://github.com/bcdev/s3tbx-c2rcc/issues/2)
// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)

/**
 * The Case 2 Regional / CoastColour Operator for SeaWiFS.
 * <p/>
 * Computes AC-reflectances and IOPs from SeaWiFS L1b data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra
 */
@OperatorMetadata(alias = "seawifs.c2rcc", version = "0.9.5",
            authors = "Roland Doerffer, Sabine Embacher, Norman Fomferra (Brockmann Consult)",
            category = "Optical Processing/Thematic Water Processing",
            copyright = "Copyright (C) 2015 by Brockmann Consult",
            description = "Performs atmospheric correction and IOP retrieval on SeaWifs L1b data products.")
public class C2rccSeaWiFSOperator extends PixelOperator implements C2rccConfigurable {
    /*
        c2rcc ops have been removed from Graph Builder. In the layer xml they are disabled
        see https://senbox.atlassian.net/browse/SNAP-395
    */

    public static final int WL_BAND_COUNT = seawifsWavelengths.length;

    // sources
//    public static final int DEM_ALT_IX = WL_BAND_COUNT + 0;
    public static final int SUN_ZEN_IX = WL_BAND_COUNT;
    public static final int SUN_AZI_IX = WL_BAND_COUNT + 1;
    public static final int VIEW_ZEN_IX = WL_BAND_COUNT + 2;
    public static final int VIEW_AZI_IX = WL_BAND_COUNT + 3;
//    public static final int ATM_PRESS_IX = WL_BAND_COUNT + 5;
//    public static final int OZONE_IX = WL_BAND_COUNT + 6;

    // targets
    public static final int REFLEC_1_IX = 0;
    public static final int IOP_APIG_IX = WL_BAND_COUNT;
    public static final int IOP_ADET_IX = WL_BAND_COUNT + 1;
    public static final int IOP_AGELB_IX = WL_BAND_COUNT + 2;
    public static final int IOP_BPART_IX = WL_BAND_COUNT + 3;
    public static final int IOP_BWIT_IX = WL_BAND_COUNT + 4;

    public static final int RTOSA_RATIO_MIN_IX = WL_BAND_COUNT + 5;
    public static final int RTOSA_RATIO_MAX_IX = WL_BAND_COUNT + 6;
    public static final int L2_QFLAGS_IX = WL_BAND_COUNT + 7;

    public static final int RTOSA_IN_1_IX = WL_BAND_COUNT + 8;
    public static final int RTOSA_OUT_1_IX = RTOSA_IN_1_IX + WL_BAND_COUNT;

    @SourceProduct(label = "SeaWiFS L1b product",
                description = "SeaWiFS L1b source product.")
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
                defaultValue = "!(l2_flags.LAND || rhot_865 > 0.25)",
                converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "" + salinity_default, unit = "DU", interval = "(0, 100)")
    private double salinity;

    @Parameter(defaultValue = "" + temperature_default, unit = "C", interval = "(-50, 50)")
    private double temperature;

    @Parameter(defaultValue = "" + ozone_default, unit = "DU", interval = "(0, 1000)")
    private double ozone;

    @Parameter(defaultValue = "" + pressure_default, unit = "hPa", interval = "(0, 2000)", label = "Air Pressure")
    private double press;

    @Parameter(description = "Path to the atmospheric auxiliary data directory. Use either this or tomsomiStartProduct, " +
                             "tomsomiEndProduct, ncepStartProduct and ncepEndProduct to use ozone and air pressure aux data " +
                             "for calculations. If the auxiliary data needed for interpolation not available in this " +
                             "path, the data will automatically downloaded.")
    private String atmosphericAuxDataPath;

    @Parameter(defaultValue = "false", label = "Output top-of-standard-atmosphere (TOSA) reflectances")
    private boolean outputRtosa;

    private C2rccSeaWiFSAlgorithm algorithm;
    private AtmosphericAuxdata atmosphericAuxdata;

    public void setAtmosphericAuxDataPath(String atmosphericAuxDataPath) {
        this.atmosphericAuxDataPath = atmosphericAuxDataPath;
    }

    public void setTomsomiStartProduct(Product tomsomiStartProduct) {
        this.tomsomiStartProduct = tomsomiStartProduct;
    }

    public void setTomsomiEndProduct(Product tomsomiEndProduct) {
        this.tomsomiEndProduct = tomsomiEndProduct;
    }

    public void setNcepStartProduct(Product ncepStartProduct) {
        this.ncepStartProduct = ncepStartProduct;
    }

    public void setNcepEndProduct(Product ncepEndProduct) {
        this.ncepEndProduct = ncepEndProduct;
    }

    public void setOzone(double ozone) {
        this.ozone = ozone;
    }

    public void setPress(double press) {
        this.press = press;
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double[] toa_ref = new double[WL_BAND_COUNT];
        for (int i = 0; i < WL_BAND_COUNT; i++) {
            toa_ref[i] = sourceSamples[i].getDouble();
        }

        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        GeoPos geoPos = sourceProduct.getSceneGeoCoding().getGeoPos(pixelPos, null);
        final double mjd = sourceProduct.getSceneTimeCoding().getMJD(pixelPos);
        final double lat = geoPos.getLat();
        final double lon = geoPos.getLon();

        final double sun_zeni = sourceSamples[SUN_ZEN_IX].getDouble();
        final double sun_azi = sourceSamples[SUN_AZI_IX].getDouble();
        final double view_zeni = sourceSamples[VIEW_ZEN_IX].getDouble();
        final double view_azi = sourceSamples[VIEW_AZI_IX].getDouble();
        final double dem_alt = 0.0;  // todo to be replaced by a real value
        final double atm_press = fetchSurfacePressure(atmosphericAuxdata, mjd, lat, lon);
        final double ozone = fetchOzone(atmosphericAuxdata, mjd, lat, lon);

        C2rccSeaWiFSAlgorithm.Result result = algorithm.processPixel(
                    toa_ref,
                    sun_zeni, sun_azi,
                    view_zeni, view_azi,
                    dem_alt,
                    atm_press, ozone
        );

        for (int i = 0; i < result.rw.length; i++) {
            targetSamples[i].set(result.rw[i]);
        }

        for (int i = 0; i < result.iops.length; i++) {
            targetSamples[result.rw.length + i].set(result.iops[i]);
        }

        targetSamples[RTOSA_RATIO_MIN_IX].set(result.rtosa_ratio_min);
        targetSamples[RTOSA_RATIO_MAX_IX].set(result.rtosa_ratio_max);
        targetSamples[L2_QFLAGS_IX].set(result.flags);

        if (outputRtosa) {
            for (int i = 0; i < result.rtosa_in.length; i++) {
                targetSamples[RTOSA_IN_1_IX + i].set(result.rtosa_in[i]);
            }
            for (int i = 0; i < result.rtosa_out.length; i++) {
                targetSamples[RTOSA_OUT_1_IX + i].set(result.rtosa_out[i]);
            }
        }
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        sc.setValidPixelMask(validPixelExpression);
        for (int i = 0; i < WL_BAND_COUNT; i++) {
            final int wavelength = seawifsWavelengths[i];
            sc.defineSample(i, "rhot_" + wavelength);
        }
//        sc.defineSample(DEM_ALT_IX, "dem_alt"); // todo
        sc.defineSample(SUN_ZEN_IX, "solz");
        sc.defineSample(SUN_AZI_IX, "sola");
        sc.defineSample(VIEW_ZEN_IX, "senz");
        sc.defineSample(VIEW_AZI_IX, "sena");
//        sc.defineSample(ATM_PRESS_IX, "atm_press"); // todo
//        sc.defineSample(OZONE_IX, "ozone");         // todo
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sc) throws OperatorException {

        for (int i = 0; i < seawifsWavelengths.length; i++) {
            int wl = seawifsWavelengths[i];
            sc.defineSample(REFLEC_1_IX + i, "reflec_" + wl);
        }

        sc.defineSample(IOP_APIG_IX, "iop_apig");
        sc.defineSample(IOP_ADET_IX, "iop_adet");
        sc.defineSample(IOP_AGELB_IX, "iop_agelb");
        sc.defineSample(IOP_BPART_IX, "iop_bpart");
        sc.defineSample(IOP_BWIT_IX, "iop_bwit");
        sc.defineSample(RTOSA_RATIO_MIN_IX, "rtosa_ratio_min");
        sc.defineSample(RTOSA_RATIO_MAX_IX, "rtosa_ratio_max");
        sc.defineSample(L2_QFLAGS_IX, "l2_qflags");

        if (outputRtosa) {
            for (int i = 0; i < seawifsWavelengths.length; i++) {
                int wl = seawifsWavelengths[i];
                sc.defineSample(RTOSA_IN_1_IX + i, "rtosa_in_" + wl);
            }
            for (int i = 0; i < seawifsWavelengths.length; i++) {
                int wl = seawifsWavelengths[i];
                sc.defineSample(RTOSA_OUT_1_IX + i, "rtosa_out_" + wl);
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();
        Product targetProduct = productConfigurer.getTargetProduct();
        TargetProductPreparer.prepareTargetProduct(targetProduct, sourceProduct, "rhot_", seawifsWavelengths, outputRtosa);
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        for (int i = 0; i < WL_BAND_COUNT; i++) {
            final int wavelength = seawifsWavelengths[i];
            assertSourceBand("rhot_" + wavelength);
        }
        assertSourceBand("l2_flags");
        assertSourceBandAndRemoveValidExpression("solz");
        assertSourceBandAndRemoveValidExpression("sola");
        assertSourceBandAndRemoveValidExpression("senz");
        assertSourceBandAndRemoveValidExpression("sena");

        if (sourceProduct.getSceneGeoCoding() == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }

        try {
            algorithm = new C2rccSeaWiFSAlgorithm();
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);

        ensureTimeCoding_Fallback(sourceProduct);
        initAtmosphericAuxdata();

    }

    public static boolean isValidInput(Product product) {
        for (int i = 0; i < WL_BAND_COUNT; i++) {
            final int wl = seawifsWavelengths[i];
            if (!product.containsBand("rhot_" + wl)) {
                return false;
            }
        }
        if (!product.containsBand("l2_flags")) {
            return false;
        }
        if (!product.containsBand("solz")) {
            return false;
        }
        if (!product.containsBand("sola")) {
            return false;
        }
        if (!product.containsBand("senz")) {
            return false;
        }
        if (!product.containsBand("sena")) {
            return false;
        }
        return true;
    }

    private void initAtmosphericAuxdata() {
        if (StringUtils.isNullOrEmpty(atmosphericAuxDataPath)) {
            try {
                atmosphericAuxdata = new AtmosphericAuxdataStatic(tomsomiStartProduct, tomsomiEndProduct, "ozone", ozone,
                                                                  ncepStartProduct, ncepEndProduct, "press", press);
            } catch (IOException e) {
                final String message = "Unable to create provider for atmospheric ancillary data.";
                getLogger().severe(message);
                getLogger().severe(e.getMessage());
                throw new OperatorException(message, e);
            }
        } else {
            final AncDownloader ancDownloader = new AncDownloader(ANC_DATA_URI);
            final AncRepository ancRepository = new AncRepository(new File(atmosphericAuxDataPath), ancDownloader);
            AncDataFormat ozoneFormat = createOzoneFormat(ozone_default);
            AncDataFormat pressureFormat = createPressureFormat(pressure_default);
            atmosphericAuxdata = new AtmosphericAuxdataDynamic(ancRepository, ozoneFormat, pressureFormat);
        }
    }

    private void assertSourceBandAndRemoveValidExpression(String bandname) {
        assertSourceBand(bandname);
        final Band band = sourceProduct.getBand(bandname);
        band.setValidPixelExpression("");
    }

    private void assertSourceBand(String name) {
        if (sourceProduct.getBand(name) == null) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtosa = outputRtosa;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (atmosphericAuxdata != null) {
            atmosphericAuxdata.dispose();
            atmosphericAuxdata = null;
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccSeaWiFSOperator.class);
        }
    }
}
