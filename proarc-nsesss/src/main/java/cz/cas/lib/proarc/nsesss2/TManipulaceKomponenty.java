//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.01.21 at 01:10:09 AM CET 
//


package cz.cas.lib.proarc.nsesss2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Metadatový kontejner pro popis výsledku ověření certifikátů a nich založených bezpečnostních prvků (tj. elektronického podpisu, elektronické značky nebo časového razítka).
 * 
 * <p>Java class for tManipulaceKomponenty complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tManipulaceKomponenty">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="OvereniBezpecnostnihoPrvku" type="{http://www.mvcr.cz/nsesss/v2}tOvereniBezpecnostnihoPrvku" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tManipulaceKomponenty", namespace = "http://www.mvcr.cz/nsesss/v2", propOrder = {
    "overeniBezpecnostnihoPrvku"
})
public class TManipulaceKomponenty {

    @XmlElement(name = "OvereniBezpecnostnihoPrvku", namespace = "http://www.mvcr.cz/nsesss/v2")
    protected List<TOvereniBezpecnostnihoPrvku> overeniBezpecnostnihoPrvku;

    /**
     * Gets the value of the overeniBezpecnostnihoPrvku property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the overeniBezpecnostnihoPrvku property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOvereniBezpecnostnihoPrvku().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TOvereniBezpecnostnihoPrvku }
     * 
     * 
     */
    public List<TOvereniBezpecnostnihoPrvku> getOvereniBezpecnostnihoPrvku() {
        if (overeniBezpecnostnihoPrvku == null) {
            overeniBezpecnostnihoPrvku = new ArrayList<TOvereniBezpecnostnihoPrvku>();
        }
        return this.overeniBezpecnostnihoPrvku;
    }

}
