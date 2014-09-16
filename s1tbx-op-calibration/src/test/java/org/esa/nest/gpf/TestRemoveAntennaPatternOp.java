/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test for Calibration Operator.
 */
public class TestRemoveAntennaPatternOp {

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new RemoveAntennaPatternOp.Spi();

    private final static String inputPathWSM = TestUtils.rootPathTestProducts + "\\input\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim";

    private final static String inputPathIMP = TestUtils.rootPathTestProducts + "\\input\\subset_0_of_ERS-1_SAR_PRI-ORBIT_32506_DATE__02-OCT-1997_14_53_43.dim";
    private final static String inputPathIMS = TestUtils.rootPathTestProducts + "\\input\\subset_0_of_ERS-2_SAR_SLC-ORBIT_10249_DATE__06-APR-1997_03_09_34.dim";

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR", "GeoTIFF", "SCS_U"};
    private String[] exceptionExemptions = {"not supported",
            "calibration has already been applied",
            "Cannot apply calibration to coregistered product"};

    @Test
    public void testProcessingWSM() throws Exception {
        processFile(inputPathWSM, null);
    }

    @Test
    public void testProcessingIMP() throws Exception {
        processFile(inputPathIMP, null);
    }

    @Test
    public void testProcessingIMS() throws Exception {
        processFile(inputPathIMS, null);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @param inputPath    the path to the input product
     * @param expectedPath the path to the expected product
     * @throws Exception general exception
     */
    public void processFile(String inputPath, String expectedPath) throws Exception {
        final File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputPath + " not found");
            return;
        }

        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final RemoveAntennaPatternOp op = (RemoveAntennaPatternOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);

    }

    @Test
    public void testProcessAllASAR() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathASAR, productTypeExemptions, null);
    }

    @Test
    public void testProcessAllERS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathERS, productTypeExemptions, null);
    }
}