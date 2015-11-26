/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2013-2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s2tbx.dataio.s2.l1b;


import https.psd_13_sentinel2_eo_esa_int.psd.s2_pdi_level_1b_datastrip_metadata.Level1B_DataStrip;
import https.psd_13_sentinel2_eo_esa_int.psd.s2_pdi_level_1b_granule_metadata.Level1B_Granule;
import https.psd_13_sentinel2_eo_esa_int.psd.user_product_level_1b.Level1B_User_Product;
import junit.framework.Assert;
import org.esa.s2tbx.dataio.s2.S2MetadataType;
import org.junit.Test;

import javax.xml.bind.*;
import java.io.InputStream;

/**
 * @author  opicas-p
 */
public class MetadataReaderTest {

    public Level1B_User_Product getUserProduct() throws Exception
    {
        Level1B_User_Product o = (Level1B_User_Product) readJaxbFromStreamResource("S2A_OPER_MTD_SAFL1B_PDMC_20140926T120000_R069_V20130707T171925_20130707T172037.xml");
        return o;
    }

    public Object readJaxbFromStreamResource(String streamResource) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext
                .newInstance(S2MetadataType.L1B);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        InputStream stream = getClass().getResourceAsStream(streamResource);

        Object ob =  unmarshaller.unmarshal(stream);
        Object casted = ((JAXBElement)ob).getValue();

        return casted;
    }

    @Test
    public void test1() throws Exception
    {
        Level1B_User_Product o = getUserProduct();

        Assert.assertNotNull(o);
    }

    @Test
    public void test2() throws Exception
    {
        Level1B_Granule o = null;

        try {
            JAXBContext jaxbContext = JAXBContext
                    .newInstance(S2MetadataType.L1B);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Marshaller marshaller = jaxbContext.createMarshaller();

            InputStream stream = getClass().getResourceAsStream("S2A_OPER_MTD_L1B_GR_MPS__20140926T120000_S20130707T171927_D06.xml");

            Object ob =  unmarshaller.unmarshal(stream);

            o = (Level1B_Granule) ((JAXBElement)ob).getValue();

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test3() throws Exception
    {
        Level1B_DataStrip o = null;

        try {
            JAXBContext jaxbContext = JAXBContext
                    .newInstance(S2MetadataType.L1B);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Marshaller marshaller = jaxbContext.createMarshaller();

            InputStream stream = getClass().getResourceAsStream("S2A_OPER_MTD_L1B_DS_MPS__20140926T120000_S20130707T171925.xml");

            Object ob =  unmarshaller.unmarshal(stream);

            o = (Level1B_DataStrip) ((JAXBElement)ob).getValue();

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
