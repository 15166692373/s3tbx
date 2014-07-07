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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlType;


/**
 * A 3*N matrix (double values)
 * 
 * <p>Classe Java pour A_3xN_MATRIX complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="A_3xN_MATRIX">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="LINE1" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_DOUBLE"/>
 *         &lt;element name="LINE2" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_DOUBLE"/>
 *         &lt;element name="LINE3" type="{http://gs2.esa.int/DICO/1.0/SY/misc/}A_LIST_OF_DOUBLE"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "A_3xN_MATRIX", propOrder = {
    "line1",
    "line2",
    "line3"
})
public class A3XNMATRIX {

    @XmlList
    @XmlElement(name = "LINE1", type = Double.class)
    protected List<Double> line1;
    @XmlList
    @XmlElement(name = "LINE2", type = Double.class)
    protected List<Double> line2;
    @XmlList
    @XmlElement(name = "LINE3", type = Double.class)
    protected List<Double> line3;

    /**
     * Gets the value of the line1 property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the line1 property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLINE1().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Double }
     * 
     * 
     */
    public List<Double> getLINE1() {
        if (line1 == null) {
            line1 = new ArrayList<Double>();
        }
        return this.line1;
    }

    /**
     * Gets the value of the line2 property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the line2 property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLINE2().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Double }
     * 
     * 
     */
    public List<Double> getLINE2() {
        if (line2 == null) {
            line2 = new ArrayList<Double>();
        }
        return this.line2;
    }

    /**
     * Gets the value of the line3 property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the line3 property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLINE3().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Double }
     * 
     * 
     */
    public List<Double> getLINE3() {
        if (line3 == null) {
            line3 = new ArrayList<Double>();
        }
        return this.line3;
    }

}
