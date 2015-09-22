/*
 *
 * Copyright (C) 2013-2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 * Copyright (C) 2014-2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s2tbx.dataio.s2.ortho;

import org.esa.s2tbx.dataio.s2.S2ProductReaderPlugIn;
import org.esa.s2tbx.dataio.s2.filepatterns.S2ProductFilename;
import org.esa.s2tbx.dataio.s2.l1c.Sentinel2L1CProductReader;
import org.esa.s2tbx.dataio.s2.l2a.Sentinel2L2AProductReader;
import org.esa.s2tbx.dataio.s2.ortho.filepatterns.S2OrthoGranuleMetadataFilename;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.datamodel.RGBImageProfile;
import org.esa.snap.framework.datamodel.RGBImageProfileManager;
import org.esa.snap.util.SystemUtils;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;

import static org.esa.s2tbx.dataio.s2.ortho.S2CRSHelper.*;

/**
 * @author Norman Fomferra
 */
public abstract class S2OrthoProductReaderPlugIn extends S2ProductReaderPlugIn {


    private static S2ProductCRSCache crsCache = new S2ProductCRSCache();

    // Product level: L1C, L2A...
    private String level = "";

    public S2OrthoProductReaderPlugIn() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI Natural Colors", new String[]{"B4", "B3", "B2"}));
        manager.addProfile(new RGBImageProfile("Sentinel 2 MSI False-color Infrared", new String[]{"B8", "B4", "B3"}));
    }

    protected String getLevel() {
        return level;
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        SystemUtils.LOG.fine("Getting decoders...");

        DecodeQualification decodeQualification = DecodeQualification.UNABLE;

        if (input instanceof File) {
            File file = (File) input;

            if (file.isFile()) {
                String fileName = file.getName();

                // first check it is a Sentinel-2 product
                Matcher matcher = PATTERN.matcher(fileName);
                if (matcher.matches()) {

                    // test for granule filename first as it is more restrictive
                    if (S2OrthoGranuleMetadataFilename.isGranuleFilename(fileName)) {
                        level = matcher.group(4).substring(0, 3);
                        S2OrthoGranuleMetadataFilename granuleMetadataFilename = S2OrthoGranuleMetadataFilename.create(fileName);
                        if (granuleMetadataFilename != null &&
                                (level.equals("L1C") ||
                                        (level.equals("L2A") && !this.getClass().equals(S2OrthoProductReaderPlugIn.class)))) {
                            String tileId = granuleMetadataFilename.tileNumber;
                            String epsg = tileIdentifierToEPSG(tileId);
                            if (getEPSG() != null && getEPSG().equalsIgnoreCase(epsg)) {
                                decodeQualification = DecodeQualification.INTENDED;
                            }
                        }
                    } else if (S2ProductFilename.isMetadataFilename(fileName)) {
                        level = matcher.group(4).substring(3);
                        S2ProductFilename productFilename = S2ProductFilename.create(fileName);
                        if (productFilename != null) {
                            if (level.equals("L1C") ||
                                    // no multi-resolution for L2A products
                                    (level.equals("L2A") &&
                                            (this instanceof S2OrthoProduct10MReaderPlugIn ||
                                                    this instanceof S2OrthoProduct20MReaderPlugIn ||
                                                    this instanceof S2OrthoProduct60MReaderPlugIn
                                            ))) {
                                crsCache.ensureIsCached(file.getAbsolutePath());
                                if (getEPSG() != null && crsCache.hasEPSG(file.getAbsolutePath(), getEPSG())) {
                                    decodeQualification = DecodeQualification.INTENDED;
                                }
                            }
                        }
                    }
                }
            }
        }

        return decodeQualification;
    }

    public abstract String getEPSG();


    @Override
    public ProductReader createReaderInstance() {
        if (level != null && level.equals("L2A")) {
            return new Sentinel2L2AProductReader(this, getEPSG());
        } else {
            return new Sentinel2L1CProductReader(this, getEPSG());
        }
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME + "-MultiRes-" + epsgToShortDisplayName(getEPSG())};
    }

    @Override
    public String getDescription(Locale locale) {
        return String.format("Sentinel-2 MSI %s - all resolutions - %s", getLevel(), epsgToDisplayName(getEPSG()));
    }
}
