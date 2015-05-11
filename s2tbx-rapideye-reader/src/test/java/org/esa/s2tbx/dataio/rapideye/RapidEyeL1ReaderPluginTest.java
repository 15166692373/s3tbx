/*
 * Copyright (C) 2014-2015 CS SI
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
 *  with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s2tbx.dataio.rapideye;

import org.esa.s2tbx.dataio.rapideye.metadata.RapidEyeConstants;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductIOPlugInManager;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.util.io.BeamFileFilter;
import org.esa.snap.utils.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Ramona Manda
 */
public class RapidEyeL1ReaderPluginTest {

    private RapidEyeL1ReaderPlugin plugIn;

    @Before
    public void setup() {
        plugIn = new RapidEyeL1ReaderPlugin();
    }

    @Test
    public void spotTake5ReaderIsLoaded() {
        final Iterator iterator = ProductIOPlugInManager.getInstance().getReaderPlugIns(RapidEyeConstants.L1_FORMAT_NAMES[0]);
        final ProductReaderPlugIn plugIn = (ProductReaderPlugIn) iterator.next();
        assertEquals(RapidEyeL1ReaderPlugin.class, plugIn.getClass());
    }

    @Test
    public void testDecodeQualificationForXML() throws IOException {
        Date startDate = Calendar.getInstance().getTime();
        DecodeQualification decodeQualification = plugIn.getDecodeQualification(TestUtil.getTestFile("dimap/test_ST4_MT.xml"));
        assertEquals(DecodeQualification.UNABLE, decodeQualification);
        decodeQualification = plugIn.getDecodeQualification(TestUtil.getTestFile("Demo03_1B/2009-04-16T104920_RE4_1B-NAC_3436599_84303_metadata.xml"));
        assertEquals(DecodeQualification.INTENDED, decodeQualification);
        Date endDate = Calendar.getInstance().getTime();
        assertTrue("The decoding time for the file is too big!", (endDate.getTime() - startDate.getTime()) / 1000 < 30);//30 sec
    }

    @Test
    public void testFileExtensions() {
        final String[] fileExtensions = plugIn.getDefaultFileExtensions();
        assertNotNull(fileExtensions);
        final List<String> extensionList = Arrays.asList(fileExtensions);
        assertEquals(4, extensionList.size());
        assertEquals(".xml", extensionList.get(0));
        assertEquals(".XML", extensionList.get(1));
        assertEquals(".zip", extensionList.get(2));
        assertEquals(".ZIP", extensionList.get(3));
    }

    @Test
    public void testFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();
        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("RapidEyeNITF", formatNames[0]);
    }

    @Test
    public void testInputTypes() {
        final Class[] classes = plugIn.getInputTypes();
        assertNotNull(classes);
        assertEquals(2, classes.length);
        final List<Class> list = Arrays.asList(classes);
        assertEquals(true, list.contains(File.class));
        assertEquals(true, list.contains(String.class));
    }

    @Test
    public void testProductFileFilter() {
        final BeamFileFilter beamFileFilter = plugIn.getProductFileFilter();
        assertNotNull(beamFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(), beamFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], beamFileFilter.getFormatName());
        assertEquals(true, beamFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }

}
