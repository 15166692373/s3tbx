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
package org.esa.nest.dataio.dem.ace;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.nest.dataio.dem.ElevationModel;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Apr 25, 2008
 * To change this template use File | Settings | File Templates.
 */
public class TestACEElevationModel {

    private final ACEElevationModelDescriptor demDescriptor = new ACEElevationModelDescriptor();

    private static double[] expectedValues = {
            1116.7764282226562, 1162.7941589355469, 1091.9768981933594, 1046.3471984863281
    };

    @Test
    public void testElevationModel() throws Exception {

        if (!demDescriptor.isDemInstalled()) {
            TestUtils.skipTest(this, demDescriptor.getName()+" not installed");
            return;
        }

        final ElevationModel dem = demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
        int height = 2;
        int width = 2;
        final double[] demValues = new double[width*height];
        int count = 0;

        final GeoPos geoPos = new GeoPos(-18, 20);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                geoPos.setLocation(geoPos.getLat() + x, geoPos.getLon() + y);
                try {
                    demValues[count++] = dem.getElevation(geoPos);
                } catch (Exception e) {
                    assertFalse("Get Elevation threw", true);
                }
            }
        }

        assertTrue(Arrays.equals(expectedValues, demValues));
    }
}
