//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.7 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.07.07 à 03:07:45 PM CEST 
//


package _int.esa.gs2.dico._1_0.sy.geographical;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour Pointing_Direction_Type complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="Pointing_Direction_Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Az" type="{http://gs2.esa.int/DICO/1.0/SY/geographical/}Azimuth_Type"/>
 *         &lt;element name="El" type="{http://gs2.esa.int/DICO/1.0/SY/geographical/}Elevation_Type"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Pointing_Direction_Type", propOrder = {
    "az",
    "el"
})
public class PointingDirectionType {

    @XmlElement(name = "Az", required = true)
    protected AzimuthType az;
    @XmlElement(name = "El", required = true)
    protected ElevationType el;

    /**
     * Obtient la valeur de la propriété az.
     * 
     * @return
     *     possible object is
     *     {@link AzimuthType }
     *     
     */
    public AzimuthType getAz() {
        return az;
    }

    /**
     * Définit la valeur de la propriété az.
     * 
     * @param value
     *     allowed object is
     *     {@link AzimuthType }
     *     
     */
    public void setAz(AzimuthType value) {
        this.az = value;
    }

    /**
     * Obtient la valeur de la propriété el.
     * 
     * @return
     *     possible object is
     *     {@link ElevationType }
     *     
     */
    public ElevationType getEl() {
        return el;
    }

    /**
     * Définit la valeur de la propriété el.
     * 
     * @param value
     *     allowed object is
     *     {@link ElevationType }
     *     
     */
    public void setEl(ElevationType value) {
        this.el = value;
    }

}
