package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.util.math.RsMathUtils;

import java.io.IOException;

/**
 * Radiance/reflectance conversion for MERIS
 *
 * @author olafd
 */
public class MerisRadReflConverter implements RadReflConverter {

    private String conversionMode;

    private Rad2ReflAuxdata rad2ReflAuxdata;

    public MerisRadReflConverter(Product sourceProduct, String conversionMode) {
        this.conversionMode = conversionMode;

        try {
            rad2ReflAuxdata = Rad2ReflAuxdata.loadAuxdata(sourceProduct.getProductType());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public float[] convert(Product sourceProduct, Sample[] sourceSamples) {
        final float sza = sourceSamples[Sensor.MERIS.getNumSpectralBands()].getFloat(); // in degrees
        float[] spectralInputValues = new float[Sensor.MERIS.getNumSpectralBands()];
        float[] spectralOutputValues = new float[Sensor.MERIS.getNumSpectralBands()];

        final int detectorIndex = sourceSamples[Sensor.MERIS.getNumSpectralBands() + 1].getInt();
        if (detectorIndex >= 0) {
            double[] solarFluxes = getMerisSolarFluxes(detectorIndex);

            for (int i = 0; i < Sensor.MERIS.getNumSpectralBands(); i++) {
                spectralInputValues[i] = sourceSamples[i].getFloat();
                if (conversionMode.equals("RAD_TO_REFL")) {
                    spectralOutputValues[i] = radianceToReflectance(spectralInputValues[i], sza, (float) solarFluxes[i]);
                } else {
                    spectralOutputValues[i] = reflectanceToRadiance(spectralInputValues[i], sza, (float) solarFluxes[i]);
                }
            }
        }

        return spectralOutputValues;
    }

    public String getSpectralBandAutogroupingString() {
        return conversionMode.equals("RAD_TO_REFL") ? Sensor.MERIS.getReflAutogroupingString() : Sensor.MERIS.getRadAutogroupingString();
    }

    public float radianceToReflectance(float rad, float sza, float solarFlux) {
        return RsMathUtils.radianceToReflectance(rad, sza, solarFlux);
    }

    public float reflectanceToRadiance(float refl, float sza, float solarFlux) {
        return RsMathUtils.reflectanceToRadiance(refl, sza, solarFlux);
    }

    private double[] getMerisSolarFluxes(int detectorIndex) {
        return rad2ReflAuxdata.getDetectorSunSpectralFluxes()[detectorIndex];
    }
}
