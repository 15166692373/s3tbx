/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.ceos.alos2;

import org.esa.s1tbx.io.ceos.CEOSConstants;

/**
 * Several constants used for reading Palsar products.
 */
public class Alos2Constants implements CEOSConstants {

    private final static String[] FORMAT_NAMES = new String[]{"ALOS 2 CEOS"};
    private final static String[] FORMAT_FILE_EXTENSIONS = new String[]{""};
    private final static String PLUGIN_DESCRIPTION = "ALOS 2 CEOS Products";      /*I18N*/

    private final static String[] VOLUME_FILE_PREFIX = {"VOL-ALOS2"};
    private static final String[] LEADER_FILE_PREFIX = {"LE"};
    private static final String[] IMAGE_FILE_PREFIX = {"IMG"};
    private static final String[] TRAILER_FILE_PREFIX = {"TR"};

    final static String MISSION = "ALOS2";

    final static String PRODUCT_DESCRIPTION_PREFIX = "ALOS 2 product ";

    private final static int MINIMUM_FILES = 4;    // 4 image files + leader file + volume file + trailer file

    public String[] getVolumeFilePrefix() {
        return VOLUME_FILE_PREFIX;
    }

    public String[] getLeaderFilePrefix() {
        return LEADER_FILE_PREFIX;
    }

    public String[] getImageFilePrefix() {
        return IMAGE_FILE_PREFIX;
    }

    public String[] getTrailerFilePrefix() {
        return TRAILER_FILE_PREFIX;
    }

    public int getMinimumNumFiles() {
        return MINIMUM_FILES;
    }

    public String getPluginDescription() {
        return PLUGIN_DESCRIPTION;
    }

    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    public String[] getForamtFileExtensions() {
        return FORMAT_FILE_EXTENSIONS;
    }

    public String getMission() {
        return MISSION;
    }
}
