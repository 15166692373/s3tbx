package org.esa.s3tbx.c2rcc.meris;

import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.ANC_DATA_URI;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.createOzoneFormat;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.createPressureFormat;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.fetchOzone;
import static org.esa.s3tbx.c2rcc.ancillary.AncillaryCommons.fetchSurfacePressure;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.DEFAULT_SOLAR_FLUX;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.merband12_ix;
import static org.esa.s3tbx.c2rcc.meris.C2rccMerisAlgorithm.merband15_ix;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.ozone_default;
import static org.esa.s3tbx.c2rcc.seawifs.C2rccSeaWiFSAlgorithm.pressure_default;

import org.esa.s3tbx.c2rcc.C2rccConfigurable;
import org.esa.s3tbx.c2rcc.ancillary.AncDataFormat;
import org.esa.s3tbx.c2rcc.ancillary.AncDownloader;
import org.esa.s3tbx.c2rcc.ancillary.AncRepository;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdata;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataDynamic;
import org.esa.s3tbx.c2rcc.ancillary.AtmosphericAuxdataStatic;
import org.esa.s3tbx.c2rcc.util.SolarFluxLazyLookup;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
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
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.converters.BooleanExpressionConverter;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

// todo (nf) - Add Thullier solar fluxes as default values to C2R-CC operator (https://github.com/bcdev/s3tbx-c2rcc/issues/1)
// todo (nf) - Add flags band and check for OOR of inputs and outputs of the NNs (https://github.com/bcdev/s3tbx-c2rcc/issues/2)
// todo (nf) - Add min/max values of NN inputs and outputs to metadata (https://github.com/bcdev/s3tbx-c2rcc/issues/3)
// todo (RD) - salinity and temperautre have to be passed to C2R ?
// todo (RD) - parameters, to control which variables to be processed, pass to C2R

/**
 * The Case 2 Regional / CoastColour Operator for MERIS.
 * <p/>
 * Computes AC-reflectances and IOPs from MERIS L1b data products using
 * an neural-network approach.
 *
 * @author Norman Fomferra
 */
@OperatorMetadata(alias = "meris.c2rcc", version = "0.6",
            authors = "Roland Doerffer, Norman Fomferra (Brockmann Consult)",
            category = "Optical Processing/Thematic Water Processing",
            copyright = "Copyright (C) 2015 by Brockmann Consult",
            description = "Performs atmospheric correction and IOP retrieval with uncertainties on MERIS L1b data products.")
public class C2rccMerisOperator extends PixelOperator implements C2rccConfigurable {

    // MERIS sources
    public static final int BAND_COUNT = 15;
    public static final int DEM_ALT_IX = BAND_COUNT;
    public static final int SUN_ZEN_IX = BAND_COUNT + 1;
    public static final int SUN_AZI_IX = BAND_COUNT + 2;
    public static final int VIEW_ZEN_IX = BAND_COUNT + 3;
    public static final int VIEW_AZI_IX = BAND_COUNT + 4;
    public static final int ATM_PRESS_IX = BAND_COUNT + 5;
    public static final int OZONE_IX = BAND_COUNT + 6;

    // MERIS targets

    public static final int BC_12 = merband12_ix.length; // Band count 12
    public static final int BC_15 = merband15_ix.length; // Band count 15
    public static final int SINGLE_IX = BC_15 + 7 * BC_12;

    public static final int RTOA_IX = 0;
    public static final int RTOSA_IX = BC_15;
    public static final int RTOSA_AANN_IX = BC_15 + BC_12;
    public static final int RPATH_IX = BC_15 + 2 * BC_12;
    public static final int TDOWN_IX = BC_15 + 3 * BC_12;
    public static final int TUP_IX = BC_15 + 4 * BC_12;
    public static final int RWA_IX = BC_15 + 5 * BC_12;
    public static final int RWN_IX = BC_15 + 6 * BC_12;

    public static final int OOS_RTOSA_IX = SINGLE_IX;
    public static final int OOS_RWA_IX = SINGLE_IX + 1;

    public static final int IOP_APIG_IX = SINGLE_IX + 2;
    public static final int IOP_ADET_IX = SINGLE_IX + 3;
    public static final int IOP_AGELB_IX = SINGLE_IX + 4;
    public static final int IOP_BPART_IX = SINGLE_IX + 5;
    public static final int IOP_BWIT_IX = SINGLE_IX + 6;

//    public static final int IOP_ADG_IX = SINGLE_IX + 7;  // virtual band
//    public static final int IOP_ATOT_IX = SINGLE_IX + 8;  // virtual band
//    public static final int IOP_BTOT_IX = SINGLE_IX + 9;  // virtual band
    public static final int KD489_IX = SINGLE_IX + 7;
    public static final int KDMIN_IX = SINGLE_IX + 8;
//    public static final int KD_Z90MAX_IX = SINGLE_IX + 12;  // virtual band
//    public static final int CONC_CHL_IX = SINGLE_IX + 13;  // virtual band
//    public static final int CONC_TSM_IX = SINGLE_IX + 14;  // virtual band

    public static final int UNC_APIG_IX = SINGLE_IX + 9;
    public static final int UNC_ADET_IX = SINGLE_IX + 10;
    public static final int UNC_AGELB_IX = SINGLE_IX + 11;
    public static final int UNC_BPART_IX = SINGLE_IX + 12;
    public static final int UNC_BWIT_IX = SINGLE_IX + 13;
    public static final int UNC_ADG_IX = SINGLE_IX + 14;
    public static final int UNC_ATOT_IX = SINGLE_IX + 15;
    public static final int UNC_BTOT_IX = SINGLE_IX + 16;
//    public static final int UNC_TSM_IX = SINGLE_IX + 24;  // virtual band
//    public static final int UNC_CHL_IX = SINGLE_IX + 23;  // virtual band
    public static final int UNC_KD489_IX = SINGLE_IX + 17;
    public static final int UNC_KDMIN_IX = SINGLE_IX + 18;
//    public static final int UNC_Z90MAX_IX = SINGLE_IX + 27;  // virtual band

    public static final int L2_FLAGS_IX = SINGLE_IX + 19;

    @SourceProduct(label = "MERIS L1b product",
                description = "MERIS L1b source product.")
    private Product sourceProduct;

    @SourceProduct(description = "The first product providing ozone values for ozone interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiEndProduct, " +
                                 "ncepStartProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "auxiliary data for calculations.",
                optional = true,
                label = "Ozone interpolation start product (TOMSOMI)")
    private Product tomsomiStartProduct;

    @SourceProduct(description = "The second product providing ozone values for ozone interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
                                 "ncepStartProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "auxiliary data for calculations.",
                optional = true,
                label = "Ozone interpolation end product (TOMSOMI)")
    private Product tomsomiEndProduct;

    @SourceProduct(description = "The first product providing air pressure values for pressure interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
                                 "tomsomiEndProduct, ncepEndProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "auxiliary data for calculations.",
                optional = true,
                label = "Air pressure interpolation start product (NCEP)")
    private Product ncepStartProduct;

    @SourceProduct(description = "The second product providing air pressure values for pressure interpolation. " +
                                 "Use either this in combination with other start- and end-products (tomsomiStartProduct, " +
                                 "tomsomiEndProduct, ncepStartProduct) or atmosphericAuxdataPath to use ozone and air pressure " +
                                 "auxiliary data for calculations.",
                optional = true,
                label = "Air pressure interpolation end product (NCEP)")
    private Product ncepEndProduct;

    @Parameter(label = "Valid-pixel expression",
                defaultValue = "!l1_flags.INVALID && !l1_flags.LAND_OCEAN",
                converter = BooleanExpressionConverter.class)
    private String validPixelExpression;

    @Parameter(defaultValue = "35.0", unit = "DU", interval = "(0, 100)")
    private double salinity;

    @Parameter(defaultValue = "15.0", unit = "C", interval = "(-50, 50)")
    private double temperature;

    @Parameter(defaultValue = "330", unit = "DU", interval = "(0, 1000)")
    private double ozone;

    @Parameter(defaultValue = "1000", unit = "hPa", interval = "(0, 2000)", label = "Air Pressure")
    private double press;

    @Parameter(defaultValue = "1.7", description = "Conversion factor btot -> TSM")
    private double TSMfak;

    @Parameter(defaultValue = "1.04", description = "Chlorophyl exponent ( CHL = iop-apig^CHLexp * CHLfak ) ")
    private double CHLexp;

    @Parameter(defaultValue = "20.0", description = "Chlorophyl factor ( CHL = iop-apig^CHLexp * CHLfak ) ")
    private double CHLfak;

    @Parameter(description = "Path to the atmospheric auxiliary data directory. Use either this or tomsomiStartProduct, " +
                             "tomsomiEndProduct, ncepStartProduct and ncepEndProduct to use ozone and air pressure aux data " +
                             "for calculations. If the auxiliary data needed for interpolation not available in this " +
                             "path, the data will automatically downloaded.")
    private String atmosphericAuxDataPath;

    @Parameter(defaultValue = "false")
    private boolean useDefaultSolarFlux;

    @Parameter(defaultValue = "false", description =
                "If selected, the ECMWF auxiliary data (ozon, air pressure) of the source product is used",
                                label = "Use ECMWF aux data of source product")
    private boolean useEcmwfAuxData;

    @Parameter(defaultValue = "false", label = "Output top-of-atmosphere (TOA) reflectances")
    private boolean outputRtoa;

    @Parameter(defaultValue = "false", label = "Output gas corrected top-of-atmosphere (TOSA) reflectances")
    private boolean outputRtoaGc;

    @Parameter(defaultValue = "false", label = "Output of auto nn, reflectances")
    private boolean outputRtoaGc_aann;

    @Parameter(defaultValue = "false", label = "Output path radiance reflectances")
    private boolean outputRpath;

    @Parameter(defaultValue = "false", label = "Output downward transmittance")
    private boolean outputTdown;

    @Parameter(defaultValue = "false", label = "Output upward transmittance")
    private boolean outputTup;

    @Parameter(defaultValue = "true", label = "Output angular dependent water leaving reflectances")
    private boolean outputRwa;

    @Parameter(defaultValue = "false", label = "Output normalized water leaving reflectances")
    private boolean outputRwn;

    @Parameter(defaultValue = "false", label = "Output of out of scope values")
    private boolean outputOos;

    @Parameter(defaultValue = "true", label = "Output of irradiance attenuation coefficients")
    private boolean outputKd;

    @Parameter(defaultValue = "false", label = "Output uncertainties")
    private boolean outputUncertainties;

    private C2rccMerisAlgorithm algorithm;
    private SolarFluxLazyLookup solarFluxLazyLookup;
    private AtmosphericAuxdata atmosphericAuxdata;

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
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public void setSalinity(double salinity) {
        this.salinity = salinity;
    }

    @Override
    public void setOzone(double ozone) {
        this.ozone = ozone;
    }

    @Override
    public void setPress(double press) {
        this.press = press;
    }

    public void setUseDefaultSolarFlux(boolean useDefaultSolarFlux) {
        this.useDefaultSolarFlux = useDefaultSolarFlux;
    }

    public void setUseEcmwfAuxData(boolean useEcmwfAuxData) {
        this.useEcmwfAuxData = useEcmwfAuxData;
    }

    @Override
    public void setValidPixelExpression(String validPixelExpression) {
        this.validPixelExpression = validPixelExpression;
    }

    @Override
    public void setOutputRtosa(boolean outputRtosa) {
        this.outputRtoa = outputRtosa;
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double[] radiances = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            radiances[i] = sourceSamples[i].getDouble();
        }

        final PixelPos pixelPos = new PixelPos(x + 0.5f, y + 0.5f);
        final double mjd = sourceProduct.getSceneTimeCoding().getMJD(pixelPos);
        if (useDefaultSolarFlux) {
            ProductData.UTC utc = new ProductData.UTC(mjd);
            Calendar calendar = utc.getAsCalendar();
            final int doy = calendar.get(Calendar.DAY_OF_YEAR);
            final int year = calendar.get(Calendar.YEAR);
            double[] correctedSolFlux = solarFluxLazyLookup.getCorrectedFluxFor(doy, year);
            algorithm.setSolflux(correctedSolFlux);
        }

        GeoPos geoPos = sourceProduct.getSceneGeoCoding().getGeoPos(pixelPos, null);
        double lat = geoPos.getLat();
        double lon = geoPos.getLon();
        double atmPress;
        double ozone;
        if (useEcmwfAuxData) {
            atmPress = sourceSamples[ATM_PRESS_IX].getDouble();
            ozone = sourceSamples[OZONE_IX].getDouble();
        } else {
            ozone = fetchOzone(atmosphericAuxdata, mjd, lat, lon);
            atmPress = fetchSurfacePressure(atmosphericAuxdata, mjd, lat, lon);
        }
        C2rccMerisAlgorithm.Result result = algorithm.processPixel(x, y, lat, lon,
                                                                   radiances,
                                                                   sourceSamples[SUN_ZEN_IX].getDouble(),
                                                                   sourceSamples[SUN_AZI_IX].getDouble(),
                                                                   sourceSamples[VIEW_ZEN_IX].getDouble(),
                                                                   sourceSamples[VIEW_AZI_IX].getDouble(),
                                                                   sourceSamples[DEM_ALT_IX].getDouble(),
                                                                   atmPress,
                                                                   ozone);

        if (outputRtoa) {
            for (int i = 0; i < result.r_toa.length; i++) {
                targetSamples[RTOA_IX + i].set(result.r_toa[i]);
            }
        }

        if (outputRtoaGc) {
            for (int i = 0; i < result.r_tosa.length; i++) {
                targetSamples[RTOSA_IX + i].set(result.r_tosa[i]);
            }
        }

        if (outputRtoaGc_aann) {
            for (int i = 0; i < result.rtosa_aann.length; i++) {
                targetSamples[RTOSA_AANN_IX + i].set(result.rtosa_aann[i]);
            }
        }

        if (outputRpath) {
            for (int i = 0; i < result.rpath_nn.length; i++) {
                targetSamples[RPATH_IX + i].set(result.rpath_nn[i]);
            }
        }

        if (outputTdown) {
            for (int i = 0; i < result.transd_nn.length; i++) {
                targetSamples[TDOWN_IX + i].set(result.transd_nn[i]);
            }
        }

        if (outputTup) {
            for (int i = 0; i < result.transu_nn.length; i++) {
                targetSamples[TUP_IX + i].set(result.transu_nn[i]);
            }
        }

        if (outputRwa) {
            for (int i = 0; i < result.rwa.length; i++) {
                targetSamples[RWA_IX + i].set(result.rwa[i]);
            }
        }

        if (outputRwn) {
            for (int i = 0; i < result.rwn.length; i++) {
                targetSamples[RWN_IX + i].set(result.rwn[i]);
            }
        }

        if (outputOos) {
            targetSamples[OOS_RTOSA_IX].set(result.rtosa_oos);
            targetSamples[OOS_RWA_IX].set(result.rwa_oos);
        }

        for (int i = 0; i < result.iops_nn.length; i++) {
            targetSamples[IOP_APIG_IX + i].set(result.iops_nn[i]);
        }

        if (outputKd) {
            targetSamples[KD489_IX].set(result.kd489_nn);
            targetSamples[KDMIN_IX].set(result.kdmin_nn);
        }

        if (outputUncertainties) {
            for (int i = 0; i < result.unc_iop_abs.length; i++) {
                targetSamples[UNC_APIG_IX + i].set(result.unc_iop_abs[i]);
            }
            targetSamples[UNC_ADG_IX].set(result.unc_abs_adg);
            targetSamples[UNC_ATOT_IX].set(result.unc_abs_atot);
            targetSamples[UNC_BTOT_IX].set(result.unc_abs_btot);
            if (outputKd) {
                targetSamples[UNC_KD489_IX].set(result.unc_abs_kd489);
                targetSamples[UNC_KDMIN_IX].set(result.unc_abs_kdmin);
            }
        }

        targetSamples[L2_FLAGS_IX].set(result.flags);
    }

    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sc) throws OperatorException {
        sc.setValidPixelMask(validPixelExpression);
        for (int i = 0; i < BAND_COUNT; i++) {
            sc.defineSample(i, "radiance_" + (i + 1));
        }
        sc.defineSample(DEM_ALT_IX, "dem_alt");
        sc.defineSample(SUN_ZEN_IX, "sun_zenith");
        sc.defineSample(SUN_AZI_IX, "sun_azimuth");
        sc.defineSample(VIEW_ZEN_IX, "view_zenith");
        sc.defineSample(VIEW_AZI_IX, "view_azimuth");
        sc.defineSample(ATM_PRESS_IX, "atm_press");
        sc.defineSample(OZONE_IX, "ozone");
    }

    @Override
    protected void configureTargetSamples(TargetSampleConfigurer tsc) throws OperatorException {

        if (outputRtoa) {
            for (int i = 0; i < merband15_ix.length; i++) {
                tsc.defineSample(RTOA_IX + i, "rtoa_" + merband15_ix[i]);
            }
        }

        if (outputRtoaGc) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RTOSA_IX + i, "rtosa_gc_" + merband12_ix[i]);
            }
        }

        if (outputRtoaGc_aann) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RTOSA_AANN_IX + i, "rtosagc_aann_" + merband12_ix[i]);
            }
        }

        if (outputRpath) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RPATH_IX + i, "rpath_" + merband12_ix[i]);
            }
        }

        if (outputTdown) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(TDOWN_IX + i, "tdown_" + merband12_ix[i]);
            }
        }

        if (outputTup) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(TUP_IX + i, "tup_" + merband12_ix[i]);
            }
        }

        if (outputRwa) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RWA_IX + i, "rwa_" + merband12_ix[i]);
            }
        }

        if (outputRwn) {
            for (int i = 0; i < merband12_ix.length; i++) {
                tsc.defineSample(RWN_IX + i, "rwn_" + merband12_ix[i]);
            }
        }

        if (outputOos) {
            tsc.defineSample(OOS_RTOSA_IX, "oos_rtosa");
            tsc.defineSample(OOS_RWA_IX, "oos_rwa");
        }

        tsc.defineSample(IOP_APIG_IX, "iop_apig");
        tsc.defineSample(IOP_ADET_IX, "iop_adet");
        tsc.defineSample(IOP_AGELB_IX, "iop_agelb");
        tsc.defineSample(IOP_BPART_IX, "iop_bpart");
        tsc.defineSample(IOP_BWIT_IX, "iop_bwit");

        if (outputKd) {
            tsc.defineSample(KD489_IX, "kd489");
            tsc.defineSample(KDMIN_IX, "kdmin");
        }

        if (outputUncertainties) {
            tsc.defineSample(UNC_APIG_IX, "unc_apig");
            tsc.defineSample(UNC_ADET_IX, "unc_adet");
            tsc.defineSample(UNC_AGELB_IX, "unc_agelb");
            tsc.defineSample(UNC_BPART_IX, "unc_bpart");
            tsc.defineSample(UNC_BWIT_IX, "unc_bwit");

            tsc.defineSample(UNC_ADG_IX, "unc_adg");
            tsc.defineSample(UNC_ATOT_IX, "unc_atot");
            tsc.defineSample(UNC_BTOT_IX, "unc_btot");
            if (outputKd) {
                tsc.defineSample(UNC_KD489_IX, "unc_kd489");
                tsc.defineSample(UNC_KDMIN_IX, "unc_kdmin");
            }
        }

        tsc.defineSample(L2_FLAGS_IX, "l2_flags");
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        productConfigurer.copyMetadata();

        Product targetProduct = productConfigurer.getTargetProduct();

        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        String autoGrouping = "iop";
        autoGrouping += ":" + "conc";

        if (outputRtoa) {
            for (int i : merband15_ix) {
                final Band band = addBand(targetProduct, "rtoa_" + i, "1", "Top-of-atmosphere reflectance");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + i), band);
            }
            autoGrouping += ":" + "rtoa";
        }
        if (outputRtoaGc) {
            for (int bi : merband12_ix) {
                Band band = addBand(targetProduct, "rtosa_gc_" + bi, "1", "Gas corrected top-of-atmosphere reflectance, input to AC");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + bi), band);
            }
            autoGrouping += ":" + "rtosa_gc";
        }
        if (outputRtoaGc_aann) {
            for (int bi : merband12_ix) {
                Band band = addBand(targetProduct, "rtosagc_aann_" + bi, "1", "Gas corrected top-of-atmosphere reflectance, output from AANN");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + bi), band);
            }
            autoGrouping += ":" + "rtosagc_aann";
        }

        if (outputRpath) {
            for (int bi : merband12_ix) {
                Band band = addBand(targetProduct, "rpath_" + bi, "1", "Path-radiance reflectances");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + bi), band);
            }
            autoGrouping += ":" + "rpath";
        }

        if (outputTdown) {
            for (int bi : merband12_ix) {
                Band band = addBand(targetProduct, "tdown_" + bi, "1", "Transmittance of downweling irradiance");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + bi), band);
            }
            autoGrouping += ":" + "tdown";
        }

        if (outputTup) {
            for (int bi : merband12_ix) {
                Band band = addBand(targetProduct, "tup_" + bi, "1", "Transmittance of upweling irradiance");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + bi), band);
            }
            autoGrouping += ":" + "tup";
        }

        if (outputRwa) {
            for (int index : merband12_ix) {
                final Band band = addBand(targetProduct, "rwa_" + index, "1", "Angular dependent water leaving reflectances");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + index), band);
            }
            autoGrouping += ":" + "rwa_";
        }

        if (outputRwn) {
            for (int index : merband12_ix) {
                final Band band = addBand(targetProduct, "rwn_" + index, "1", "Normalized water leaving reflectances");
                ProductUtils.copySpectralBandProperties(sourceProduct.getBand("radiance_" + index), band);
            }
            autoGrouping += ":" + "rwn_";
        }

        if (outputOos) {
            addBand(targetProduct, "oos_rtosa", "1", "Gas corrected top-of-atmosphere reflectances are out of scope of nn training dataset");
            addBand(targetProduct, "oos_rwa", "1", "Water leavin reflectances are out of scope of nn training dataset");
            autoGrouping += ":" + "oos_";
        }

        addBand(targetProduct, "iop_apig", "m^-1", "Absorption coefficient of phytoplankton pigments at 443 nm");
        addBand(targetProduct, "iop_adet", "m^-1", "Absorption coefficient of detritus at 443 nm");
        addBand(targetProduct, "iop_agelb", "m^-1", "Absorption coefficient of gelbstoff at 443 nm");
        addBand(targetProduct, "iop_bpart", "m^-1", "Scattering coefficient of marine paticles at 443 nm");
        addBand(targetProduct, "iop_bwit", "m^-1", "Scattering coefficient of white particles at 443 nm");
        addVirtualBand(targetProduct, "iop_adg", "iop_adet + iop_agelb", "m^-1", "Detritus + gelbstoff absorption at 443 nm");
        addVirtualBand(targetProduct, "iop_atot", "iop_apig + iop_adet + iop_agelb", "m^-1", "phytoplankton + detritus + gelbstoff absorption at 443 nm");
        addVirtualBand(targetProduct, "iop_btot", "iop_bpart + iop_bwit", "m^-1", "total particle scattering at 443 nm");

        if (outputKd) {
            addBand(targetProduct, "kd489", "m^-1", "Irradiance attenuation coefficient at 489 nm");
            addBand(targetProduct, "kdmin", "m^-1", "Mean irradiance attenuation coefficient at the three bands with minimum kd");
            addVirtualBand(targetProduct, "kd_z90max", "1 / kdmin", "m", "Depth of the water column from which 90% of the water leaving irradiance comes from");
            autoGrouping += ":" + "kd";
        }

        addVirtualBand(targetProduct, "conc_tsm", "(iop_bpart + iop_bwit) * " + TSMfak, "g m^-3", "Total suspended matter dry weight concentration");
        addVirtualBand(targetProduct, "conc_chl", "pow(iop_apig, " + CHLexp + ") * " + CHLfak, "mg m^-3", "Chlorophyll concentration");

        if (outputUncertainties) {
            addBand(targetProduct, "unc_apig", "m^-1", "uncertainty of pigment absorption coefficient");
            addBand(targetProduct, "unc_adet", "m^-1", "uncertainty of detritus absorption coefficient");
            addBand(targetProduct, "unc_agelb", "m^-1", "uncertainty of dissolved gelbstoff absorption coefficient");
            addBand(targetProduct, "unc_bpart", "m^-1", "uncertainty of particle scattering coefficient");
            addBand(targetProduct, "unc_bwit", "m^-1", "uncertainty of white particle scattering coefficient");
            addBand(targetProduct, "unc_adg", "m^-1", "uncertainty of total gelbstoff absorption coefficient");
            addBand(targetProduct, "unc_atot", "m^-1", "uncertainty of total water constituent absorption coefficient");
            addBand(targetProduct, "unc_btot", "m^-1", "uncertainty of total water constituent scattering coefficient");
            addVirtualBand(targetProduct, "unc_tsm", "unc_btot * " + TSMfak, "g m^-3", "uncertainty of total suspended matter (TSM) dry weight concentration");
            addVirtualBand(targetProduct, "unc_chl", "pow(unc_apig, " + CHLexp + ") * " + CHLfak, "mg m^-3", "uncertainty of chlorophyll concentration");
            if (outputKd) {
                addBand(targetProduct, "unc_kd489", "m^-1", "uncertainty of irradiance attenuation coefficient");
                addBand(targetProduct, "unc_kdmin", "m^-1", "uncertainty of mean irradiance attenuation coefficient");
                addVirtualBand(targetProduct, "unc_kd_z90max", "abs(kd_z90max - 1.0 / abs(kdmin - unc_kdmin))", "m", "uncertainty of depth of the water column from which 90% of the water leaving irradiance comes from");
            }
            autoGrouping += ":" + "unc";
        }

        // flag settings
// flags = BitSetter.setFlag(flags, 0, rtosa_oor_flag);
// flags = BitSetter.setFlag(flags, 1, rtosa_oos_flag);
// flags = BitSetter.setFlag(flags, 2, rw_oor_flag);
// flags = BitSetter.setFlag(flags, 3, iop_oor_flag);
// flags = BitSetter.setFlag(flags, iv + 4, iop_at_max_flag[iv]); 5 flags for 5 IOPs
// flags = BitSetter.setFlag(flags, iv + 9, iop_at_min_flag[iv]); 5 flags for 5 IOPs
// flags = BitSetter.setFlag(flags, 14, rw_oos_flag);
// flags = BitSetter.setFlag(flags, 15, kd489_oor_flag);
// flags = BitSetter.setFlag(flags, 16, kdmin_oor_flag);
// flags = BitSetter.setFlag(flags, 17, kd489_at_max_flag);
// flags = BitSetter.setFlag(flags, 18, kdmin_at_max_flag);

        Band l2_flags = targetProduct.addBand("l2_flags", ProductData.TYPE_UINT32);
        l2_flags.setDescription("Quality flags");

        FlagCoding flagCoding = new FlagCoding("l2_flags");
        //0
        flagCoding.addFlag("Rtosa_OOR", 0x01, "The input spectrum to atmospheric correction neural net out of training range");
        flagCoding.addFlag("Rtosa_OOS", 0x02, "The input spectrum to atmospheric correction neural net was unknown");
        flagCoding.addFlag("Rwa_OOR", 0x04, "One of the inputs to the IOP retrieval neural net is out of training range");
        flagCoding.addFlag("Iop_OOR", 0x08, "One of the IOPs is out of range");
        flagCoding.addFlag("Apig_at_max", 0x010, "Apig output of the IOP retrieval neural net is at its maximum");
        //5
        flagCoding.addFlag("Adet_at_max", 0x020, "Adet output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Agelb_at_max", 0x040, "Agelb output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Bpart_at_max", 0x080, "Bpart output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Bwit_at_max", 0x0100, "Bwit output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Apig_at_min", 0x0200, "Apig output of the IOP retrieval neural net is at its maximum");
        //10
        flagCoding.addFlag("Adet_at_min", 0x0400, "Adet output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Agelb_at_min", 0x0800, "Agelb output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Bpart_at_min", 0x01000, "Bpart output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Bwit_at_min", 0x02000, "Bwit output of the IOP retrieval neural net is at its maximum");
        flagCoding.addFlag("Rwa_OOS", 0x04000, "The Rwa input spectrum to IOP neural net is unknown");
        //15
        flagCoding.addFlag("Kd489_OOR", 0x08000, "kd489 is out of range");
        flagCoding.addFlag("Kdmin_OOR", 0x010000, "kdmin is out of range");
        flagCoding.addFlag("Kd489_at_max", 0x020000, "kdmin is at max");
        flagCoding.addFlag("Kdmin_at_max", 0x040000, "kdmin is at max");

        targetProduct.getFlagCodingGroup().add(flagCoding);
        l2_flags.setSampleCoding(flagCoding);

        Color[] maskColors = {Color.RED, Color.ORANGE, Color.YELLOW, Color.BLUE, Color.GREEN, Color.PINK, Color.MAGENTA, Color.CYAN, Color.GRAY};
        String[] flagNames = flagCoding.getFlagNames();
        for (int i = 0; i < flagNames.length; i++) {
            String flagName = flagNames[i];
            MetadataAttribute flag = flagCoding.getFlag(flagName);
            targetProduct.addMask(flagName, "l2_flags." + flagName, flag.getDescription(), maskColors[i % maskColors.length], 0.5);
        }
        targetProduct.setAutoGrouping(autoGrouping);
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        for (int i = 0; i < BAND_COUNT; i++) {
            assertSourceBand("radiance_" + (i + 1));
        }
        assertSourceBand("l1_flags");

        if (sourceProduct.getSceneGeoCoding() == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }

        try {
            algorithm = new C2rccMerisAlgorithm();
        } catch (IOException e) {
            throw new OperatorException(e);
        }

        algorithm.setTemperature(temperature);
        algorithm.setSalinity(salinity);

        algorithm.setOutputRtoaGcAann(outputRtoaGc_aann);
        algorithm.setOutputRpath(outputRpath);
        algorithm.setOutputTdown(outputTdown);
        algorithm.setOutputTup(outputTup);
        algorithm.setOutputRwa(outputRwa);
        algorithm.setOutputRwn(outputRwn);
        algorithm.setOutputOos(outputOos);
        algorithm.setOutputKd(outputKd);
        algorithm.setOutputUncertainties(outputUncertainties);

        if (useDefaultSolarFlux) {  // not the sol flux values from the input product
            solarFluxLazyLookup = new SolarFluxLazyLookup(DEFAULT_SOLAR_FLUX);
        } else {
            double[] solfluxFromL1b = new double[BAND_COUNT];
            for (int i = 0; i < BAND_COUNT; i++) {
                solfluxFromL1b[i] = sourceProduct.getBand("radiance_" + (i + 1)).getSolarFlux();
            }
            if (isSolfluxValid(solfluxFromL1b)) {
                algorithm.setSolflux(solfluxFromL1b);
            } else {
                throw new OperatorException("Invalid solar flux in source product!");
            }
        }
        if (!useEcmwfAuxData) {
            initAtmosphericAuxdata();
        }
    }

    public static boolean isValidInput(Product product) {
        for (int i = 0; i < BAND_COUNT; i++) {
            if (!product.containsBand("radiance_" + (i + 1))) {
                return false;
            }
        }
        if (!product.containsBand("l1_flags")) {
            return false;
        }
        if (!product.containsRasterDataNode("dem_alt")) {
            return false;
        }
        if (!product.containsRasterDataNode("sun_zenith")) {
            return false;
        }
        if (!product.containsRasterDataNode("sun_azimuth")) {
            return false;
        }
        if (!product.containsRasterDataNode("view_zenith")) {
            return false;
        }
        if (!product.containsRasterDataNode("view_azimuth")) {
            return false;
        }
        if (!product.containsRasterDataNode("atm_press")) {
            return false;
        }
        if (!product.containsRasterDataNode("ozone")) {
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
                throw new OperatorException("Unable to create provider for atmospheric ancillary data.", e);
            }
        } else {
            final AncDownloader ancDownloader = new AncDownloader(ANC_DATA_URI);
            final AncRepository ancRepository = new AncRepository(new File(atmosphericAuxDataPath), ancDownloader);
            AncDataFormat ozoneFormat = createOzoneFormat(ozone_default);
            AncDataFormat pressureFormat = createPressureFormat(pressure_default);
            atmosphericAuxdata = new AtmosphericAuxdataDynamic(ancRepository, ozoneFormat, pressureFormat);
        }
    }

    private void assertSourceBand(String name) {
        if (sourceProduct.getBand(name) == null) {
            throw new OperatorException("Invalid source product, band '" + name + "' required");
        }
    }

    private Band addBand(Product targetProduct, String name, String unit, String description) {
        Band targetBand = targetProduct.addBand(name, ProductData.TYPE_FLOAT32);
        targetBand.setUnit(unit);
        targetBand.setDescription(description);
        targetBand.setGeophysicalNoDataValue(Double.NaN);
        targetBand.setNoDataValueUsed(true);
        return targetBand;
    }

    private void addVirtualBand(Product targetProduct, String name, String expression, String unit, String description) {
        Band band = targetProduct.addBand(name, expression);
        band.setUnit(unit);
        band.setDescription(description);
        band.getSourceImage(); // trigger source image creation
        band.setGeophysicalNoDataValue(Double.NaN);
        band.setNoDataValueUsed(true);
    }

    private static boolean isSolfluxValid(double[] solflux) {
        for (double v : solflux) {
            if (v <= 0.0) {
                return false;
            }
        }
        return true;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(C2rccMerisOperator.class);
        }
    }
}
