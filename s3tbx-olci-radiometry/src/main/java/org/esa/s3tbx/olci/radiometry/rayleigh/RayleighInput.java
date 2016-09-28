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

package org.esa.s3tbx.olci.radiometry.rayleigh;

/**
 * @author muhammad.bc.
 */
public class RayleighInput {
    private float[] sourceRefls;
    private float[] lowerRefls;
    private float[] upperRefls;

    float sourceReflectance;
    float lowerReflectance;
    float upperReflectance;

    int sourceIndex;
    int lowerWaterIndex;
    int upperWaterIndex;

    public RayleighInput(float sourceReflectance, float lowerReflectance, float upperReflectance, int sourceIndex, int lowerWaterIndex, int upperWaterIndex) {
        this.sourceReflectance = sourceReflectance;
        this.lowerReflectance = lowerReflectance;
        this.upperReflectance = upperReflectance;

        this.sourceIndex = sourceIndex;
        this.lowerWaterIndex = lowerWaterIndex;
        this.upperWaterIndex = upperWaterIndex;
    }

    public RayleighInput(float[] sourceRefl, float[] lowerRefl, float[] upperRefl, int sourceIndx, int lowerWaterIndx, int upperWaterIndx) {

        this.sourceRefls = sourceRefl;
        this.lowerRefls = lowerRefl;
        this.upperRefls = upperRefl;
        this.sourceIndex = sourceIndx;
        this.lowerWaterIndex = lowerWaterIndx;
        this.upperWaterIndex = upperWaterIndx;

    }


    public float getSourceReflectance() {
        return sourceReflectance;
    }


    public float getLowerReflectance() {
        return lowerReflectance;
    }


    public float getUpperReflectance() {
        return upperReflectance;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public int getLowerWaterIndex() {
        return lowerWaterIndex;
    }

    public int getUpperWaterIndex() {
        return upperWaterIndex;
    }

    public float[] getSourceReflectences() {
        return sourceRefls;
    }

    public float[] getLowerReflectences() {
        return lowerRefls;
    }

    public float[] getUpperReflectences() {
        return upperRefls;
    }
}
