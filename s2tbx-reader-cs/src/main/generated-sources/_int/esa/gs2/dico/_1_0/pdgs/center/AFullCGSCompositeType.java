//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.7 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2014.07.07 à 03:07:45 PM CEST 
//


package _int.esa.gs2.dico._1_0.pdgs.center;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour A_Full_CGS_Composite_Type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="A_Full_CGS_Composite_Type">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value=".*SVLB.*"/>
 *     &lt;enumeration value=".*MASP.*"/>
 *     &lt;enumeration value=".*MATA.*"/>
 *     &lt;enumeration value=".*CGS1.*"/>
 *     &lt;enumeration value=".*CGS2.*"/>
 *     &lt;enumeration value=".*CGS3.*"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "A_Full_CGS_Composite_Type", namespace = "http://gs2.esa.int/DICO/1.0/PDGS/center/")
@XmlEnum
public enum AFullCGSCompositeType {

    @XmlEnumValue(".*SVLB.*")
    SVLB(".*SVLB.*"),
    @XmlEnumValue(".*MASP.*")
    MASP(".*MASP.*"),
    @XmlEnumValue(".*MATA.*")
    MATA(".*MATA.*"),
    @XmlEnumValue(".*CGS1.*")
    CGS_1(".*CGS1.*"),
    @XmlEnumValue(".*CGS2.*")
    CGS_2(".*CGS2.*"),
    @XmlEnumValue(".*CGS3.*")
    CGS_3(".*CGS3.*");
    private final String value;

    AFullCGSCompositeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static AFullCGSCompositeType fromValue(String v) {
        for (AFullCGSCompositeType c: AFullCGSCompositeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
