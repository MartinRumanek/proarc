//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.01.21 at 01:10:09 AM CET 
//


package cz.cas.lib.proarc.desa.nsesss2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * Sada elementů pro identifikaci entit nebo objektů.
 * 
 * <p>Java class for tIdentifikator complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tIdentifikator">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.mvcr.cz/nsesss/v2>tIdentifikatorHodnota">
 *       &lt;attribute name="zdroj" use="required" type="{http://www.mvcr.cz/nsesss/v2}tNazev" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tIdentifikator", namespace = "http://www.mvcr.cz/nsesss/v2", propOrder = {
    "value"
})
public class TIdentifikator {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "zdroj", required = true)
    protected String zdroj;

    /**
     * Identifikátor je údaj pevně spojený s entitou nebo objektem zajišťující jejich nezaměnitelnost a jedinečnost v rámci ERMS nebo v rámci systémů elektronické komunikace (například e-mail, informační systém datových schránek, vydané certifikáty). V případě dokumentu tento identifikátor plní funkci jednoznačného identifikátoru ve smyslu zákona č. 499/2004 Sb. Jednoznačný identifikátor obsahuje zejména označení původce, popřípadě zkratku označení původce, a to ve formě alfanumerického kódu. V případě komponenty se zaznamenávají všechny identifikátory, které zajišťují nezaměnitelnost a jedinečnost entity v rámci příslušných informačních systémů, ve kterých je komponenta zpracovávána nebo zpřístupňována (ERMS, e-mail, informační systém datových schránek apod.).
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the zdroj property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getZdroj() {
        return zdroj;
    }

    /**
     * Sets the value of the zdroj property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setZdroj(String value) {
        this.zdroj = value;
    }

}
