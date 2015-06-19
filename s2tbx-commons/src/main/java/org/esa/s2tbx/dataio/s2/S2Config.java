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

package org.esa.s2tbx.dataio.s2;

import org.esa.s2tbx.openjpeg.OpenJpegUtils;
import org.esa.snap.framework.datamodel.ProductData;

import java.awt.image.DataBuffer;

/**
 * @author Norman Fomferra
 */
public interface S2Config {
    boolean DEBUG = Boolean.getBoolean("org.esa.s2tbx.dataio.s2.l1c.S2Config.DEBUG");
    boolean NODUMP = Boolean.getBoolean("org.esa.s2tbx.dataio.s2.l1c.S2Config.NODUMP");

    /**
     * Path to "opj_decompress" executable from OpenJPEG 2.1.0 package
     */

    // fixme parametrize log levels
    String LOG_JPEG = DEBUG ? "INFO" : "FINEST";
    String LOG_SCENE = DEBUG ? "INFO" : "FINEST";
    String LOG_DEBUG = DEBUG ? "WARNING" : "FINEST";

    String OPJ_DECOMPRESSOR_EXE = OpenJpegUtils.getSafeDecompressor();
    String OPJ_INFO_EXE = OpenJpegUtils.getSafeInfoExtractor();

    int DEFAULT_JAI_TILE_SIZE = 512;

    int SAMPLE_PRODUCT_DATA_TYPE = ProductData.TYPE_UINT16;
    int SAMPLE_DATA_BUFFER_TYPE = DataBuffer.TYPE_USHORT;
    int SAMPLE_BYTE_COUNT = 2;

    short FILL_CODE_NO_FILE = DEBUG ? (short) 1000 : 0;
    short FILL_CODE_NO_INTERSECTION = DEBUG ? (short) 1 : 0;
    short FILL_CODE_OUT_OF_X_BOUNDS = DEBUG ? (short) 2 : 0;
    short FILL_CODE_OUT_OF_Y_BOUNDS = DEBUG ? (short) 3 : 0;
    short FILL_CODE_MOSAIC_BG = DEBUG ? (short) 4 : 0;

    short RAW_NO_DATA_THRESHOLD = DEBUG ? (short) 4 : (short) 1;

    String MTD_EXT = ".xml";
}
