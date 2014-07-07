//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.7 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.07.07 à 03:07:45 PM CEST 
//


package _int.esa.gs2.dico._1_0.sy.misc;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * Space separated list of 3 integer values expressed in millimeters ('mm' unit attribute)
 * 
 * <p>Classe Java pour A_LIST_OF_3_LONG_WITH_MM_ATTR complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="A_LIST_OF_3_LONG_WITH_MM_ATTR">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://gs2.esa.int/DICO/1.0/SY/misc/>A_LIST_OF_3_LONG">
 *       &lt;attribute name="unit" use="required" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_MM_UNIT" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "A_LIST_OF_3_LONG_WITH_MM_ATTR", propOrder = {
    "value"
})
public class ALISTOF3LONGWITHMMATTR {

    @XmlValue
    protected List<Long> value;
    @XmlAttribute(name = "unit", required = true)
    protected AMMUNIT unit;

    /**
     * Space separated list of 3 long values Gets the value of the value property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the value property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getValue().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Long }
     * 
     * 
     */
    public List<Long> getValue() {
        if (value == null) {
            value = new ArrayList<Long>();
        }
        return this.value;
    }

    /**
     * Obtient la valeur de la propriété unit.
     * 
     * @return
     *     possible object is
     *     {@link AMMUNIT }
     *     
     */
    public AMMUNIT getUnit() {
        return unit;
    }

    /**
     * Définit la valeur de la propriété unit.
     * 
     * @param value
     *     allowed object is
     *     {@link AMMUNIT }
     *     
     */
    public void setUnit(AMMUNIT value) {
        this.unit = value;
    }

}
