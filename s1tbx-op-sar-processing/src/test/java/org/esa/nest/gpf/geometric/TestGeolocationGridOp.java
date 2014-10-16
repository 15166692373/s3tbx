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
package org.esa.nest.gpf.geometric;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test for Geolocation Grid.
 */
public class TestGeolocationGridOp {

    static {
        TestUtils.initTestEnvironment();
    }
    private final static OperatorSpi spi = new GeolocationGridGeocodingOp.Spi();

    private final static String inputPathWSM = TestUtils.rootPathTestProducts + "\\input\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim";
    private final static String expectedPathWSM = TestUtils.rootPathTestProducts + "\\expected\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977_EC.dim";

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX"};
    private String[] exceptionExemptions = {"not supported", "already map projected"};

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessing() throws Exception {
        final File inputFile = new File(inputPathWSM);
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputPathWSM + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final GeolocationGridGeocodingOp op = (GeolocationGridGeocodingOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        final String[] excemptionList = {"total_size"};
        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        TestUtils.compareProducts(targetProduct, expectedPathWSM, excemptionList);
    }

    @Test
    public void testProcessAllASAR() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathASAR, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllERS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathERS, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllALOS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathALOS, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllRadarsat2() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathRadarsat2, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllTerraSARX() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathTerraSarX, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllCosmo() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathCosmoSkymed, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllSentinel1() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathSentinel1, null, exceptionExemptions);
    }
}