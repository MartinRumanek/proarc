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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Sada elementů pro popis entity "komponenta" (jednoznačně vymezený řetězec bitů tvořící počítačový soubor). Tato sada se povinně alespoň jednou vyskytuje v každé instanci XML podle tohoto schématu.
 * 
 * <p>Java class for tKomponenta complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tKomponenta">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="EvidencniUdaje" type="{http://www.mvcr.cz/nsesss/v2}tEvidencniUdajeKomponenty"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ID" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tKomponenta", namespace = "http://www.mvcr.cz/nsesss/v2", propOrder = {
    "evidencniUdaje"
})
public class TKomponenta {

    @XmlElement(name = "EvidencniUdaje", namespace = "http://www.mvcr.cz/nsesss/v2", required = true)
    protected TEvidencniUdajeKomponenty evidencniUdaje;
    @XmlAttribute(name = "ID", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;

    /**
     * Gets the value of the evidencniUdaje property.
     * 
     * @return
     *     possible object is
     *     {@link TEvidencniUdajeKomponenty }
     *     
     */
    public TEvidencniUdajeKomponenty getEvidencniUdaje() {
        return evidencniUdaje;
    }

    /**
     * Sets the value of the evidencniUdaje property.
     * 
     * @param value
     *     allowed object is
     *     {@link TEvidencniUdajeKomponenty }
     *     
     */
    public void setEvidencniUdaje(TEvidencniUdajeKomponenty value) {
        this.evidencniUdaje = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setID(String value) {
        this.id = value;
    }

}
