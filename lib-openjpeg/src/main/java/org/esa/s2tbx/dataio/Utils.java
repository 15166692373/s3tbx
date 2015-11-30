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

package org.esa.s2tbx.dataio;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

public class Utils {

    public static String getStackTrace(Throwable tr) {
        StringWriter sw = new StringWriter();
        tr.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String GetShortPathName(String path) {
        if (!(new File(path).exists())) {
            return "";
        }

        int sizeBuffer = 2048;
        byte[] shortt = new byte[sizeBuffer];

        //Call CKernel32 interface to execute GetShortPathNameA method
        CKernel32.INSTANCE.GetShortPathNameA(path, shortt, sizeBuffer);
        return Native.toString(shortt);
    }

    public static String GetIterativeShortPathName(String path) {
        if (!(new File(path).exists())) {
            return "";
        }

        String firstTry = GetShortPathName(path);
        if (firstTry.length() != 0) {
            return firstTry;
        }

        int lenght = 0;
        String workingPath = path;
        while (lenght == 0) {
            workingPath = new File(workingPath).getParent();
            lenght = GetShortPathName(workingPath).length();
        }

        String[] shortenedFragments = GetShortPathName(workingPath).split(Pattern.quote(File.separator));
        String[] fragments = path.split(Pattern.quote(File.separator));
        // if the path did not split, we didn't have the system separator but '/'
        if(fragments.length == 1) {
            fragments = path.split(Pattern.quote("/"));
        }

        System.arraycopy(shortenedFragments, 0, fragments, 0, shortenedFragments.length);

        String complete = String.join(File.separator, fragments);
        String shortComplete = GetShortPathName(complete);

        if (shortComplete.length() != 0) {
            return shortComplete;
        }

        return GetIterativeShortPathName(complete);
    }

    public interface CKernel32 extends Library {

        CKernel32 INSTANCE = (CKernel32) Native.loadLibrary("kernel32", CKernel32.class);

        int GetShortPathNameA(String LongName, byte[] ShortName, int BufferCount);
    }

}
