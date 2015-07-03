
package org.esa.s2tbx.dataio.s2.l1c.plugins;

import org.esa.s2tbx.dataio.s2.l1c.Sentinel2L1CProduct10MReaderPlugIn;

/**
 * Reader plugin for S2 MSI L1C over WGS84 / UTM Zone 42 S
 */
public class Sentinel2L1CProduct_10M_UTM42S_ReaderPlugIn extends Sentinel2L1CProduct10MReaderPlugIn {

    @Override
    public String getEPSG()
    {
        return "EPSG:32742";
    }

}
