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

package org.esa.s2tbx.dataio.s2.ortho;

import org.esa.s2tbx.dataio.s2.Sentinel2ProductReader;
import org.esa.s2tbx.dataio.s2.filepatterns.S2ProductFilename;
import org.esa.s2tbx.dataio.s2.l1c.Sentinel2L1CProductReader;
import org.esa.s2tbx.dataio.s2.l2a.Sentinel2L2AProductReader;
import org.esa.s2tbx.dataio.s2.ortho.filepatterns.S2OrthoGranuleMetadataFilename;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.util.SystemUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

import static org.esa.s2tbx.dataio.s2.ortho.S2CRSHelper.epsgToDisplayName;
import static org.esa.s2tbx.dataio.s2.ortho.S2CRSHelper.epsgToShortDisplayName;

/**
 * @author Nicolas Ducoin
 */
public abstract class S2OrthoProduct20MReaderPlugIn extends S2OrthoProductReaderPlugIn {

    @Override
    public ProductReader createReaderInstance() {
        SystemUtils.LOG.info("Building product reader 20M");

        if (getLevel() != null && getLevel().equals("L2A")) {
            return new Sentinel2L2AProductReader(this, Sentinel2ProductReader.ProductInterpretation.RESOLUTION_20M, getEPSG());
        } else {
            return new Sentinel2L1CProductReader(this, Sentinel2ProductReader.ProductInterpretation.RESOLUTION_20M, getEPSG());
        }
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{String.format("%s-20M-%s", getFormatName(), epsgToShortDisplayName(getEPSG()))};
    }

    @Override
    public String getDescription(Locale locale) {
        return String.format("Sentinel-2 MSI %s - Resampled at 20m resolution - %s", getLevel(), epsgToDisplayName(getEPSG()));
    }


    //This method is overriden to make the specific resolution checking in level 2 products
    @Override
    public DecodeQualification getDecodeQualification(Object input) {

        //call to S2OrthoProductReaderPlugIn getDecodeQualification(Object input)
        DecodeQualification decodeQualification = super.getDecodeQualification(input);

        //If decodeQualification is already unable or level is not level 2, the method return the value of parent's method
        if(decodeQualification==DecodeQualification.UNABLE ||  !(getLevel().equals("L2A")))
            return decodeQualification;

        //If the level is 2, the plugin is able if it exists the folder with the corresponding resolution
        File file = (File) input;
        String fileNameComplete = file.toString(); //file name with full path
        String fileName = file.getName(); //file name without path

        //the qualification is set to unable and it is changed only if a folder "R20m" exist into
        //the img_data folder of granules
        decodeQualification=DecodeQualification.UNABLE;

        if (S2OrthoGranuleMetadataFilename.isGranuleFilename(fileName)) { //when input is a granule
            Path rootPath = new File(fileNameComplete).toPath().getParent();
            File imgFolder = rootPath.resolve("IMG_DATA").toFile();
            File[] files = imgFolder.listFiles();
            if (files != null) {
                for (File imgData : files) {
                    if (imgData.isDirectory()) {
                        if (imgData.getName().equals("R20m")) {
                            return DecodeQualification.INTENDED;
                        }
                    }
                }
            }
        } else if (S2ProductFilename.isMetadataFilename(fileName)) { //when input is the global xml

            Path rootPath = new File(fileNameComplete).toPath().getParent();
            File granuleFolder = rootPath.resolve("GRANULE").toFile();
            File[] files = granuleFolder.listFiles();
            if (files != null) {
                for (File granule : files) {
                    if (granule.isDirectory()) {
                        Path granulePath = new File(granule.toString()).toPath();
                        File internalGranuleFolder = granulePath.resolve("IMG_DATA").toFile();
                        File[] files2 = internalGranuleFolder.listFiles();
                        if (files2 != null) {
                            for (File imgData : files2) {
                                if (imgData.isDirectory()) {
                                    if (imgData.getName().equals("R20m")) {
                                        return DecodeQualification.INTENDED;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return decodeQualification;
    }
}
