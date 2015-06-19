package org.esa.s2tbx.dataio.s2.l1b;

import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripDirFilename;
import org.esa.s2tbx.dataio.s2.filepatterns.S2DatastripFilename;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class S2L1bDatastripFilenameTest {

    @Test
    public void testFileName() throws Exception
    {
        S2DatastripFilename s2gf = S2DatastripFilename.create("S2A_OPER_MTD_L1C_DS_CGS1_20130621T120000_S20091211T165928");
        assertNotNull(s2gf);
        assertEquals("S2A_OPER_MTD_L1C_DS_CGS1_20130621T120000_S20091211T165928", s2gf.name);
        assertEquals("S2A", s2gf.missionID);
        assertEquals("OPER", s2gf.fileClass);
        assertEquals("MTD_", s2gf.fileCategory);
        assertEquals("L1C_DS", s2gf.fileSemantic);
        assertEquals("CGS1", s2gf.siteCentre);
        assertEquals("20130621T120000", s2gf.creationDate);
        assertEquals("S20091211T165928", s2gf.applicabilityStart);
    }

    @Test
    public void testDirFileName() throws Exception
    {
        S2DatastripDirFilename s2gf = S2DatastripDirFilename.create("S2A_OPER_MSI_L1C_DS_CGS1_20130621T120000_S20091211T165928_N01.01", null);
        assertNotNull(s2gf);
        assertEquals("S2A_OPER_MSI_L1C_DS_CGS1_20130621T120000_S20091211T165928_N01.01", s2gf.name);
        assertEquals("S2A", s2gf.missionID);
        assertEquals("OPER", s2gf.fileClass);
        assertEquals("MSI_", s2gf.fileCategory);
        assertEquals("L1C_DS", s2gf.fileSemantic);
        assertEquals("CGS1", s2gf.siteCentre);
        assertEquals("20130621T120000", s2gf.creationDate);
        assertEquals("S20091211T165928", s2gf.applicabilityStart);

        S2DatastripFilename afin = s2gf.getDatastripFilename(null);
        assertEquals("S2A_OPER_MTD_L1C_DS_CGS1_20130621T120000_S20091211T165928.xml", afin.name);

    }

}
