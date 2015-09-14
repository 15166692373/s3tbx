package org.esa.s3tbx.c2rcc.anc;

import java.io.IOException;

public class AtmosphericAuxdataDynamic implements AtmosphericAuxdata {

    private final DataInterpolatorDynamic ozoneInterpolator;
    private final DataInterpolatorDynamic pressInterpolator;

    public AtmosphericAuxdataDynamic(AncRepository ancRepository, final AncDataFormat ozoneFormat, final AncDataFormat pressFormat) {
        ozoneInterpolator = new DataInterpolatorDynamic(ozoneFormat, ancRepository);
        pressInterpolator = new DataInterpolatorDynamic(pressFormat, ancRepository);
    }

    @Override
    public double getOzone(double timeMJD, double lat, double lon) throws IOException {
        return ozoneInterpolator.getValue(timeMJD, lat, lon);
    }

    @Override
    public double getSurfacePressure(double timeMJD, double lat, double lon) throws IOException {
        return pressInterpolator.getValue(timeMJD, lat, lon);
    }

    @Override
    public void dispose() {
        ozoneInterpolator.dispose();
        pressInterpolator.dispose();
    }
}
