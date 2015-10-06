/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.landsat.geotiff;

import com.bc.ceres.core.VirtualDir;
import org.esa.s3tbx.dataio.landsat.tgz.VirtualDirTgz;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.util.StringUtils;
import org.esa.snap.util.io.FileUtils;
import org.esa.snap.util.io.SnapFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Thomas Storm
 */
public class LandsatGeotiffReaderPlugin implements ProductReaderPlugIn {

    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String[] FORMAT_NAMES = new String[]{"LandsatGeoTIFF"};
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".txt", ".TXT", ".gz"};
    private static final String READER_DESCRIPTION = "Landsat Data Products (GeoTIFF)";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        String filename = new File(input.toString()).getName();
        if (!isLandsatMSSFilename(filename) &&
                !isLandsat4Filename(filename) &&
                !isLandsat5Filename(filename) &&
                !isLandsat7Filename(filename) &&
                !isLandsat8Filename(filename) &&
                !isLandsat5LegacyFilename(filename) &&
                !isLandsat7LegacyFilename(filename)) {
            return DecodeQualification.UNABLE;
        }

        VirtualDir virtualDir;
        try {
            virtualDir = getInput(input);
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        return getDecodeQualification(virtualDir);

    }

    static DecodeQualification getDecodeQualification(VirtualDir virtualDir) {
        if (virtualDir == null) {
            return DecodeQualification.UNABLE;
        }

        String[] list;
        try {
            list = virtualDir.list("");
            if (list == null || list.length == 0) {
                return DecodeQualification.UNABLE;
            }
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        for (String fileName : list) {
            try {
                File file = virtualDir.getFile(fileName);
                if (isMetadataFile(file)) {
                    return DecodeQualification.INTENDED;
                }
            } catch (IOException ignore) {
                // file is broken, but be tolerant here
            }
        }
        // didn't find the expected metadata file
        return DecodeQualification.UNABLE;
    }

    static boolean isMetadataFile(File file) {
        if (!file.getName().toLowerCase().endsWith("_mtl.txt")) {
            return false;
        }
        BufferedReader reader = null;
        String line;
        try {
            reader = new BufferedReader(new FileReader(file));
            line = reader.readLine();
        } catch (IOException e) {
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
        return line != null && line.trim().matches("GROUP = L1_METADATA_FILE");
    }

    @Override
    public Class[] getInputTypes() {
        return READER_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new LandsatGeotiffReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return READER_DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], DEFAULT_FILE_EXTENSIONS, READER_DESCRIPTION);
    }

    static VirtualDir getInput(Object input) throws IOException {
        File inputFile = getFileInput(input);

        if (inputFile != null && inputFile.isFile() && !isCompressedFile(inputFile)) {
            final File absoluteFile = inputFile.getAbsoluteFile();
            inputFile = absoluteFile.getParentFile();
            if (inputFile == null) {
                throw new IOException("Unable to retrieve parent to file: " + absoluteFile.getAbsolutePath());
            }
        }

        VirtualDir virtualDir = VirtualDir.create(inputFile);
        if (virtualDir == null) {
            virtualDir = new VirtualDirTgz(inputFile);
        }
        return virtualDir;
    }

    static File getFileInput(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    static boolean isLandsatMSSFilename(String filename) {
        if (filename.matches("LM[1-5]\\d{13}\\w{3}\\d{2}_MTL.(txt|TXT)")) {
            return true;
        }
        return false;
    }

    static boolean isLandsat4Filename(String filename) {
        if (filename.matches("LT4\\d{13}\\w{3}\\d{2}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("LT4\\d{13}\\w{3}\\d{2}\\.tar\\.gz")) {
            return true;
        }
        return false;
    }

    static boolean isLandsat5Filename(String filename) {
        if (filename.matches("LT5\\d{13}.{3}\\d{2}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("LT5\\d{13}.{3}\\d{2}\\.tar\\.gz")) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isLandsat7Filename(String filename) {
        if (filename.matches("LE7\\d{13}.{3}\\d{2}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("LE7\\d{13}.{3}\\d{2}\\.tar\\.gz")) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isLandsat8Filename(String filename) {
        if (filename.matches("L[O,T,C]8\\d{13}.{3}\\d{2}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("L[O,T,C]8\\d{13}.{3}\\d{2}\\.tar\\.gz")) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isLandsat5LegacyFilename(String filename) {
        if (filename.matches("LT5\\d{13}.{3}\\d{2}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("L5\\d{6}_\\d{11}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("LT5\\d{13}.{3}\\d{2}\\.tar\\.gz")) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isLandsat7LegacyFilename(String filename) {
        if (filename.matches("LE7\\d{13}.{3}\\d{2}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("L7\\d{7}_\\d{11}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("LE7\\d{13}.{3}\\d{2}\\.tar\\.gz")) {
            return true;
        } else {
            return false;
        }
    }

    static boolean isCompressedFile(File file) {
        String extension = FileUtils.getExtension(file);
        if (StringUtils.isNullOrEmpty(extension)) {
            return false;
        }

        extension = extension.toLowerCase();

        return extension.contains("zip")
                || extension.contains("tar")
                || extension.contains("tgz")
                || extension.contains("gz");
    }
}
