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

package org.esa.s2tbx.dataio.s2.l2a;

import https.psd_12_sentinel2_eo_esa_int.psd.s2_pdi_level_2a_tile_metadata.Level2A_Tile;
import https.psd_12_sentinel2_eo_esa_int.psd.user_product_level_2a.Level2A_User_Product;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.esa.s2tbx.dataio.Utils;
import org.esa.s2tbx.dataio.s2.l2a.filepatterns.S2L2aDatastripDirFilename;
import org.esa.s2tbx.dataio.s2.l2a.filepatterns.S2L2aDatastripFilename;
import org.esa.s2tbx.dataio.s2.l2a.filepatterns.S2L2aGranuleDirFilename;
import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.logging.BeamLogManager;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the Sentinel-2 MSI L1C XML metadata header file.
 * <p>
 * Note: No data interpretation is done in this class, it is intended to serve the pure metadata content only.
 *
 * @author Norman Fomferra
 */
public class L2aMetadata {

    static Element NULL_ELEM = new Element("null") {
    };


    private MetadataElement metadataElement;
    protected Logger logger = SystemUtils.LOG;


    static class Tile {
        String id;
        String horizontalCsName;
        String horizontalCsCode;
        TileGeometry tileGeometry10M;
        TileGeometry tileGeometry20M;
        TileGeometry tileGeometry60M;
        AnglesGrid sunAnglesGrid;
        AnglesGrid[] viewingIncidenceAnglesGrids;

        public static enum idGeom {G10M, G20M, G60M}

        ;

        public Tile(String id) {
            this.id = id;
            tileGeometry10M = new TileGeometry();
            tileGeometry20M = new TileGeometry();
            tileGeometry60M = new TileGeometry();
        }

        public TileGeometry getGeometry(idGeom index) {
            switch (index) {
                case G10M:
                    return tileGeometry10M;
                case G20M:
                    return tileGeometry20M;
                case G60M:
                    return tileGeometry60M;
                default:
                    throw new IllegalStateException();
            }
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    static class AnglesGrid {
        int bandId;
        int detectorId;
        float[][] zenith;
        float[][] azimuth;

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    static class TileGeometry {
        int numRows;
        int numCols;
        double upperLeftX;
        double upperLeftY;
        double xDim;
        double yDim;

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    static class ReflectanceConversion {
        double u;
        /**
         * Unit: W/m²/µm
         */
        double[] solarIrradiances;

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    static class ProductCharacteristics {
        String spacecraft;
        String datasetProductionDate;
        String processingLevel;
        SpectralInformation[] bandInformations;

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    static class SpectralInformation {
        int bandId;
        String physicalBand;
        int resolution;
        double wavelenghtMin;
        double wavelenghtMax;
        double wavelenghtCentral;
        double spectralResponseStep;
        double[] spectralResponseValues;

        public SpectralInformation() {
            spectralResponseValues = new double[]{};
        }

        public SpectralInformation(String physicalBand, int bandId, int resolution)
        {
            this.physicalBand = physicalBand;
            this.bandId = bandId;
            this.resolution = resolution;
            spectralResponseValues = new double[]{};
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    static class QuicklookDescriptor {
        int imageNCols;
        int imageNRows;
        Histogram[] histogramList;

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    static class Histogram {
        public int bandId;
        int[] values;
        int step;
        double min;
        double max;
        double mean;
        double stdDev;

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    private List<Tile> tileList;
    private Collection<ImageInfo> imageList;
    private ProductCharacteristics productCharacteristics;
    private JAXBContext context;
    private Unmarshaller unmarshaller;

    public static L2aMetadata parseHeader(File file) throws JDOMException, IOException {
        return new L2aMetadata(new FileInputStream(file), file, file.getParent());
    }

    public List<Tile> getTileList() {
        return tileList;
    }

    public Collection<ImageInfo> getImageList() {
        return imageList;
    }

    public ProductCharacteristics getProductCharacteristics() {
        return productCharacteristics;
    }


    public MetadataElement getMetadataElement() {
        return metadataElement;
    }

    private L2aMetadata(InputStream stream, File file, String parent) throws DataConversionException {
        try {
            context = L2aMetadataProc.getJaxbContext();
            unmarshaller = context.createUnmarshaller();

            Object ob = unmarshaller.unmarshal(stream);
            Object casted = ((JAXBElement) ob).getValue();

            if(casted instanceof Level2A_User_Product)
            {
                initProduct(stream, file, parent, casted);
            }
            else
            {
                initTile(stream, file, parent, casted);
            }
        } catch (JAXBException e) {
            logger.severe(Utils.getStackTrace(e));
        } catch (FileNotFoundException e) {
            logger.severe(Utils.getStackTrace(e));
        } catch (JDOMException e) {
            logger.severe(Utils.getStackTrace(e));
        } catch (IOException e) {
            logger.severe(Utils.getStackTrace(e));
        }
    }

    private void initProduct(InputStream stream, File file, String parent, Object casted) throws IOException, JAXBException, JDOMException {
        Level2A_User_Product product = (Level2A_User_Product) casted;
        productCharacteristics = L2aMetadataProc.getProductOrganization(product);

        Collection<String> tileNames = L2aMetadataProc.getTiles(product);
        imageList = L2aMetadataProc.getImages(product);
        List<File> fullTileNamesList = new ArrayList<File>();

        tileList = new ArrayList<Tile>();

        for (String granuleName : tileNames) {
            S2L2aGranuleDirFilename aGranuleDir = S2L2aGranuleDirFilename.create(granuleName);
            String theName = aGranuleDir.getMetadataFilename().name;

            File nestedGranuleMetadata = new File(parent, "GRANULE" + File.separator + granuleName + File.separator + theName);
            if (nestedGranuleMetadata.exists()) {
                fullTileNamesList.add(nestedGranuleMetadata);
            } else {
                String errorMessage = "Corrupted product: the file for the granule " + granuleName + " is missing";
                logger.log(Level.WARNING, errorMessage);
            }
        }

        for (File aGranuleMetadataFile : fullTileNamesList) {
            Object ob = unmarshaller.unmarshal(new FileInputStream(aGranuleMetadataFile));
            Object tmp = ((JAXBElement) ob).getValue();
            Level2A_Tile aTile = (Level2A_Tile) tmp;

            Map<Integer, TileGeometry> geoms = L2aMetadataProc.getTileGeometries(aTile);

            Tile t = new Tile(aTile.getGeneral_Info().getTILE_ID_2A().getValue());
            t.horizontalCsCode = aTile.getGeometric_Info().getTile_Geocoding().getHORIZONTAL_CS_CODE();
            t.horizontalCsName = aTile.getGeometric_Info().getTile_Geocoding().getHORIZONTAL_CS_NAME();

            t.tileGeometry10M = geoms.get(10);
            t.tileGeometry20M = geoms.get(20);
            t.tileGeometry60M = geoms.get(60);

            t.sunAnglesGrid = L2aMetadataProc.getSunGrid(aTile);
            t.viewingIncidenceAnglesGrids = L2aMetadataProc.getAnglesGrid(aTile);

            tileList.add(t);
        }

        S2L2aDatastripFilename stripName = L2aMetadataProc.getDatastrip(product);
        S2L2aDatastripDirFilename dirStripName = L2aMetadataProc.getDatastripDir(product);

        File dataStripMetadata = new File(parent, "DATASTRIP" + File.separator + dirStripName.name + File.separator + stripName.name);

        metadataElement = new MetadataElement("root");
        MetadataElement userProduct = parseAll(new SAXBuilder().build(file).getRootElement());
        MetadataElement dataStrip = parseAll(new SAXBuilder().build(dataStripMetadata).getRootElement());
        metadataElement.addElement(userProduct);
        metadataElement.addElement(dataStrip);
        MetadataElement granulesMetaData = new MetadataElement("Granules");

        for (File aGranuleMetadataFile : fullTileNamesList) {
            MetadataElement aGranule = parseAll(new SAXBuilder().build(aGranuleMetadataFile).getRootElement());
            granulesMetaData.addElement(aGranule);
        }

        metadataElement.addElement(granulesMetaData);
    }

    private void initTile(InputStream stream, File file, String parent, Object casted) throws IOException, JAXBException, JDOMException {
        Level2A_Tile aTile = (Level2A_Tile) casted;

        {
            Map<Integer, TileGeometry> geoms = L2aMetadataProc.getTileGeometries(aTile);

            Tile t = new Tile(aTile.getGeneral_Info().getTILE_ID_2A().getValue());
            t.horizontalCsCode = aTile.getGeometric_Info().getTile_Geocoding().getHORIZONTAL_CS_CODE();
            t.horizontalCsName = aTile.getGeometric_Info().getTile_Geocoding().getHORIZONTAL_CS_NAME();

            t.tileGeometry10M = geoms.get(10);
            t.tileGeometry20M = geoms.get(20);
            t.tileGeometry60M = geoms.get(60);

            t.sunAnglesGrid = L2aMetadataProc.getSunGrid(aTile);
            t.viewingIncidenceAnglesGrids = L2aMetadataProc.getAnglesGrid(aTile);

            tileList.add(t);
        }
    }

    private MetadataElement parseAll(Element parent) {
        return parseTree(parent, null, new HashSet<String>(Arrays.asList("Viewing_Incidence_Angles_Grids", "Sun_Angles_Grid")));
    }

    private MetadataElement parseTree(Element element, MetadataElement mdParent, Set<String> excludes) {

        MetadataElement mdElement = new MetadataElement(element.getName());

        List attributes = element.getAttributes();
        for (Object a : attributes) {
            Attribute attribute = (Attribute) a;
            MetadataAttribute mdAttribute = new MetadataAttribute(attribute.getName().toUpperCase(), ProductData.createInstance(attribute.getValue()), true);
            mdElement.addAttribute(mdAttribute);
        }

        for (Object c : element.getChildren()) {
            Element child = (Element) c;
            String childName = child.getName();
            String childValue = child.getValue();
            if (!excludes.contains(childName)) {
                if (childValue != null && !childValue.isEmpty() && childName.equals(childName.toUpperCase())) {
                    MetadataAttribute mdAttribute = new MetadataAttribute(childName, ProductData.createInstance(childValue), true);
                    String unit = child.getAttributeValue("unit");
                    if (unit != null) {
                        mdAttribute.setUnit(unit);
                    }
                    mdElement.addAttribute(mdAttribute);
                } else {
                    parseTree(child, mdElement, excludes);
                }
            }
        }

        if (mdParent != null) {
            mdParent.addElement(mdElement);
        }

        return mdElement;
    }


    private static Element getChild(Element parent, String... path) {
        Element child = parent;
        if (child == null) {
            return NULL_ELEM;
        }
        for (String name : path) {
            child = child.getChild(name);
            if (child == null) {
                return NULL_ELEM;
            }
        }
        return child;
    }

    private static double getElementValueDouble(String elementValue, String name) throws DataConversionException {
        try {
            return Double.parseDouble(elementValue);
        } catch (NumberFormatException e) {
            throw new DataConversionException(name, "double");
        }
    }
}
