//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.7 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.07.07 à 03:07:45 PM CEST 
//


package _int.esa.gs2.dico._1_0.pdgs.dimap;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour AN_IMAGE_DATA_INFO_DSL1A complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="AN_IMAGE_DATA_INFO_DSL1A">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Granules_Information">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Detector_List">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="Detector" maxOccurs="12">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                     &lt;sequence>
 *                                       &lt;element name="Granule_List">
 *                                         &lt;complexType>
 *                                           &lt;complexContent>
 *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                               &lt;sequence>
 *                                                 &lt;element name="Granule" maxOccurs="unbounded">
 *                                                   &lt;complexType>
 *                                                     &lt;complexContent>
 *                                                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                                         &lt;sequence>
 *                                                           &lt;element name="POSITION">
 *                                                             &lt;simpleType>
 *                                                               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
 *                                                                 &lt;minInclusive value="1"/>
 *                                                               &lt;/restriction>
 *                                                             &lt;/simpleType>
 *                                                           &lt;/element>
 *                                                         &lt;/sequence>
 *                                                         &lt;attribute name="granuleId" use="required" type="{http://gs2.esa.int/DICO/1.0/DataAccess/item/}GRANULE_ID" />
 *                                                       &lt;/restriction>
 *                                                     &lt;/complexContent>
 *                                                   &lt;/complexType>
 *                                                 &lt;/element>
 *                                               &lt;/sequence>
 *                                             &lt;/restriction>
 *                                           &lt;/complexContent>
 *                                         &lt;/complexType>
 *                                       &lt;/element>
 *                                     &lt;/sequence>
 *                                     &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_DETECTOR_NUMBER" />
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                           &lt;/sequence>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *                 &lt;attribute name="metadataLevel" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}metadataLevel" fixed="Standard" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Sensor_Configuration" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_SENSOR_CONFIGURATION"/>
 *         &lt;element name="Geometric_Header_List">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;extension base="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_GEOMETRIC_HEADER_LIST_EXPERTISE">
 *                 &lt;attribute name="metadataLevel" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}metadataLevel" fixed="Expertise" />
 *               &lt;/extension>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Radiometric_Info">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;extension base="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_RADIOMETRIC_DATA_L1A">
 *                 &lt;attribute name="metadataLevel" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}metadataLevel" fixed="Standard" />
 *               &lt;/extension>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="List_Fake_Decompr_Source_Frames" maxOccurs="13" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="FIRST" type="{http://www.w3.org/2001/XMLSchema}unsignedInt"/>
 *                   &lt;element name="NUMBER_SOURCE" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="metadataLevel" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}metadataLevel" fixed="Standard" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Product_Compression" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="COMPRESSION">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                         &lt;enumeration value="NONE"/>
 *                         &lt;enumeration value="LOSSY"/>
 *                         &lt;enumeration value="LOSSLESS"/>
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *                 &lt;attribute name="metadataLevel" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}metadataLevel" fixed="Standard" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AN_IMAGE_DATA_INFO_DSL1A", propOrder = {
    "granulesInformation",
    "sensorConfiguration",
    "geometricHeaderList",
    "radiometricInfo",
    "listFakeDecomprSourceFrames",
    "productCompression"
})
public class ANIMAGEDATAINFODSL1A {

    @XmlElement(name = "Granules_Information", required = true)
    protected ANIMAGEDATAINFODSL1A.GranulesInformation granulesInformation;
    @XmlElement(name = "Sensor_Configuration", required = true)
    protected ASENSORCONFIGURATION sensorConfiguration;
    @XmlElement(name = "Geometric_Header_List", required = true)
    protected ANIMAGEDATAINFODSL1A.GeometricHeaderList geometricHeaderList;
    @XmlElement(name = "Radiometric_Info", required = true)
    protected ANIMAGEDATAINFODSL1A.RadiometricInfo radiometricInfo;
    @XmlElement(name = "List_Fake_Decompr_Source_Frames")
    protected List<ANIMAGEDATAINFODSL1A.ListFakeDecomprSourceFrames> listFakeDecomprSourceFrames;
    @XmlElement(name = "Product_Compression")
    protected ANIMAGEDATAINFODSL1A.ProductCompression productCompression;

    /**
     * Obtient la valeur de la propriété granulesInformation.
     * 
     * @return
     *     possible object is
     *     {@link ANIMAGEDATAINFODSL1A.GranulesInformation }
     *     
     */
    public ANIMAGEDATAINFODSL1A.GranulesInformation getGranulesInformation() {
        return granulesInformation;
    }

    /**
     * Définit la valeur de la propriété granulesInformation.
     * 
     * @param value
     *     allowed object is
     *     {@link ANIMAGEDATAINFODSL1A.GranulesInformation }
     *     
     */
    public void setGranulesInformation(ANIMAGEDATAINFODSL1A.GranulesInformation value) {
        this.granulesInformation = value;
    }

    /**
     * Obtient la valeur de la propriété sensorConfiguration.
     * 
     * @return
     *     possible object is
     *     {@link ASENSORCONFIGURATION }
     *     
     */
    public ASENSORCONFIGURATION getSensorConfiguration() {
        return sensorConfiguration;
    }

    /**
     * Définit la valeur de la propriété sensorConfiguration.
     * 
     * @param value
     *     allowed object is
     *     {@link ASENSORCONFIGURATION }
     *     
     */
    public void setSensorConfiguration(ASENSORCONFIGURATION value) {
        this.sensorConfiguration = value;
    }

    /**
     * Obtient la valeur de la propriété geometricHeaderList.
     * 
     * @return
     *     possible object is
     *     {@link ANIMAGEDATAINFODSL1A.GeometricHeaderList }
     *     
     */
    public ANIMAGEDATAINFODSL1A.GeometricHeaderList getGeometricHeaderList() {
        return geometricHeaderList;
    }

    /**
     * Définit la valeur de la propriété geometricHeaderList.
     * 
     * @param value
     *     allowed object is
     *     {@link ANIMAGEDATAINFODSL1A.GeometricHeaderList }
     *     
     */
    public void setGeometricHeaderList(ANIMAGEDATAINFODSL1A.GeometricHeaderList value) {
        this.geometricHeaderList = value;
    }

    /**
     * Obtient la valeur de la propriété radiometricInfo.
     * 
     * @return
     *     possible object is
     *     {@link ANIMAGEDATAINFODSL1A.RadiometricInfo }
     *     
     */
    public ANIMAGEDATAINFODSL1A.RadiometricInfo getRadiometricInfo() {
        return radiometricInfo;
    }

    /**
     * Définit la valeur de la propriété radiometricInfo.
     * 
     * @param value
     *     allowed object is
     *     {@link ANIMAGEDATAINFODSL1A.RadiometricInfo }
     *     
     */
    public void setRadiometricInfo(ANIMAGEDATAINFODSL1A.RadiometricInfo value) {
        this.radiometricInfo = value;
    }

    /**
     * Gets the value of the listFakeDecomprSourceFrames property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the listFakeDecomprSourceFrames property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getListFakeDecomprSourceFrames().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ANIMAGEDATAINFODSL1A.ListFakeDecomprSourceFrames }
     * 
     * 
     */
    public List<ANIMAGEDATAINFODSL1A.ListFakeDecomprSourceFrames> getListFakeDecomprSourceFrames() {
        if (listFakeDecomprSourceFrames == null) {
            listFakeDecomprSourceFrames = new ArrayList<ANIMAGEDATAINFODSL1A.ListFakeDecomprSourceFrames>();
        }
        return this.listFakeDecomprSourceFrames;
    }

    /**
     * Obtient la valeur de la propriété productCompression.
     * 
     * @return
     *     possible object is
     *     {@link ANIMAGEDATAINFODSL1A.ProductCompression }
     *     
     */
    public ANIMAGEDATAINFODSL1A.ProductCompression getProductCompression() {
        return productCompression;
    }

    /**
     * Définit la valeur de la propriété productCompression.
     * 
     * @param value
     *     allowed object is
     *     {@link ANIMAGEDATAINFODSL1A.ProductCompression }
     *     
     */
    public void setProductCompression(ANIMAGEDATAINFODSL1A.ProductCompression value) {
        this.productCompression = value;
    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;extension base="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_GEOMETRIC_HEADER_LIST_EXPERTISE">
     *       &lt;attribute name="metadataLevel" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}metadataLevel" fixed="Expertise" />
     *     &lt;/extension>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class GeometricHeaderList
        extends AGEOMETRICHEADERLISTEXPERTISE
    {

        @XmlAttribute(name = "metadataLevel")
        protected String metadataLevel;

        /**
         * Obtient la valeur de la propriété metadataLevel.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getMetadataLevel() {
            if (metadataLevel == null) {
                return "Expertise";
            } else {
                return metadataLevel;
            }
        }

        /**
         * Définit la valeur de la propriété metadataLevel.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setMetadataLevel(String value) {
            this.metadataLevel = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Detector_List">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element name="Detector" maxOccurs="12">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                           &lt;sequence>
     *                             &lt;element name="Granule_List">
     *                               &lt;complexType>
     *                                 &lt;complexContent>
     *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                     &lt;sequence>
     *                                       &lt;element name="Granule" maxOccurs="unbounded">
     *                                         &lt;complexType>
     *                                           &lt;complexContent>
     *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                               &lt;sequence>
     *                                                 &lt;element name="POSITION">
     *                                                   &lt;simpleType>
     *                                                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
     *                                                       &lt;minInclusive value="1"/>
     *                                                     &lt;/restriction>
     *                                                   &lt;/simpleType>
     *                                                 &lt;/element>
     *                                               &lt;/sequence>
     *                                               &lt;attribute name="granuleId" use="required" type="{http://gs2.esa.int/DICO/1.0/DataAccess/item/}GRANULE_ID" />
     *                                             &lt;/restriction>
     *                                           &lt;/complexContent>
     *                                         &lt;/complexType>
     *                                       &lt;/element>
     *                                     &lt;/sequence>
     *                                   &lt;/restriction>
     *                                 &lt;/complexContent>
     *                               &lt;/complexType>
     *                             &lt;/element>
     *                           &lt;/sequence>
     *                           &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_DETECTOR_NUMBER" />
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                 &lt;/sequence>
     *               &lt;/restriction>
     *             &lt;/complexContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *       &lt;/sequence>
     *       &lt;attribute name="metadataLevel" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}metadataLevel" fixed="Standard" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "detectorList"
    })
    public static class GranulesInformation {

        @XmlElement(name = "Detector_List", required = true)
        protected ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList detectorList;
        @XmlAttribute(name = "metadataLevel")
        protected String metadataLevel;

        /**
         * Obtient la valeur de la propriété detectorList.
         * 
         * @return
         *     possible object is
         *     {@link ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList }
         *     
         */
        public ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList getDetectorList() {
            return detectorList;
        }

        /**
         * Définit la valeur de la propriété detectorList.
         * 
         * @param value
         *     allowed object is
         *     {@link ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList }
         *     
         */
        public void setDetectorList(ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList value) {
            this.detectorList = value;
        }

        /**
         * Obtient la valeur de la propriété metadataLevel.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getMetadataLevel() {
            if (metadataLevel == null) {
                return "Standard";
            } else {
                return metadataLevel;
            }
        }

        /**
         * Définit la valeur de la propriété metadataLevel.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setMetadataLevel(String value) {
            this.metadataLevel = value;
        }


        /**
         * <p>Classe Java pour anonymous complex type.
         * 
         * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
         * 
         * <pre>
         * &lt;complexType>
         *   &lt;complexContent>
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *       &lt;sequence>
         *         &lt;element name="Detector" maxOccurs="12">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                 &lt;sequence>
         *                   &lt;element name="Granule_List">
         *                     &lt;complexType>
         *                       &lt;complexContent>
         *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                           &lt;sequence>
         *                             &lt;element name="Granule" maxOccurs="unbounded">
         *                               &lt;complexType>
         *                                 &lt;complexContent>
         *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                                     &lt;sequence>
         *                                       &lt;element name="POSITION">
         *                                         &lt;simpleType>
         *                                           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
         *                                             &lt;minInclusive value="1"/>
         *                                           &lt;/restriction>
         *                                         &lt;/simpleType>
         *                                       &lt;/element>
         *                                     &lt;/sequence>
         *                                     &lt;attribute name="granuleId" use="required" type="{http://gs2.esa.int/DICO/1.0/DataAccess/item/}GRANULE_ID" />
         *                                   &lt;/restriction>
         *                                 &lt;/complexContent>
         *                               &lt;/complexType>
         *                             &lt;/element>
         *                           &lt;/sequence>
         *                         &lt;/restriction>
         *                       &lt;/complexContent>
         *                     &lt;/complexType>
         *                   &lt;/element>
         *                 &lt;/sequence>
         *                 &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_DETECTOR_NUMBER" />
         *               &lt;/restriction>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *       &lt;/sequence>
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "detector"
        })
        public static class DetectorList {

            @XmlElement(name = "Detector", required = true)
            protected List<ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector> detector;

            /**
             * Gets the value of the detector property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the detector property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getDetector().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector }
             * 
             * 
             */
            public List<ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector> getDetector() {
                if (detector == null) {
                    detector = new ArrayList<ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector>();
                }
                return this.detector;
            }


            /**
             * <p>Classe Java pour anonymous complex type.
             * 
             * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
             * 
             * <pre>
             * &lt;complexType>
             *   &lt;complexContent>
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *       &lt;sequence>
             *         &lt;element name="Granule_List">
             *           &lt;complexType>
             *             &lt;complexContent>
             *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                 &lt;sequence>
             *                   &lt;element name="Granule" maxOccurs="unbounded">
             *                     &lt;complexType>
             *                       &lt;complexContent>
             *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                           &lt;sequence>
             *                             &lt;element name="POSITION">
             *                               &lt;simpleType>
             *                                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
             *                                   &lt;minInclusive value="1"/>
             *                                 &lt;/restriction>
             *                               &lt;/simpleType>
             *                             &lt;/element>
             *                           &lt;/sequence>
             *                           &lt;attribute name="granuleId" use="required" type="{http://gs2.esa.int/DICO/1.0/DataAccess/item/}GRANULE_ID" />
             *                         &lt;/restriction>
             *                       &lt;/complexContent>
             *                     &lt;/complexType>
             *                   &lt;/element>
             *                 &lt;/sequence>
             *               &lt;/restriction>
             *             &lt;/complexContent>
             *           &lt;/complexType>
             *         &lt;/element>
             *       &lt;/sequence>
             *       &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_DETECTOR_NUMBER" />
             *     &lt;/restriction>
             *   &lt;/complexContent>
             * &lt;/complexType>
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {
                "granuleList"
            })
            public static class Detector {

                @XmlElement(name = "Granule_List", required = true)
                protected ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector.GranuleList granuleList;
                @XmlAttribute(name = "detectorId", required = true)
                protected String detectorId;

                /**
                 * Obtient la valeur de la propriété granuleList.
                 * 
                 * @return
                 *     possible object is
                 *     {@link ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector.GranuleList }
                 *     
                 */
                public ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector.GranuleList getGranuleList() {
                    return granuleList;
                }

                /**
                 * Définit la valeur de la propriété granuleList.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector.GranuleList }
                 *     
                 */
                public void setGranuleList(ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector.GranuleList value) {
                    this.granuleList = value;
                }

                /**
                 * Obtient la valeur de la propriété detectorId.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getDetectorId() {
                    return detectorId;
                }

                /**
                 * Définit la valeur de la propriété detectorId.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setDetectorId(String value) {
                    this.detectorId = value;
                }


                /**
                 * <p>Classe Java pour anonymous complex type.
                 * 
                 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
                 * 
                 * <pre>
                 * &lt;complexType>
                 *   &lt;complexContent>
                 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *       &lt;sequence>
                 *         &lt;element name="Granule" maxOccurs="unbounded">
                 *           &lt;complexType>
                 *             &lt;complexContent>
                 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *                 &lt;sequence>
                 *                   &lt;element name="POSITION">
                 *                     &lt;simpleType>
                 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
                 *                         &lt;minInclusive value="1"/>
                 *                       &lt;/restriction>
                 *                     &lt;/simpleType>
                 *                   &lt;/element>
                 *                 &lt;/sequence>
                 *                 &lt;attribute name="granuleId" use="required" type="{http://gs2.esa.int/DICO/1.0/DataAccess/item/}GRANULE_ID" />
                 *               &lt;/restriction>
                 *             &lt;/complexContent>
                 *           &lt;/complexType>
                 *         &lt;/element>
                 *       &lt;/sequence>
                 *     &lt;/restriction>
                 *   &lt;/complexContent>
                 * &lt;/complexType>
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "granule"
                })
                public static class GranuleList {

                    @XmlElement(name = "Granule", required = true)
                    protected List<ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector.GranuleList.Granule> granule;

                    /**
                     * Gets the value of the granule property.
                     * 
                     * <p>
                     * This accessor method returns a reference to the live list,
                     * not a snapshot. Therefore any modification you make to the
                     * returned list will be present inside the JAXB object.
                     * This is why there is not a <CODE>set</CODE> method for the granule property.
                     * 
                     * <p>
                     * For example, to add a new item, do as follows:
                     * <pre>
                     *    getGranule().add(newItem);
                     * </pre>
                     * 
                     * 
                     * <p>
                     * Objects of the following type(s) are allowed in the list
                     * {@link ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector.GranuleList.Granule }
                     * 
                     * 
                     */
                    public List<ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector.GranuleList.Granule> getGranule() {
                        if (granule == null) {
                            granule = new ArrayList<ANIMAGEDATAINFODSL1A.GranulesInformation.DetectorList.Detector.GranuleList.Granule>();
                        }
                        return this.granule;
                    }


                    /**
                     * <p>Classe Java pour anonymous complex type.
                     * 
                     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
                     * 
                     * <pre>
                     * &lt;complexType>
                     *   &lt;complexContent>
                     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                     *       &lt;sequence>
                     *         &lt;element name="POSITION">
                     *           &lt;simpleType>
                     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
                     *               &lt;minInclusive value="1"/>
                     *             &lt;/restriction>
                     *           &lt;/simpleType>
                     *         &lt;/element>
                     *       &lt;/sequence>
                     *       &lt;attribute name="granuleId" use="required" type="{http://gs2.esa.int/DICO/1.0/DataAccess/item/}GRANULE_ID" />
                     *     &lt;/restriction>
                     *   &lt;/complexContent>
                     * &lt;/complexType>
                     * </pre>
                     * 
                     * 
                     */
                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {
                        "position"
                    })
                    public static class Granule {

                        @XmlElement(name = "POSITION")
                        protected int position;
                        @XmlAttribute(name = "granuleId", required = true)
                        protected String granuleId;

                        /**
                         * Obtient la valeur de la propriété position.
                         * 
                         */
                        public int getPOSITION() {
                            return position;
                        }

                        /**
                         * Définit la valeur de la propriété position.
                         * 
                         */
                        public void setPOSITION(int value) {
                            this.position = value;
                        }

                        /**
                         * Obtient la valeur de la propriété granuleId.
                         * 
                         * @return
                         *     possible object is
                         *     {@link String }
                         *     
                         */
                        public String getGranuleId() {
                            return granuleId;
                        }

                        /**
                         * Définit la valeur de la propriété granuleId.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link String }
                         *     
                         */
                        public void setGranuleId(String value) {
                            this.granuleId = value;
                        }

                    }

                }

            }

        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="FIRST" type="{http://www.w3.org/2001/XMLSchema}unsignedInt"/>
     *         &lt;element name="NUMBER_SOURCE" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" maxOccurs="unbounded"/>
     *       &lt;/sequence>
     *       &lt;attribute name="metadataLevel" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}metadataLevel" fixed="Standard" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "first",
        "numbersource"
    })
    public static class ListFakeDecomprSourceFrames {

        @XmlElement(name = "FIRST")
        @XmlSchemaType(name = "unsignedInt")
        protected long first;
        @XmlElement(name = "NUMBER_SOURCE", type = Long.class)
        @XmlSchemaType(name = "unsignedInt")
        protected List<Long> numbersource;
        @XmlAttribute(name = "metadataLevel")
        protected String metadataLevel;

        /**
         * Obtient la valeur de la propriété first.
         * 
         */
        public long getFIRST() {
            return first;
        }

        /**
         * Définit la valeur de la propriété first.
         * 
         */
        public void setFIRST(long value) {
            this.first = value;
        }

        /**
         * Gets the value of the numbersource property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the numbersource property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getNUMBERSOURCE().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Long }
         * 
         * 
         */
        public List<Long> getNUMBERSOURCE() {
            if (numbersource == null) {
                numbersource = new ArrayList<Long>();
            }
            return this.numbersource;
        }

        /**
         * Obtient la valeur de la propriété metadataLevel.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getMetadataLevel() {
            if (metadataLevel == null) {
                return "Standard";
            } else {
                return metadataLevel;
            }
        }

        /**
         * Définit la valeur de la propriété metadataLevel.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setMetadataLevel(String value) {
            this.metadataLevel = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="COMPRESSION">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *               &lt;enumeration value="NONE"/>
     *               &lt;enumeration value="LOSSY"/>
     *               &lt;enumeration value="LOSSLESS"/>
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *       &lt;/sequence>
     *       &lt;attribute name="metadataLevel" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}metadataLevel" fixed="Standard" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "compression"
    })
    public static class ProductCompression {

        @XmlElement(name = "COMPRESSION", required = true)
        protected String compression;
        @XmlAttribute(name = "metadataLevel")
        protected String metadataLevel;

        /**
         * Obtient la valeur de la propriété compression.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getCOMPRESSION() {
            return compression;
        }

        /**
         * Définit la valeur de la propriété compression.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setCOMPRESSION(String value) {
            this.compression = value;
        }

        /**
         * Obtient la valeur de la propriété metadataLevel.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getMetadataLevel() {
            if (metadataLevel == null) {
                return "Standard";
            } else {
                return metadataLevel;
            }
        }

        /**
         * Définit la valeur de la propriété metadataLevel.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setMetadataLevel(String value) {
            this.metadataLevel = value;
        }

    }


    /**
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;extension base="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_RADIOMETRIC_DATA_L1A">
     *       &lt;attribute name="metadataLevel" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}metadataLevel" fixed="Standard" />
     *     &lt;/extension>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class RadiometricInfo
        extends ARADIOMETRICDATAL1A
    {

        @XmlAttribute(name = "metadataLevel")
        protected String metadataLevel;

        /**
         * Obtient la valeur de la propriété metadataLevel.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getMetadataLevel() {
            if (metadataLevel == null) {
                return "Standard";
            } else {
                return metadataLevel;
            }
        }

        /**
         * Définit la valeur de la propriété metadataLevel.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setMetadataLevel(String value) {
            this.metadataLevel = value;
        }

    }

}
