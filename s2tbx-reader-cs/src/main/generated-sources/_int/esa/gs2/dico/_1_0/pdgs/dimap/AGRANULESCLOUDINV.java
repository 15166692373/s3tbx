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
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import _int.esa.gs2.dico._1_0.sy.geographical.AGMLPOLYGON3D;
import _int.esa.gs2.dico._1_0.sy.image.ANIMAGEGEOMETRY;


/**
 * <p>Classe Java pour A_GRANULES_CLOUD_INV complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="A_GRANULES_CLOUD_INV">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GEOMETRY" type="{http://gs2.esa.int/DICO/1.0/SY/image/}AN_IMAGE_GEOMETRY"/>
 *         &lt;element name="Detector_List">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Detector" maxOccurs="unbounded">
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
 *                                                 &lt;element name="GPS_TIME" type="{http://gs2.esa.int/DICO/1.0/SY/date_time/}A_GPS_NANOSECOND_DATE_TIME"/>
 *                                                 &lt;element name="Ground_Footprint" type="{http://gs2.esa.int/DICO/1.0/SY/geographical/}A_GML_POLYGON_3D"/>
 *                                                 &lt;element name="QL_FOOTPRINT" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_8_DOUBLE"/>
 *                                                 &lt;element name="Geometric_Header">
 *                                                   &lt;complexType>
 *                                                     &lt;complexContent>
 *                                                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                                         &lt;sequence>
 *                                                           &lt;element name="GROUND_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_3_DOUBLE"/>
 *                                                           &lt;element name="QL_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_2_DOUBLE"/>
 *                                                           &lt;element name="Incidence_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
 *                                                           &lt;element name="Solar_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
 *                                                         &lt;/sequence>
 *                                                       &lt;/restriction>
 *                                                     &lt;/complexContent>
 *                                                   &lt;/complexType>
 *                                                 &lt;/element>
 *                                                 &lt;element name="Quality_Assessment">
 *                                                   &lt;complexType>
 *                                                     &lt;complexContent>
 *                                                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                                                         &lt;sequence>
 *                                                           &lt;element name="DEGRADED_ANC_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
 *                                                           &lt;element name="DEGRADED_MSI_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
 *                                                           &lt;element name="CLOUDY_PIXEL_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_CLOUDY_PIXEL_PERCENTAGE"/>
 *                                                         &lt;/sequence>
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
 *                                   &lt;/restriction>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                           &lt;/sequence>
 *                           &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/image/}A_DETECTOR_NUMBER" />
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
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "A_GRANULES_CLOUD_INV", propOrder = {
    "geometry",
    "detectorList"
})
public class AGRANULESCLOUDINV {

    @XmlElement(name = "GEOMETRY", required = true)
    protected ANIMAGEGEOMETRY geometry;
    @XmlElement(name = "Detector_List", required = true)
    protected AGRANULESCLOUDINV.DetectorList detectorList;

    /**
     * Obtient la valeur de la propriété geometry.
     * 
     * @return
     *     possible object is
     *     {@link ANIMAGEGEOMETRY }
     *     
     */
    public ANIMAGEGEOMETRY getGEOMETRY() {
        return geometry;
    }

    /**
     * Définit la valeur de la propriété geometry.
     * 
     * @param value
     *     allowed object is
     *     {@link ANIMAGEGEOMETRY }
     *     
     */
    public void setGEOMETRY(ANIMAGEGEOMETRY value) {
        this.geometry = value;
    }

    /**
     * Obtient la valeur de la propriété detectorList.
     * 
     * @return
     *     possible object is
     *     {@link AGRANULESCLOUDINV.DetectorList }
     *     
     */
    public AGRANULESCLOUDINV.DetectorList getDetectorList() {
        return detectorList;
    }

    /**
     * Définit la valeur de la propriété detectorList.
     * 
     * @param value
     *     allowed object is
     *     {@link AGRANULESCLOUDINV.DetectorList }
     *     
     */
    public void setDetectorList(AGRANULESCLOUDINV.DetectorList value) {
        this.detectorList = value;
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
     *         &lt;element name="Detector" maxOccurs="unbounded">
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
     *                                       &lt;element name="GPS_TIME" type="{http://gs2.esa.int/DICO/1.0/SY/date_time/}A_GPS_NANOSECOND_DATE_TIME"/>
     *                                       &lt;element name="Ground_Footprint" type="{http://gs2.esa.int/DICO/1.0/SY/geographical/}A_GML_POLYGON_3D"/>
     *                                       &lt;element name="QL_FOOTPRINT" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_8_DOUBLE"/>
     *                                       &lt;element name="Geometric_Header">
     *                                         &lt;complexType>
     *                                           &lt;complexContent>
     *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                               &lt;sequence>
     *                                                 &lt;element name="GROUND_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_3_DOUBLE"/>
     *                                                 &lt;element name="QL_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_2_DOUBLE"/>
     *                                                 &lt;element name="Incidence_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
     *                                                 &lt;element name="Solar_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
     *                                               &lt;/sequence>
     *                                             &lt;/restriction>
     *                                           &lt;/complexContent>
     *                                         &lt;/complexType>
     *                                       &lt;/element>
     *                                       &lt;element name="Quality_Assessment">
     *                                         &lt;complexType>
     *                                           &lt;complexContent>
     *                                             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                                               &lt;sequence>
     *                                                 &lt;element name="DEGRADED_ANC_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
     *                                                 &lt;element name="DEGRADED_MSI_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
     *                                                 &lt;element name="CLOUDY_PIXEL_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_CLOUDY_PIXEL_PERCENTAGE"/>
     *                                               &lt;/sequence>
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
     *                         &lt;/restriction>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                 &lt;/sequence>
     *                 &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/image/}A_DETECTOR_NUMBER" />
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
        protected List<AGRANULESCLOUDINV.DetectorList.Detector> detector;

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
         * {@link AGRANULESCLOUDINV.DetectorList.Detector }
         * 
         * 
         */
        public List<AGRANULESCLOUDINV.DetectorList.Detector> getDetector() {
            if (detector == null) {
                detector = new ArrayList<AGRANULESCLOUDINV.DetectorList.Detector>();
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
         *                             &lt;element name="GPS_TIME" type="{http://gs2.esa.int/DICO/1.0/SY/date_time/}A_GPS_NANOSECOND_DATE_TIME"/>
         *                             &lt;element name="Ground_Footprint" type="{http://gs2.esa.int/DICO/1.0/SY/geographical/}A_GML_POLYGON_3D"/>
         *                             &lt;element name="QL_FOOTPRINT" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_8_DOUBLE"/>
         *                             &lt;element name="Geometric_Header">
         *                               &lt;complexType>
         *                                 &lt;complexContent>
         *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                                     &lt;sequence>
         *                                       &lt;element name="GROUND_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_3_DOUBLE"/>
         *                                       &lt;element name="QL_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_2_DOUBLE"/>
         *                                       &lt;element name="Incidence_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
         *                                       &lt;element name="Solar_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
         *                                     &lt;/sequence>
         *                                   &lt;/restriction>
         *                                 &lt;/complexContent>
         *                               &lt;/complexType>
         *                             &lt;/element>
         *                             &lt;element name="Quality_Assessment">
         *                               &lt;complexType>
         *                                 &lt;complexContent>
         *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
         *                                     &lt;sequence>
         *                                       &lt;element name="DEGRADED_ANC_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
         *                                       &lt;element name="DEGRADED_MSI_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
         *                                       &lt;element name="CLOUDY_PIXEL_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_CLOUDY_PIXEL_PERCENTAGE"/>
         *                                     &lt;/sequence>
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
         *               &lt;/restriction>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *       &lt;/sequence>
         *       &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/image/}A_DETECTOR_NUMBER" />
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
            protected AGRANULESCLOUDINV.DetectorList.Detector.GranuleList granuleList;
            @XmlAttribute(name = "detectorId", required = true)
            protected String detectorId;

            /**
             * Obtient la valeur de la propriété granuleList.
             * 
             * @return
             *     possible object is
             *     {@link AGRANULESCLOUDINV.DetectorList.Detector.GranuleList }
             *     
             */
            public AGRANULESCLOUDINV.DetectorList.Detector.GranuleList getGranuleList() {
                return granuleList;
            }

            /**
             * Définit la valeur de la propriété granuleList.
             * 
             * @param value
             *     allowed object is
             *     {@link AGRANULESCLOUDINV.DetectorList.Detector.GranuleList }
             *     
             */
            public void setGranuleList(AGRANULESCLOUDINV.DetectorList.Detector.GranuleList value) {
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
             *                   &lt;element name="GPS_TIME" type="{http://gs2.esa.int/DICO/1.0/SY/date_time/}A_GPS_NANOSECOND_DATE_TIME"/>
             *                   &lt;element name="Ground_Footprint" type="{http://gs2.esa.int/DICO/1.0/SY/geographical/}A_GML_POLYGON_3D"/>
             *                   &lt;element name="QL_FOOTPRINT" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_8_DOUBLE"/>
             *                   &lt;element name="Geometric_Header">
             *                     &lt;complexType>
             *                       &lt;complexContent>
             *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                           &lt;sequence>
             *                             &lt;element name="GROUND_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_3_DOUBLE"/>
             *                             &lt;element name="QL_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_2_DOUBLE"/>
             *                             &lt;element name="Incidence_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
             *                             &lt;element name="Solar_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
             *                           &lt;/sequence>
             *                         &lt;/restriction>
             *                       &lt;/complexContent>
             *                     &lt;/complexType>
             *                   &lt;/element>
             *                   &lt;element name="Quality_Assessment">
             *                     &lt;complexType>
             *                       &lt;complexContent>
             *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
             *                           &lt;sequence>
             *                             &lt;element name="DEGRADED_ANC_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
             *                             &lt;element name="DEGRADED_MSI_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
             *                             &lt;element name="CLOUDY_PIXEL_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_CLOUDY_PIXEL_PERCENTAGE"/>
             *                           &lt;/sequence>
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
                protected List<AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule> granule;

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
                 * {@link AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule }
                 * 
                 * 
                 */
                public List<AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule> getGranule() {
                    if (granule == null) {
                        granule = new ArrayList<AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule>();
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
                 *         &lt;element name="GPS_TIME" type="{http://gs2.esa.int/DICO/1.0/SY/date_time/}A_GPS_NANOSECOND_DATE_TIME"/>
                 *         &lt;element name="Ground_Footprint" type="{http://gs2.esa.int/DICO/1.0/SY/geographical/}A_GML_POLYGON_3D"/>
                 *         &lt;element name="QL_FOOTPRINT" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_8_DOUBLE"/>
                 *         &lt;element name="Geometric_Header">
                 *           &lt;complexType>
                 *             &lt;complexContent>
                 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *                 &lt;sequence>
                 *                   &lt;element name="GROUND_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_3_DOUBLE"/>
                 *                   &lt;element name="QL_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_2_DOUBLE"/>
                 *                   &lt;element name="Incidence_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
                 *                   &lt;element name="Solar_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
                 *                 &lt;/sequence>
                 *               &lt;/restriction>
                 *             &lt;/complexContent>
                 *           &lt;/complexType>
                 *         &lt;/element>
                 *         &lt;element name="Quality_Assessment">
                 *           &lt;complexType>
                 *             &lt;complexContent>
                 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
                 *                 &lt;sequence>
                 *                   &lt;element name="DEGRADED_ANC_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
                 *                   &lt;element name="DEGRADED_MSI_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
                 *                   &lt;element name="CLOUDY_PIXEL_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_CLOUDY_PIXEL_PERCENTAGE"/>
                 *                 &lt;/sequence>
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
                    "position",
                    "gpstime",
                    "groundFootprint",
                    "qlfootprint",
                    "geometricHeader",
                    "qualityAssessment"
                })
                public static class Granule {

                    @XmlElement(name = "POSITION")
                    protected int position;
                    @XmlElement(name = "GPS_TIME", required = true)
                    protected XMLGregorianCalendar gpstime;
                    @XmlElement(name = "Ground_Footprint", required = true)
                    protected AGMLPOLYGON3D groundFootprint;
                    @XmlList
                    @XmlElement(name = "QL_FOOTPRINT", type = Double.class)
                    protected List<Double> qlfootprint;
                    @XmlElement(name = "Geometric_Header", required = true)
                    protected AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule.GeometricHeader geometricHeader;
                    @XmlElement(name = "Quality_Assessment", required = true)
                    protected AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule.QualityAssessment qualityAssessment;

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
                     * Obtient la valeur de la propriété gpstime.
                     * 
                     * @return
                     *     possible object is
                     *     {@link XMLGregorianCalendar }
                     *     
                     */
                    public XMLGregorianCalendar getGPSTIME() {
                        return gpstime;
                    }

                    /**
                     * Définit la valeur de la propriété gpstime.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link XMLGregorianCalendar }
                     *     
                     */
                    public void setGPSTIME(XMLGregorianCalendar value) {
                        this.gpstime = value;
                    }

                    /**
                     * Obtient la valeur de la propriété groundFootprint.
                     * 
                     * @return
                     *     possible object is
                     *     {@link AGMLPOLYGON3D }
                     *     
                     */
                    public AGMLPOLYGON3D getGroundFootprint() {
                        return groundFootprint;
                    }

                    /**
                     * Définit la valeur de la propriété groundFootprint.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link AGMLPOLYGON3D }
                     *     
                     */
                    public void setGroundFootprint(AGMLPOLYGON3D value) {
                        this.groundFootprint = value;
                    }

                    /**
                     * Gets the value of the qlfootprint property.
                     * 
                     * <p>
                     * This accessor method returns a reference to the live list,
                     * not a snapshot. Therefore any modification you make to the
                     * returned list will be present inside the JAXB object.
                     * This is why there is not a <CODE>set</CODE> method for the qlfootprint property.
                     * 
                     * <p>
                     * For example, to add a new item, do as follows:
                     * <pre>
                     *    getQLFOOTPRINT().add(newItem);
                     * </pre>
                     * 
                     * 
                     * <p>
                     * Objects of the following type(s) are allowed in the list
                     * {@link Double }
                     * 
                     * 
                     */
                    public List<Double> getQLFOOTPRINT() {
                        if (qlfootprint == null) {
                            qlfootprint = new ArrayList<Double>();
                        }
                        return this.qlfootprint;
                    }

                    /**
                     * Obtient la valeur de la propriété geometricHeader.
                     * 
                     * @return
                     *     possible object is
                     *     {@link AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule.GeometricHeader }
                     *     
                     */
                    public AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule.GeometricHeader getGeometricHeader() {
                        return geometricHeader;
                    }

                    /**
                     * Définit la valeur de la propriété geometricHeader.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule.GeometricHeader }
                     *     
                     */
                    public void setGeometricHeader(AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule.GeometricHeader value) {
                        this.geometricHeader = value;
                    }

                    /**
                     * Obtient la valeur de la propriété qualityAssessment.
                     * 
                     * @return
                     *     possible object is
                     *     {@link AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule.QualityAssessment }
                     *     
                     */
                    public AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule.QualityAssessment getQualityAssessment() {
                        return qualityAssessment;
                    }

                    /**
                     * Définit la valeur de la propriété qualityAssessment.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule.QualityAssessment }
                     *     
                     */
                    public void setQualityAssessment(AGRANULESCLOUDINV.DetectorList.Detector.GranuleList.Granule.QualityAssessment value) {
                        this.qualityAssessment = value;
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
                     *         &lt;element name="GROUND_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_3_DOUBLE"/>
                     *         &lt;element name="QL_CENTER" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_2_DOUBLE"/>
                     *         &lt;element name="Incidence_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
                     *         &lt;element name="Solar_Angles" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_ZENITH_AND_AZIMUTH_ANGLES"/>
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
                        "groundcenter",
                        "qlcenter",
                        "incidenceAngles",
                        "solarAngles"
                    })
                    public static class GeometricHeader {

                        @XmlList
                        @XmlElement(name = "GROUND_CENTER", type = Double.class)
                        protected List<Double> groundcenter;
                        @XmlList
                        @XmlElement(name = "QL_CENTER", type = Double.class)
                        protected List<Double> qlcenter;
                        @XmlElement(name = "Incidence_Angles", required = true)
                        protected AZENITHANDAZIMUTHANGLES incidenceAngles;
                        @XmlElement(name = "Solar_Angles", required = true)
                        protected AZENITHANDAZIMUTHANGLES solarAngles;

                        /**
                         * Gets the value of the groundcenter property.
                         * 
                         * <p>
                         * This accessor method returns a reference to the live list,
                         * not a snapshot. Therefore any modification you make to the
                         * returned list will be present inside the JAXB object.
                         * This is why there is not a <CODE>set</CODE> method for the groundcenter property.
                         * 
                         * <p>
                         * For example, to add a new item, do as follows:
                         * <pre>
                         *    getGROUNDCENTER().add(newItem);
                         * </pre>
                         * 
                         * 
                         * <p>
                         * Objects of the following type(s) are allowed in the list
                         * {@link Double }
                         * 
                         * 
                         */
                        public List<Double> getGROUNDCENTER() {
                            if (groundcenter == null) {
                                groundcenter = new ArrayList<Double>();
                            }
                            return this.groundcenter;
                        }

                        /**
                         * Gets the value of the qlcenter property.
                         * 
                         * <p>
                         * This accessor method returns a reference to the live list,
                         * not a snapshot. Therefore any modification you make to the
                         * returned list will be present inside the JAXB object.
                         * This is why there is not a <CODE>set</CODE> method for the qlcenter property.
                         * 
                         * <p>
                         * For example, to add a new item, do as follows:
                         * <pre>
                         *    getQLCENTER().add(newItem);
                         * </pre>
                         * 
                         * 
                         * <p>
                         * Objects of the following type(s) are allowed in the list
                         * {@link Double }
                         * 
                         * 
                         */
                        public List<Double> getQLCENTER() {
                            if (qlcenter == null) {
                                qlcenter = new ArrayList<Double>();
                            }
                            return this.qlcenter;
                        }

                        /**
                         * Obtient la valeur de la propriété incidenceAngles.
                         * 
                         * @return
                         *     possible object is
                         *     {@link AZENITHANDAZIMUTHANGLES }
                         *     
                         */
                        public AZENITHANDAZIMUTHANGLES getIncidenceAngles() {
                            return incidenceAngles;
                        }

                        /**
                         * Définit la valeur de la propriété incidenceAngles.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link AZENITHANDAZIMUTHANGLES }
                         *     
                         */
                        public void setIncidenceAngles(AZENITHANDAZIMUTHANGLES value) {
                            this.incidenceAngles = value;
                        }

                        /**
                         * Obtient la valeur de la propriété solarAngles.
                         * 
                         * @return
                         *     possible object is
                         *     {@link AZENITHANDAZIMUTHANGLES }
                         *     
                         */
                        public AZENITHANDAZIMUTHANGLES getSolarAngles() {
                            return solarAngles;
                        }

                        /**
                         * Définit la valeur de la propriété solarAngles.
                         * 
                         * @param value
                         *     allowed object is
                         *     {@link AZENITHANDAZIMUTHANGLES }
                         *     
                         */
                        public void setSolarAngles(AZENITHANDAZIMUTHANGLES value) {
                            this.solarAngles = value;
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
                     *         &lt;element name="DEGRADED_ANC_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
                     *         &lt;element name="DEGRADED_MSI_DATA_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_PERCENTAGE"/>
                     *         &lt;element name="CLOUDY_PIXEL_PERCENTAGE" type="{http://gs2.esa.int/DICO/1.0/PDGS/dimap/}A_CLOUDY_PIXEL_PERCENTAGE"/>
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
                        "degradedancdatapercentage",
                        "degradedmsidatapercentage",
                        "cloudypixelpercentage"
                    })
                    public static class QualityAssessment {

                        @XmlElement(name = "DEGRADED_ANC_DATA_PERCENTAGE")
                        protected double degradedancdatapercentage;
                        @XmlElement(name = "DEGRADED_MSI_DATA_PERCENTAGE")
                        protected double degradedmsidatapercentage;
                        @XmlElement(name = "CLOUDY_PIXEL_PERCENTAGE")
                        protected double cloudypixelpercentage;

                        /**
                         * Obtient la valeur de la propriété degradedancdatapercentage.
                         * 
                         */
                        public double getDEGRADEDANCDATAPERCENTAGE() {
                            return degradedancdatapercentage;
                        }

                        /**
                         * Définit la valeur de la propriété degradedancdatapercentage.
                         * 
                         */
                        public void setDEGRADEDANCDATAPERCENTAGE(double value) {
                            this.degradedancdatapercentage = value;
                        }

                        /**
                         * Obtient la valeur de la propriété degradedmsidatapercentage.
                         * 
                         */
                        public double getDEGRADEDMSIDATAPERCENTAGE() {
                            return degradedmsidatapercentage;
                        }

                        /**
                         * Définit la valeur de la propriété degradedmsidatapercentage.
                         * 
                         */
                        public void setDEGRADEDMSIDATAPERCENTAGE(double value) {
                            this.degradedmsidatapercentage = value;
                        }

                        /**
                         * Obtient la valeur de la propriété cloudypixelpercentage.
                         * 
                         */
                        public double getCLOUDYPIXELPERCENTAGE() {
                            return cloudypixelpercentage;
                        }

                        /**
                         * Définit la valeur de la propriété cloudypixelpercentage.
                         * 
                         */
                        public void setCLOUDYPIXELPERCENTAGE(double value) {
                            this.cloudypixelpercentage = value;
                        }

                    }

                }

            }

        }

    }

}
