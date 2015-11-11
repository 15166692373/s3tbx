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
package org.esa.s1tbx.sentinel1.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test for TOPSARDeburst Operator.
 */
public class TestDeburstOperator {

    private OperatorSpi spi;

    final String s1FolderFilePath = "P:\\s1tbx\\s1tbx\\Data\\First Images\\S1A_IW_SLC__1SDV_20140823T052821_20140823T052840_002063_00205B_4658.SAFE\\manifest.safe";

    @Before
    public void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        spi = new TOPSARDeburstOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    @Ignore
    public void testProcessing() throws Exception {
        final File inputFile = new File(s1FolderFilePath);
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final TOPSARDeburstOp op = (TOPSARDeburstOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);

        final Band targetBand = targetProduct.getBandAt(0);
        assertNotNull(targetBand);

        final int bandWidth = 5000;//targetBand.getRasterWidth();
        final int bandHeight = 5000;//targetBand.getRasterHeight();

        // readPixels: execute computeTiles()
        final float[] floatValues = new float[bandWidth*bandHeight];
        targetBand.readPixels(0, 0,  bandWidth, bandHeight, floatValues, ProgressMonitor.NULL);
    }

}
