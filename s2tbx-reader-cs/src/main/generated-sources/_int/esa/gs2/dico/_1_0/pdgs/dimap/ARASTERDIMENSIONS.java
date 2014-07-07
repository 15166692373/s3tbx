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
import javax.xml.bind.annotation.XmlType;
import _int.esa.gs2.dico._1_0.sy.image.ANIMAGESIZE;


/**
 * <p>Classe Java pour A_RASTER_DIMENSIONS complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="A_RASTER_DIMENSIONS">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Dimensions_List">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Dimensions" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;element name="Detector_Dimensions" maxOccurs="unbounded">
 *                               &lt;complexType>
 *                                 &lt;complexContent>
 *                                   &lt;extension base="{http://gs2.esa.int/DICO/1.0/SY/image/}AN_IMAGE_SIZE">
 *                                     &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/image/}A_DETECTOR_NUMBER" />
 *                                   &lt;/extension>
 *                                 &lt;/complexContent>
 *                               &lt;/complexType>
 *                             &lt;/element>
 *                           &lt;/sequence>
 *                           &lt;attribute name="bandId" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/image/}A_BAND_NUMBER" />
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="NBANDS">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
 *               &lt;minInclusive value="1"/>
 *               &lt;maxInclusive value="13"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="NBITS">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
 *               &lt;enumeration value="8"/>
 *               &lt;enumeration value="16"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
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
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "A_RASTER_DIMENSIONS", propOrder = {
    "dimensionsList",
    "nbands",
    "nbits",
    "compression"
})
public class ARASTERDIMENSIONS {

    @XmlElement(name = "Dimensions_List", required = true)
    protected ARASTERDIMENSIONS.DimensionsList dimensionsList;
    @XmlElement(name = "NBANDS")
    protected int nbands;
    @XmlElement(name = "NBITS")
    protected int nbits;
    @XmlElement(name = "COMPRESSION", required = true)
    protected String compression;

    /**
     * Obtient la valeur de la propriété dimensionsList.
     * 
     * @return
     *     possible object is
     *     {@link ARASTERDIMENSIONS.DimensionsList }
     *     
     */
    public ARASTERDIMENSIONS.DimensionsList getDimensionsList() {
        return dimensionsList;
    }

    /**
     * Définit la valeur de la propriété dimensionsList.
     * 
     * @param value
     *     allowed object is
     *     {@link ARASTERDIMENSIONS.DimensionsList }
     *     
     */
    public void setDimensionsList(ARASTERDIMENSIONS.DimensionsList value) {
        this.dimensionsList = value;
    }

    /**
     * Obtient la valeur de la propriété nbands.
     * 
     */
    public int getNBANDS() {
        return nbands;
    }

    /**
     * Définit la valeur de la propriété nbands.
     * 
     */
    public void setNBANDS(int value) {
        this.nbands = value;
    }

    /**
     * Obtient la valeur de la propriété nbits.
     * 
     */
    public int getNBITS() {
        return nbits;
    }

    /**
     * Définit la valeur de la propriété nbits.
     * 
     */
    public void setNBITS(int value) {
        this.nbits = value;
    }

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
     * <p>Classe Java pour anonymous complex type.
     * 
     * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Dimensions" maxOccurs="unbounded">
     *           &lt;complexType>
     *             &lt;complexContent>
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *                 &lt;sequence>
     *                   &lt;element name="Detector_Dimensions" maxOccurs="unbounded">
     *                     &lt;complexType>
     *                       &lt;complexContent>
     *                         &lt;extension base="{http://gs2.esa.int/DICO/1.0/SY/image/}AN_IMAGE_SIZE">
     *                           &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/image/}A_DETECTOR_NUMBER" />
     *                         &lt;/extension>
     *                       &lt;/complexContent>
     *                     &lt;/complexType>
     *                   &lt;/element>
     *                 &lt;/sequence>
     *                 &lt;attribute name="bandId" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/image/}A_BAND_NUMBER" />
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
        "dimensions"
    })
    public static class DimensionsList {

        @XmlElement(name = "Dimensions", required = true)
        protected List<ARASTERDIMENSIONS.DimensionsList.Dimensions> dimensions;

        /**
         * Gets the value of the dimensions property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the dimensions property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getDimensions().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link ARASTERDIMENSIONS.DimensionsList.Dimensions }
         * 
         * 
         */
        public List<ARASTERDIMENSIONS.DimensionsList.Dimensions> getDimensions() {
            if (dimensions == null) {
                dimensions = new ArrayList<ARASTERDIMENSIONS.DimensionsList.Dimensions>();
            }
            return this.dimensions;
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
         *         &lt;element name="Detector_Dimensions" maxOccurs="unbounded">
         *           &lt;complexType>
         *             &lt;complexContent>
         *               &lt;extension base="{http://gs2.esa.int/DICO/1.0/SY/image/}AN_IMAGE_SIZE">
         *                 &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/image/}A_DETECTOR_NUMBER" />
         *               &lt;/extension>
         *             &lt;/complexContent>
         *           &lt;/complexType>
         *         &lt;/element>
         *       &lt;/sequence>
         *       &lt;attribute name="bandId" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/image/}A_BAND_NUMBER" />
         *     &lt;/restriction>
         *   &lt;/complexContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "detectorDimensions"
        })
        public static class Dimensions {

            @XmlElement(name = "Detector_Dimensions", required = true)
            protected List<ARASTERDIMENSIONS.DimensionsList.Dimensions.DetectorDimensions> detectorDimensions;
            @XmlAttribute(name = "bandId", required = true)
            protected String bandId;

            /**
             * Gets the value of the detectorDimensions property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the detectorDimensions property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getDetectorDimensions().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link ARASTERDIMENSIONS.DimensionsList.Dimensions.DetectorDimensions }
             * 
             * 
             */
            public List<ARASTERDIMENSIONS.DimensionsList.Dimensions.DetectorDimensions> getDetectorDimensions() {
                if (detectorDimensions == null) {
                    detectorDimensions = new ArrayList<ARASTERDIMENSIONS.DimensionsList.Dimensions.DetectorDimensions>();
                }
                return this.detectorDimensions;
            }

            /**
             * Obtient la valeur de la propriété bandId.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getBandId() {
                return bandId;
            }

            /**
             * Définit la valeur de la propriété bandId.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setBandId(String value) {
                this.bandId = value;
            }


            /**
             * <p>Classe Java pour anonymous complex type.
             * 
             * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
             * 
             * <pre>
             * &lt;complexType>
             *   &lt;complexContent>
             *     &lt;extension base="{http://gs2.esa.int/DICO/1.0/SY/image/}AN_IMAGE_SIZE">
             *       &lt;attribute name="detectorId" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/image/}A_DETECTOR_NUMBER" />
             *     &lt;/extension>
             *   &lt;/complexContent>
             * &lt;/complexType>
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "")
            public static class DetectorDimensions
                extends ANIMAGESIZE
            {

                @XmlAttribute(name = "detectorId", required = true)
                protected String detectorId;

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

            }

        }

    }

}
