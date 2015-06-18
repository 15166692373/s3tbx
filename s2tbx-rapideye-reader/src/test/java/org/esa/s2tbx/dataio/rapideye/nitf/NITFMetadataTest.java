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

package org.esa.s2tbx.dataio.rapideye.nitf;

import junit.framework.TestCase;
import org.esa.s2tbx.dataio.nitf.NITFMetadata;
import org.esa.s2tbx.dataio.nitf.NITFReaderWrapper;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.utils.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Ramona Manda
 */
public class NITFMetadataTest extends TestCase {
    private NITFMetadata metadata;

    @Before
    public void setUp() throws Exception {
        NITFReaderWrapper reader = new NITFReaderWrapper(TestUtil.getTestFile("2009-04-16T104920_RE4_1B-NAC_3436599_84303_band2.ntf"));
        metadata = reader.getMetadata();
    }

    @Test
    public void testGetMetadataRoot() throws Exception {
        assertNotNull(metadata.getMetadataRoot());
    }

    @Test
    public void testGetFileDate() throws Exception {
        Date expectedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2010-06-09 11:57:19");
        assertEquals(expectedDate.getTime(), metadata.getFileDate().getAsDate().getTime());
    }

    @Test
    public void testGetFileTitle() throws Exception {
        assertEquals("RE Image Data", metadata.getFileTitle());
    }

    @Test
    public void testGetNumImages() throws Exception {
        assertEquals(1, metadata.getNumImages());
    }

    @Test
    public void testGetNumBands() throws Exception {
        assertEquals(1, metadata.getNumBands());
    }

    @Test
    public void testGetWidth() throws Exception {
        assertEquals(11829, metadata.getWidth());
    }

    @Test
    public void testGetHeight() throws Exception {
        assertEquals(7422, metadata.getHeight());
    }

    @Test
    public void testGetDataType() throws Exception {
        assertEquals(ProductData.TYPE_UINT16, metadata.getDataType());
    }

    @Test
    public void testGetUnit() throws Exception {
        assertEquals("nm", metadata.getUnit());
    }

    @Test
    public void testGetWavelength() throws Exception {
        assertEquals(-1.0, metadata.getWavelength(), 0.001);
    }
    
}
