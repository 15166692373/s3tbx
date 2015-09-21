/*
 *
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


import com.vividsolutions.jts.geom.Coordinate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.ProductData;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the Sentinel-2 MSI XML metadata header file.
 * <p>
 * Note: No data interpretation is done in this class, it is intended to serve the pure metadata content only.
 *
 * @author Nicolas Ducoin
 */
public abstract class S2Metadata {

    private List<MetadataElement> metadataElements;

    private List<Tile> tileList;

    private S2Config config;

    private Unmarshaller unmarshaller;

    private String psdString;

    private ProductCharacteristics productCharacteristics;


    public S2Metadata(S2Config config, JAXBContext context, String psdString) throws JAXBException {
        this.config = config;
        this.unmarshaller = context.createUnmarshaller();
        this.psdString = psdString;
        metadataElements = new ArrayList<>();
    }

    public S2Config getConfig() {
        return config;
    }

    public List<MetadataElement> getMetadataElements() {
        return metadataElements;
    }

    public List<Tile> getTileList() {
        return tileList;
    }

    public void resetTileList() {
        tileList = new ArrayList<>();
    }

    public void addTileToList(Tile tile) {
        tileList.add(tile);
    }

    public ProductCharacteristics getProductCharacteristics() {
        return productCharacteristics;
    }

    public void setProductCharacteristics(ProductCharacteristics productCharacteristics) {
        this.productCharacteristics = productCharacteristics;
    }

    protected Object updateAndUnmarshal(InputStream xmlStream) throws IOException, JAXBException {
        InputStream updatedStream = changePSDIfRequired(xmlStream, psdString);
        Object ob = unmarshaller.unmarshal(updatedStream);
        return ((JAXBElement) ob).getValue();
    }

    /**
     * from the input stream, replace the psd number in the header to allow jaxb to find
     * xsd files. This allows to open product with psd different from the reference one
     * for small changes.
     */
    static InputStream changePSDIfRequired(InputStream xmlStream, String psdNumber) throws IOException {
        InputStream updatedXmlStream;

        String xmlStreamAsString = IOUtils.toString(xmlStream);

        final String psd13String = "psd-" + psdNumber + ".sentinel2.eo.esa.int";
        if (!xmlStreamAsString.contains(psd13String)) {
            String regex="psd-\\d{2,}.sentinel2.eo.esa.int";
            String updatedXmlStreamAsString =
                    xmlStreamAsString.replaceAll(
                            regex, psd13String);
            updatedXmlStream = IOUtils.toInputStream(updatedXmlStreamAsString, "UTF-8");
        } else {
            updatedXmlStream = IOUtils.toInputStream(xmlStreamAsString, "UTF-8");
        }

        return updatedXmlStream;
    }

    protected MetadataElement parseAll(Element parent) {
        return parseTree(parent, null, new HashSet<>(Arrays.asList("Viewing_Incidence_Angles_Grids", "Sun_Angles_Grid")));
    }

    protected MetadataElement parseTree(Element element, MetadataElement mdParent, Set<String> excludes) {

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



    public static class Tile {
        private String id;
        private String detectorId;
        private String horizontalCsName;
        private String horizontalCsCode;
        private TileGeometry tileGeometry10M;
        private TileGeometry tileGeometry20M;
        private TileGeometry tileGeometry60M;
        private AnglesGrid sunAnglesGrid;
        private AnglesGrid[] viewingIncidenceAnglesGrids;
        private MaskFilename[] maskFilenames;
        public List<Coordinate> corners;


        public Tile(String id) {
            this.id = id;
            tileGeometry10M = new TileGeometry();
            tileGeometry20M = new TileGeometry();
            tileGeometry60M = new TileGeometry();
        }

        public Tile(String id, String detectorId) {
            this.id = id;
            this.detectorId = detectorId;
            tileGeometry10M = new TileGeometry();
            tileGeometry20M = new TileGeometry();
            tileGeometry60M = new TileGeometry();
        }

        public TileGeometry getGeometry(S2SpatialResolution spacialResolution) {
            switch (spacialResolution.resolution) {
                case 10:
                    return tileGeometry10M;
                case 20:
                    return tileGeometry20M;
                case 60:
                    return tileGeometry60M;
                default:
                    throw new IllegalStateException();
            }
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getHorizontalCsName() {
            return horizontalCsName;
        }

        public void setHorizontalCsName(String horizontalCsName) {
            this.horizontalCsName = horizontalCsName;
        }

        public String getHorizontalCsCode() {
            return horizontalCsCode;
        }

        public void setHorizontalCsCode(String horizontalCsCode) {
            this.horizontalCsCode = horizontalCsCode;
        }

        public TileGeometry getTileGeometry10M() {
            return tileGeometry10M;
        }

        public void setTileGeometry10M(TileGeometry tileGeometry10M) {
            this.tileGeometry10M = tileGeometry10M;
        }

        public TileGeometry getTileGeometry20M() {
            return tileGeometry20M;
        }

        public void setTileGeometry20M(TileGeometry tileGeometry20M) {
            this.tileGeometry20M = tileGeometry20M;
        }

        public TileGeometry getTileGeometry60M() {
            return tileGeometry60M;
        }

        public void setTileGeometry60M(TileGeometry tileGeometry60M) {
            this.tileGeometry60M = tileGeometry60M;
        }

        public AnglesGrid getSunAnglesGrid() {
            return sunAnglesGrid;
        }

        public void setSunAnglesGrid(AnglesGrid sunAnglesGrid) {
            this.sunAnglesGrid = sunAnglesGrid;
        }

        public AnglesGrid[] getViewingIncidenceAnglesGrids() {
            return viewingIncidenceAnglesGrids;
        }

        public void setViewingIncidenceAnglesGrids(AnglesGrid[] viewingIncidenceAnglesGrids) {
            this.viewingIncidenceAnglesGrids = viewingIncidenceAnglesGrids;
        }

        public String getDetectorId() {
            return detectorId;
        }

        public void setDetectorId(String detectorId) {
            this.detectorId = detectorId;
        }

        public MaskFilename[] getMaskFilenames() {
            return maskFilenames;
        }

        public void setMaskFilenames(MaskFilename[] maskFilenames) {
            this.maskFilenames = maskFilenames;
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class MaskFilename {
        String bandId;
        String type;
        File name;

        public MaskFilename(String bandId, String type, File name) {
            this.bandId = bandId;
            this.type = type;
            this.name = name;
        }

        public String getBandId() {
            return bandId;
        }

        public void setBandId(String bandId) {
            this.bandId = bandId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public File getName() {
            return name;
        }

        public void setName(File name) {
            this.name = name;
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class AnglesGrid {
        private int bandId;
        private int detectorId;
        private float[][] zenith;
        private float[][] azimuth;

        public int getBandId() {
            return bandId;
        }

        public void setBandId(int bandId) {
            this.bandId = bandId;
        }

        public int getDetectorId() {
            return detectorId;
        }

        public void setDetectorId(int detectorId) {
            this.detectorId = detectorId;
        }

        public float[][] getZenith() {
            return zenith;
        }

        public void setZenith(float[][] zenith) {
            this.zenith = zenith;
        }

        public float[][] getAzimuth() {
            return azimuth;
        }

        public void setAzimuth(float[][] azimuth) {
            this.azimuth = azimuth;
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class TileGeometry {
        private int numRows;
        private int numCols;
        private double upperLeftX;
        private double upperLeftY;
        private double xDim;
        private double yDim;
        private String detector;
        private Integer position;
        private int resolution;
        private int numRowsDetector;

        public int getNumRows() {
            return numRows;
        }

        public void setNumRows(int numRows) {
            this.numRows = numRows;
        }

        public int getNumCols() {
            return numCols;
        }

        public void setNumCols(int numCols) {
            this.numCols = numCols;
        }

        public double getUpperLeftX() {
            return upperLeftX;
        }

        public void setUpperLeftX(double upperLeftX) {
            this.upperLeftX = upperLeftX;
        }

        public double getUpperLeftY() {
            return upperLeftY;
        }

        public void setUpperLeftY(double upperLeftY) {
            this.upperLeftY = upperLeftY;
        }

        public double getxDim() {
            return xDim;
        }

        public void setxDim(double xDim) {
            this.xDim = xDim;
        }

        public double getyDim() {
            return yDim;
        }

        public void setyDim(double yDim) {
            this.yDim = yDim;
        }

        public String getDetector() {
            return detector;
        }

        public void setDetector(String detector) {
            this.detector = detector;
        }

        public Integer getPosition() {
            return position;
        }

        public void setPosition(Integer position) {
            this.position = position;
        }

        public int getResolution() {
            return resolution;
        }

        public void setResolution(int resolution) {
            this.resolution = resolution;
        }

        public int getNumRowsDetector() {
            return numRowsDetector;
        }

        public void setNumRowsDetector(int numRowsDetector) {
            this.numRowsDetector = numRowsDetector;
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static class ProductCharacteristics {
        private String spacecraft;
        private String datasetProductionDate;
        private String processingLevel;
        private S2SpectralInformation[] bandInformations;
        private String metaDataLevel;

        public String getSpacecraft() {
            return spacecraft;
        }

        public void setSpacecraft(String spacecraft) {
            this.spacecraft = spacecraft;
        }

        public String getDatasetProductionDate() {
            return datasetProductionDate;
        }

        public void setDatasetProductionDate(String datasetProductionDate) {
            this.datasetProductionDate = datasetProductionDate;
        }

        public String getProcessingLevel() {
            return processingLevel;
        }

        public void setProcessingLevel(String processingLevel) {
            this.processingLevel = processingLevel;
        }

        public S2SpectralInformation[] getBandInformations() {
            return bandInformations;
        }

        public void setBandInformations(S2SpectralInformation[] bandInformations) {
            this.bandInformations = bandInformations;
        }

        public String getMetaDataLevel() {
            return metaDataLevel;
        }

        public void setMetaDataLevel(String metaDataLevel) {
            this.metaDataLevel = metaDataLevel;
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }


    }
}
