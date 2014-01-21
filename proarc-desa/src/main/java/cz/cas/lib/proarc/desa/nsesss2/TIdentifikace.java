//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.01.21 at 01:10:09 AM CET 
//


package cz.cas.lib.proarc.desa.nsesss2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Sada elementů pro identifikační údaje entit. Prvek je opakovatelný pouze v případě použití v entitě "komponenta".
 * 
 * <p>Java class for tIdentifikace complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tIdentifikace">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Identifikator" type="{http://www.mvcr.cz/nsesss/v2}tIdentifikator" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tIdentifikace", namespace = "http://www.mvcr.cz/nsesss/v2", propOrder = {
    "identifikator"
})
public class TIdentifikace {

    @XmlElement(name = "Identifikator", namespace = "http://www.mvcr.cz/nsesss/v2", required = true)
    protected List<TIdentifikator> identifikator;

    /**
     * Gets the value of the identifikator property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the identifikator property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIdentifikator().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TIdentifikator }
     * 
     * 
     */
    public List<TIdentifikator> getIdentifikator() {
        if (identifikator == null) {
            identifikator = new ArrayList<TIdentifikator>();
        }
        return this.identifikator;
    }

}
