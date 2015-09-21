package org.esa.s2tbx.dataio.s2;

import org.junit.Assert;
import org.apache.commons.lang.SystemUtils;
import org.esa.s2tbx.dataio.Utils;
import org.esa.snap.util.io.FileUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * @author opicas-p
*/
public class ShortenTest {

    @Before
    public void beforeMethod() {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
    }

    /**
     * Test we can shorten a long directory name. Can't test with a very long directory name since
     * it is not valid on Windows.
     *
     * @throws Exception
     */
    @Test
    public void testLongDirectoryName() throws Exception {
        String mediumPath = "C:\\5A3AA7c3-475c-42a5-9a25-94d6a93c67b7\\S2A_OPER_PRD_MSIL1C_PDMC_20130621T120000_R065_V20091211T165928_20091211T170025";
        File directory = new File(mediumPath);

        if (directory.mkdirs()) {
            String shortPath = Utils.GetShortPathName(mediumPath);
            Assert.assertEquals("C:\\5A3AA7~1\\S2A_OP~1", shortPath);
            FileUtils.deleteTree(directory.getParentFile());
        }
    }

    /**
     * Test we can shorten a long file name. Can't test with a very long file name since
     * it is not valid on Windows.
     *
     * @throws Exception
     */
    @Test
    public void testLongFileName() throws Exception {
        String mediumPath = "C:\\5A3AA7c3-475c-42a5-9a25-94d6a93c67b7\\S2A_OPER_PRD_MSIL1C_PDMC_20130621T120000_R065_V20091211T165928_20091211T170025.JP2";
        File mediumFile = new File(mediumPath);
        if (mediumFile.getParentFile().mkdir() && mediumFile.createNewFile()) {
            String shortPath = Utils.GetShortPathName(mediumPath);
            Assert.assertEquals("C:\\5A3AA7~1\\S2A_OP~1.JP2", shortPath);
            FileUtils.deleteTree(mediumFile.getParentFile());
        }
    }

    /**
     * Test GetIterativeShortPathName returns "" when the path does not exist.
     * It also tests we cleaned well the files created
     *
     * @throws Exception
     */
    @Test
    public void testVeryLongFileName2() throws Exception {
        String nonExistingPath = "C:\\5a3aa7c3-475c-42a5-9a25-94d6a93c67b7";
        String shortPath = Utils.GetIterativeShortPathName(nonExistingPath);
        Assert.assertEquals("", shortPath);
    }
}
