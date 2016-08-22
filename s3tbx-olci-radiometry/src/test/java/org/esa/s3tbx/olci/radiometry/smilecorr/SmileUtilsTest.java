/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.esa.s3tbx.olci.radiometry.rayleighcorrection.AuxiliaryValues;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class SmileUtilsTest {

    private AuxiliaryValues auxiliaryValues;

    @Before
    public void setUp() throws Exception {
        auxiliaryValues = new AuxiliaryValues();
    }

    @Test
    public void testMultiple2ArrayNullAndNotNull() throws Exception {

        assertArrayEquals(new float[]{1, 4, 9}, SmileUtils.multiple2ArrayFloat(new float[]{1, 2, 3}, new float[]{1, 2, 3}), 0);
        assertArrayEquals(new float[]{0, 4, 10}, SmileUtils.multiple2ArrayFloat(new float[]{0, 1, 2}, new float[]{3, 4, 5}), 0);
        assertArrayEquals(new float[]{0, 4, 10, 10}, SmileUtils.multiple2ArrayFloat(new float[]{0, 1, 2, 1}, new float[]{3, 4, 5, 10}), 0);
    }


    @Test
    public void testMultiple3ArrayNullAndNotNull() throws Exception {
        assertArrayEquals(new float[]{0, 4, 20}, SmileUtils.multiple3ArrayFloat(new float[]{0, 1, 2}, new float[]{3, 4, 5}, new float[]{0, 1, 2}), 0);
        assertArrayEquals(new float[]{0, 4, 10, 10}, SmileUtils.multiple3ArrayFloat(new float[]{0, 1, 2, 1}, new float[]{3, 4, 5, 10}, new float[]{1, 1, 1, 1}), 0);
        float[] actuals = SmileUtils.multiple3ArrayFloat(new float[]{1, 2, 3}, new float[]{4, 5, 6}, new float[]{7, 8, 9});
        assertEquals(3, actuals.length);
        assertArrayEquals(new float[]{28, 80, 162}, actuals, 0);
    }

    @Test
    public void convertDegsToRads() throws Exception {
        double[] degToRads = SmileUtils.convertDegreesToRadians(new double[]{1.0, 2.0, 3.0});
        assertArrayEquals(new double[]{0.017453292519943295, 0.03490658503988659, 0.05235987755982988}, degToRads, 1e-8);
    }

    @Test
    public void getAirMass() throws Exception {
        auxiliaryValues.setViewZenithAngles(new double[]{1.0, 2.0, 3});
        auxiliaryValues.setSunZenithAngles(new double[]{1.0, 2.0, 3});

        auxiliaryValues.setSunAzimuthAngles(new double[]{1.0, 2.0, 3});
        auxiliaryValues.setViewAzimuthAngles(new double[]{1.0, 2.0, 3});

        auxiliaryValues.setViewZenithAnglesRad();
        auxiliaryValues.setSunZenithAnglesRad();
        auxiliaryValues.setCosOZARads();
        auxiliaryValues.setCosSZARads();

        double[] airMass = SmileUtils.getAirMass(auxiliaryValues);
        assertArrayEquals(new double[]{2.0003046560878155, 2.0012190885976433, 2.002744691995842}, airMass, 1e-8);
    }

    @Test
    public void getAziDiff() throws Exception {
        auxiliaryValues.setSunAzimuthAngles(new double[]{2.0, 8.0, 10.0});
        auxiliaryValues.setViewAzimuthAngles(new double[]{4.0, 5.0, 6.0});
        double[] aziDiff = SmileUtils.getAziDiff(auxiliaryValues);
        assertArrayEquals(new double[]{0.03490658503988567, 0.05235987755983066, 0.0698131700797739}, aziDiff, 1e-8);
    }
}