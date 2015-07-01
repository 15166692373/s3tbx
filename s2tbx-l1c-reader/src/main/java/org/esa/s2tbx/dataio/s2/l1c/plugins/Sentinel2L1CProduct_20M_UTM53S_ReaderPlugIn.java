
package org.esa.s2tbx.dataio.s2.l1c.plugins;

import org.esa.s2tbx.dataio.s2.l1c.Sentinel2L1CProduct20MReaderPlugIn;

/**
 * Reader plugin for S2 MSI L1C over WGS84 / UTM Zone 53 S
 */
public class Sentinel2L1CProduct_20M_UTM53S_ReaderPlugIn extends Sentinel2L1CProduct20MReaderPlugIn {

    @Override
    public String getEPSG()
    {
        return "EPSG:32753";
    }

}
