/*
 * Copyright (C) 2011 Jan Pokorsky
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.pas.editor.server.fedora;

import com.yourmediashelf.fedora.generated.foxml.ContentLocationType;
import com.yourmediashelf.fedora.generated.foxml.DatastreamType;
import com.yourmediashelf.fedora.generated.foxml.DatastreamVersionType;
import com.yourmediashelf.fedora.generated.foxml.DigitalObjectType;
import com.yourmediashelf.fedora.generated.foxml.XmlContentType;
import cz.incad.pas.oaidublincore.ElementType;
import cz.incad.pas.oaidublincore.OaiDcType;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

/**
 *
 * @author Jan Pokorsky
 */
public final class DigitalObjectRepository {

    private static final Logger LOG = Logger.getLogger(DigitalObjectRepository.class.getName());

    private static final DigitalObjectRepository INSTANCE = new DigitalObjectRepository();

    // 4a7c2e50-af36-11dd-9643-000d606f5dc6 Drobnustky page
//    private static final String TMP_REPOSITORY_FOLDER = "/home/honza/Documents/Incad/kramerius4/fedora/Install/Drobnustky-foxml-import/%s.xml";
    private static final String TMP_REPOSITORY_FOLDER = "/home/honza/Downloads/40114/%s.xml";
//    private static final String TMP_REPOSITORY_FOLDER = "/fast/paseditor/40114/%s.xml";

    private final  Map<String, DigitalObjectRecord> memoryImpl = new HashMap<String, DigitalObjectRecord>();

    public static DigitalObjectRepository getInstance() {
        return INSTANCE;
    }

    public DublinCoreRecord getDc(String pid) {
        DigitalObjectRecord doRecord;
        synchronized(memoryImpl) {
            doRecord = getDigitalObjectRecord(pid);
            DublinCoreRecord dcRecord = doRecord.getDc();
            if (dcRecord != null) {
                return dcRecord;
            }
        }

        OaiDcType dc = findDublinCore(pid);

        synchronized(memoryImpl) {
            DublinCoreRecord dcRecord = doRecord.getDc();
            if (dcRecord == null) {
                dcRecord = new DublinCoreRecord(dc, System.currentTimeMillis(), pid);
                doRecord.setDc(dcRecord);
            }
            return dcRecord;
        }
    }

    public void updateDc(DublinCoreRecord dcRecord, int user) {
        synchronized(memoryImpl) {
            DigitalObjectRecord doRecord = getDigitalObjectRecord(dcRecord.pid);
            DublinCoreRecord dcRecordOld = doRecord.getDc();
            if (dcRecordOld != null) {
                if (dcRecordOld.timestamp > dcRecord.timestamp) {
                    throw new IllegalStateException("Dublin Core already modified: " + dcRecord.pid);
                }
                dcRecord.timestamp = System.currentTimeMillis();
                doRecord.setDc(dcRecord);
            } else {
                throw new IllegalStateException("PID not found: " + dcRecord.pid);
            }
        }
    }

    public DatastreamVersionType getPreview(String pid) {
//        DatastreamVersionType ds = findDataStream(pid, "IMG_THUMB");
        DatastreamVersionType ds = findDataStream(pid, "IMG_PREVIEW");
        return ds;
    }

    public DatastreamVersionType getThumbnail(String pid) {
        DatastreamVersionType ds = findDataStream(pid, "IMG_THUMB");
        return ds;
    }

    public OcrRecord getOcr(String pid) {
        DigitalObjectRecord doRecord;
        synchronized(memoryImpl) {
            doRecord = getDigitalObjectRecord(pid);
            OcrRecord ocrRecord = doRecord.getOcr();
            if (ocrRecord != null) {
                return ocrRecord;
            }
        }

        DatastreamVersionType ds = findDataStream(pid, "TEXT_OCR");

        synchronized(memoryImpl) {
            OcrRecord ocrRecord = doRecord.getOcr();
            if (ocrRecord == null) {
                String ocr = new String(ds.getBinaryContent());
                // Browser converts line endings to '\n' that results to changed data.
                ocr = ocr.replaceAll("\\r\\n|\\r", "\n");
                ocrRecord = new OcrRecord(ocr, System.currentTimeMillis(), pid);
                doRecord.setOcr(ocrRecord);
            }
            return ocrRecord;
        }

    }

    public void updateOcr(OcrRecord ocrRecord, int user) {
        synchronized(memoryImpl) {
            DigitalObjectRecord doRecord = getDigitalObjectRecord(ocrRecord.pid);
            OcrRecord ocrRecordOld = doRecord.getOcr();
            LOG.info(ocrRecord.ocr);
            if (ocrRecordOld != null) {
                if (ocrRecordOld.timestamp > ocrRecord.timestamp) {
                    throw new IllegalStateException("OCR already modified: " + ocrRecord.pid);
                }
                ocrRecord.timestamp = System.currentTimeMillis();
                doRecord.setOcr(ocrRecord);
            } else {
                throw new IllegalStateException("PID not found: " + ocrRecord.pid);
            }
        }
    }

    private DigitalObjectRecord getDigitalObjectRecord(String pid) {
        synchronized(memoryImpl) {
            DigitalObjectRecord doRecord = memoryImpl.get(pid);
            if (doRecord == null) {
                doRecord = new DigitalObjectRecord(pid, null, null);
                memoryImpl.put(pid, doRecord);
            }
            return doRecord;
        }
    }

    OaiDcType findDublinCore(String pid) {
        try {
            return findDublinCoreImpl(pid);
        } catch (JAXBException ex) {
            throw new IllegalStateException(pid);
        }
    }

    private DatastreamVersionType findDataStream(String pid, String dsId) {
        String uuid = getUuid(pid);
        DigitalObjectType fdobj = readFoxml(uuid);
        DatastreamVersionType datastreamVersion = null;
        if (fdobj != null) {
            DatastreamType datastream = findDatastream(fdobj, dsId);
            if (datastream != null) {
                datastreamVersion = findDatastreamVersion(datastream);
            }
        }
        return datastreamVersion;
    }

    private OaiDcType findDublinCoreImpl2(String pid) throws JAXBException {
        DatastreamVersionType ds = findDataStream(pid, "DC");
        XmlContentType xml = ds.getXmlContent();
        Element elm = xml.getAny().get(0);
        OaiDcType dc = JAXB.unmarshal(new DOMSource(elm), OaiDcType.class);
        return dc;
    }

    private OaiDcType findDublinCoreImpl(String pid) throws JAXBException {
        String uuid = getUuid(pid);
        DigitalObjectType fdobj = readFoxml(uuid);
        String result = fdobj.getPID();
        System.out.println("## result: " + result);

        for (DatastreamType datastream : fdobj.getDatastream()) {
            String dsId = datastream.getID();
            String fedoraUri = datastream.getFEDORAURI();
            System.out.printf("DatastreamType: id: %s, fedoraUri: %s\n", dsId, fedoraUri);
            for (DatastreamVersionType datastreamVersion : datastream.getDatastreamVersion()) {
                String dsVerId = datastreamVersion.getID();
                String formatUri = datastreamVersion.getFORMATURI();
                String mimetype = datastreamVersion.getMIMETYPE();
                XMLGregorianCalendar created = datastreamVersion.getCREATED();
                ContentLocationType contentLocation = datastreamVersion.getContentLocation();
                XmlContentType xmlContent = datastreamVersion.getXmlContent();
                byte[] binaryContent = datastreamVersion.getBinaryContent();
                System.out.printf("  DatastreamVersionType id: %s, formatUri: %s,"
                        + " mimetype: %s, created: %s, contentLocation: %s,"
                        + "\n    xmlContent: %s, binaryContent: %s\n",
                        dsVerId, formatUri, mimetype, created, contentLocation, xmlContent, binaryContent);
                if (xmlContent != null) {
                    for (Element element : xmlContent.getAny()) {
                        System.out.printf("  xml.element: %s\n", element);
                        if ("DC".equals(dsId)) {
                            String stringFromNode = getStringFromNode(element);
                            System.out.printf("  xml.element.content: %s\n", stringFromNode);
                            OaiDcType dc = JAXB.unmarshal(new DOMSource(element), OaiDcType.class);
                            for (JAXBElement<ElementType> dcElm : dc.getTitleOrCreatorOrSubject()) {
                                QName name = dcElm.getName();
                                String value = dcElm.getValue().getValue();
                                String lang = dcElm.getValue().getLang();
                                System.out.printf("  dc.%s, value: %s, lang: %s\n", name, value, lang);
                            }
                            return dc;
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("Something is broken: " + pid);
    }

    private static DigitalObjectType readFoxml(String uuid) {
        File file = new File(String.format(TMP_REPOSITORY_FOLDER, uuid));
        DigitalObjectType fdobj = JAXB.unmarshal(file, DigitalObjectType.class);
        return fdobj;
    }

    private static DatastreamType findDatastream(DigitalObjectType digitalObject, String dsId) {
        for (DatastreamType datastream : digitalObject.getDatastream()) {
            String id = datastream.getID();
            String fedoraUri = datastream.getFEDORAURI();
            System.out.printf("DatastreamType: id: %s, fedoraUri: %s\n", id, fedoraUri);
            if (dsId.equals(id)) {
                return datastream;
            }
        }
        return null;
    }

    private static DatastreamVersionType findDatastreamVersion(DatastreamType datastream) {
        for (DatastreamVersionType datastreamVersion : datastream.getDatastreamVersion()) {
            String dsVerId = datastreamVersion.getID();
            // XXX find the last one
            return datastreamVersion;
        }
        return null;
    }

    private String getUuid(String pid) {
        return pid.substring("uuid:".length());
    }

    private static String getStringFromNode(Node node) {
        DOMImplementationLS domImplementation =
                (DOMImplementationLS) node.getOwnerDocument().getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        return lsSerializer.writeToString(node);
    }

    @XmlRootElement(name="dcRecord", namespace="http://www.incad.cz/pas/editor/dor/")
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(namespace="http://www.incad.cz/pas/editor/dor/")
    public static class DublinCoreRecord {

        @XmlElement(name="pid", namespace="http://www.incad.cz/pas/editor/dor/")
        private String pid;
        /** last modification of the DC content*/

        @XmlElement(name="timestamp", namespace="http://www.incad.cz/pas/editor/dor/")
        private long timestamp;

        @XmlElement(namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/", name = "dc", required = true)
        private OaiDcType dc;

        public DublinCoreRecord() {
        }

        public DublinCoreRecord(OaiDcType dc, long timestamp, String pid) {
            this.dc = dc;
            this.timestamp = timestamp;
            this.pid = pid;
        }

    }

    @XmlRootElement(name = "ocrRecord")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OcrRecord {

//        @XmlElement(name="pid", namespace="http://www.incad.cz/pas/editor/dor/")
        private String pid;
        /** last modification of the DC content*/

//        @XmlElement(name="timestamp", namespace="http://www.incad.cz/pas/editor/dor/")
        private long timestamp;

//        @XmlElement(namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/", name = "dc", required = true)
        private String ocr;

        public OcrRecord() {
        }

        public OcrRecord(String ocr, long timestamp, String pid) {
            this.ocr = ocr;
            this.timestamp = timestamp;
            this.pid = pid;
        }
    }

    public static class DigitalObjectRecord {
        private String pid;
        private DublinCoreRecord dc;
        private OcrRecord ocr;

        public DigitalObjectRecord(String pid, DublinCoreRecord dc, OcrRecord ocr) {
            this.pid = pid;
            this.dc = dc;
            this.ocr = ocr;
        }

        public DublinCoreRecord getDc() {
            return dc;
        }

        public void setDc(DublinCoreRecord dc) {
            this.dc = dc;
        }

        public OcrRecord getOcr() {
            return ocr;
        }

        public void setOcr(OcrRecord ocr) {
            this.ocr = ocr;
        }

    }

}
