//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.7 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.07.07 à 03:07:45 PM CEST 
//


package _int.esa.gs2.dico._1_0.sy.image;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour A_RESTORATION_SCENARIO.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="A_RESTORATION_SCENARIO">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="F_CONVOLUTION"/>
 *     &lt;enumeration value="W_DENOISING"/>
 *     &lt;enumeration value="F_CONVOLUTION_W_DENOISING"/>
 *     &lt;enumeration value="NONE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "A_RESTORATION_SCENARIO")
@XmlEnum
public enum ARESTORATIONSCENARIO {

    F_CONVOLUTION,
    W_DENOISING,
    F_CONVOLUTION_W_DENOISING,
    NONE;

    public String value() {
        return name();
    }

    public static ARESTORATIONSCENARIO fromValue(String v) {
        return valueOf(v);
    }

}
