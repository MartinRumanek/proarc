/*
 * Copyright (C) 2014 Robert Simonovsky
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cz.cas.lib.proarc.mets;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each Java content interface and Java
 * element interface generated in the cz.cas.lib.proarc.mets package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the
 * Java representation for XML content. The Java representation of XML content
 * can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory
 * methods for each of these are provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of
     * schema derived classes for package: cz.cas.lib.proarc.mets
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link MetsType }
     * 
     */
    public MetsType createMetsType() {
        return new MetsType();
    }

    /**
     * Create an instance of {@link DivType }
     * 
     */
    public DivType createDivType() {
        return new DivType();
    }

    /**
     * Create an instance of {@link StructLinkType }
     * 
     */
    public StructLinkType createStructLinkType() {
        return new StructLinkType();
    }

    /**
     * Create an instance of {@link StructLinkType.SmLinkGrp }
     * 
     */
    public StructLinkType.SmLinkGrp createStructLinkTypeSmLinkGrp() {
        return new StructLinkType.SmLinkGrp();
    }

    /**
     * Create an instance of {@link FileType }
     * 
     */
    public FileType createFileType() {
        return new FileType();
    }

    /**
     * Create an instance of {@link FileType.FContent }
     * 
     */
    public FileType.FContent createFileTypeFContent() {
        return new FileType.FContent();
    }

    /**
     * Create an instance of {@link MetsType.FileSec }
     * 
     */
    public MetsType.FileSec createMetsTypeFileSec() {
        return new MetsType.FileSec();
    }

    /**
     * Create an instance of {@link MdSecType }
     * 
     */
    public MdSecType createMdSecType() {
        return new MdSecType();
    }

    /**
     * Create an instance of {@link MdSecType.MdWrap }
     * 
     */
    public MdSecType.MdWrap createMdSecTypeMdWrap() {
        return new MdSecType.MdWrap();
    }

    /**
     * Create an instance of {@link MetsType.MetsHdr }
     * 
     */
    public MetsType.MetsHdr createMetsTypeMetsHdr() {
        return new MetsType.MetsHdr();
    }

    /**
     * Create an instance of {@link Mets }
     * 
     */
    public Mets createMets() {
        return new Mets();
    }

    /**
     * Create an instance of {@link AmdSecType }
     * 
     */
    public AmdSecType createAmdSecType() {
        return new AmdSecType();
    }

    /**
     * Create an instance of {@link StructMapType }
     * 
     */
    public StructMapType createStructMapType() {
        return new StructMapType();
    }

    /**
     * Create an instance of {@link MetsType.StructLink }
     * 
     */
    public MetsType.StructLink createMetsTypeStructLink() {
        return new MetsType.StructLink();
    }

    /**
     * Create an instance of {@link BehaviorSecType }
     * 
     */
    public BehaviorSecType createBehaviorSecType() {
        return new BehaviorSecType();
    }

    /**
     * Create an instance of {@link BehaviorType }
     * 
     */
    public BehaviorType createBehaviorType() {
        return new BehaviorType();
    }

    /**
     * Create an instance of {@link ParType }
     * 
     */
    public ParType createParType() {
        return new ParType();
    }

    /**
     * Create an instance of {@link FileGrpType }
     * 
     */
    public FileGrpType createFileGrpType() {
        return new FileGrpType();
    }

    /**
     * Create an instance of {@link SeqType }
     * 
     */
    public SeqType createSeqType() {
        return new SeqType();
    }

    /**
     * Create an instance of {@link AreaType }
     * 
     */
    public AreaType createAreaType() {
        return new AreaType();
    }

    /**
     * Create an instance of {@link ObjectType }
     * 
     */
    public ObjectType createObjectType() {
        return new ObjectType();
    }

    /**
     * Create an instance of {@link DivType.Mptr }
     * 
     */
    public DivType.Mptr createDivTypeMptr() {
        return new DivType.Mptr();
    }

    /**
     * Create an instance of {@link DivType.Fptr }
     * 
     */
    public DivType.Fptr createDivTypeFptr() {
        return new DivType.Fptr();
    }

    /**
     * Create an instance of {@link StructLinkType.SmLink }
     * 
     */
    public StructLinkType.SmLink createStructLinkTypeSmLink() {
        return new StructLinkType.SmLink();
    }

    /**
     * Create an instance of {@link StructLinkType.SmLinkGrp.SmLocatorLink }
     * 
     */
    public StructLinkType.SmLinkGrp.SmLocatorLink createStructLinkTypeSmLinkGrpSmLocatorLink() {
        return new StructLinkType.SmLinkGrp.SmLocatorLink();
    }

    /**
     * Create an instance of {@link StructLinkType.SmLinkGrp.SmArcLink }
     * 
     */
    public StructLinkType.SmLinkGrp.SmArcLink createStructLinkTypeSmLinkGrpSmArcLink() {
        return new StructLinkType.SmLinkGrp.SmArcLink();
    }

    /**
     * Create an instance of {@link FileType.FLocat }
     * 
     */
    public FileType.FLocat createFileTypeFLocat() {
        return new FileType.FLocat();
    }

    /**
     * Create an instance of {@link FileType.Stream }
     * 
     */
    public FileType.Stream createFileTypeStream() {
        return new FileType.Stream();
    }

    /**
     * Create an instance of {@link FileType.TransformFile }
     * 
     */
    public FileType.TransformFile createFileTypeTransformFile() {
        return new FileType.TransformFile();
    }

    /**
     * Create an instance of {@link FileType.FContent.XmlData }
     * 
     */
    public FileType.FContent.XmlData createFileTypeFContentXmlData() {
        return new FileType.FContent.XmlData();
    }

    /**
     * Create an instance of {@link MetsType.FileSec.FileGrp }
     * 
     */
    public MetsType.FileSec.FileGrp createMetsTypeFileSecFileGrp() {
        return new MetsType.FileSec.FileGrp();
    }

    /**
     * Create an instance of {@link MdSecType.MdRef }
     * 
     */
    public MdSecType.MdRef createMdSecTypeMdRef() {
        return new MdSecType.MdRef();
    }

    /**
     * Create an instance of {@link MdSecType.MdWrap.XmlData }
     * 
     */
    public MdSecType.MdWrap.XmlData createMdSecTypeMdWrapXmlData() {
        return new MdSecType.MdWrap.XmlData();
    }

    /**
     * Create an instance of {@link MetsType.MetsHdr.Agent }
     * 
     */
    public MetsType.MetsHdr.Agent createMetsTypeMetsHdrAgent() {
        return new MetsType.MetsHdr.Agent();
    }

    /**
     * Create an instance of {@link MetsType.MetsHdr.AltRecordID }
     * 
     */
    public MetsType.MetsHdr.AltRecordID createMetsTypeMetsHdrAltRecordID() {
        return new MetsType.MetsHdr.AltRecordID();
    }

    /**
     * Create an instance of {@link MetsType.MetsHdr.MetsDocumentID }
     * 
     */
    public MetsType.MetsHdr.MetsDocumentID createMetsTypeMetsHdrMetsDocumentID() {
        return new MetsType.MetsHdr.MetsDocumentID();
    }

}
