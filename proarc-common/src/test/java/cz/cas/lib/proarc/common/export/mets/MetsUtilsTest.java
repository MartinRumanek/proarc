/*
 * Copyright (C) 2013 Robert Simonovsky
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

package cz.cas.lib.proarc.common.export.mets;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import net.lingala.zip4j.core.ZipFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.yourmediashelf.fedora.generated.foxml.DigitalObject;

import cz.cas.lib.proarc.common.export.mets.structure.MetsEntity;
import cz.cas.lib.proarc.common.export.mets.structure.MetsInfo;
import cz.cas.lib.proarc.info.Info;
import cz.cas.lib.proarc.mets.Mets;

public class MetsUtilsTest {
    private static final Logger LOG = Logger.getLogger(MetsUtilsTest.class.getName());
    private final List<MetsExportTestElement> testElements = new ArrayList<MetsExportTestElement>();

    private static HashMap<String, String> parents = new HashMap<String, String>();
    static {
        parents.put("uuid:b46ab0eb-26af-11e3-88e3-001b63bd97ba", "uuid:2ad73b97-ef9d-429a-b3a5-65083fa4c317");
        parents.put("uuid:b46aff0c-26af-11e3-88e3-001b63bd97ba", "uuid:2ad73b97-ef9d-429a-b3a5-65083fa4c317");
        parents.put("uuid:b46aff0d-26af-11e3-88e3-001b63bd97ba", "uuid:2ad73b97-ef9d-429a-b3a5-65083fa4c317");
        parents.put("uuid:b46aff0e-26af-11e3-88e3-001b63bd97ba", "uuid:2ad73b97-ef9d-429a-b3a5-65083fa4c317");
        parents.put("uuid:b46aff0f-26af-11e3-88e3-001b63bd97ba", "uuid:2ad73b97-ef9d-429a-b3a5-65083fa4c317");
        parents.put("uuid:b46b2620-26af-11e3-88e3-001b63bd97ba", "uuid:2ad73b97-ef9d-429a-b3a5-65083fa4c317");
        parents.put("uuid:b46b2621-26af-11e3-88e3-001b63bd97ba", "uuid:2ad73b97-ef9d-429a-b3a5-65083fa4c317");
        parents.put("uuid:b46b2622-26af-11e3-88e3-001b63bd97ba", "uuid:2ad73b97-ef9d-429a-b3a5-65083fa4c317");
        parents.put("uuid:2ad73b97-ef9d-429a-b3a5-65083fa4c317", "uuid:90ca85a1-beb8-4b5d-a320-b4b7ac5c8df5");
        parents.put("uuid:90ca85a1-beb8-4b5d-a320-b4b7ac5c8df5", "uuid:3733b6e3-61ab-42fc-a437-964d143acc45");
        parents.put("uuid:2ff2dd0c-d438-4d95-940f-690ee0f44a4a", "uuid:44589055-9fad-4a9f-b6a8-75be399f332d");
        parents.put("uuid:44589055-9fad-4a9f-b6a8-75be399f332d", "uuid:1ccbf6c5-b22c-4d89-b42e-8cd14101a737");
    }

    /**
     * Inits the elements to test - documents which are in repository
     * 
     */
    private void initTestElements() {
        MetsExportTestElement monografieTestElement = new MetsExportTestElement("monograph.zip", "monograph", 6, 15, 5, "monograph", "1ccbf6c5-b22c-4d89-b42e-8cd14101a737.xml");
        this.testElements.add(monografieTestElement);
        MetsExportTestElement periodikumTestElement = new MetsExportTestElement("periodikum.zip", "periodikum", 42, 323, 5, "periodical", "3733b6e3-61ab-42fc-a437-964d143acc45.xml");
        this.testElements.add(periodikumTestElement);
        MetsExportTestElement periodikumPageTestElement = new MetsExportTestElement("periodikum.zip", "periodikumPage", 7, 39, 5, "periodical", "b46aff0e-26af-11e3-88e3-001b63bd97ba.xml");
        this.testElements.add(periodikumPageTestElement);
    }

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void setUp() {
        initTestElements();
    }

    /**
     * 
     * Simple test for ModName conversion
     * 
     * @throws Exception
     */
    @Test
    public void getModNameTest() throws Exception {
        String modName = MetsUtils.getModName(Const.PERIODICAL_VOLUME);
        assertEquals(modName, "VOLUME");
    }

    /**
     * Copies the files for a test from jar file to a temporary file system
     * 
     * @param testElement
     */
    private void copyFiles(MetsExportTestElement testElement) {
        File destination = null;
        try {
            destination = tmp.newFolder(testElement.getDirectory());
        } catch (IOException ex) {
            throw new RuntimeException("Unable to create folder: " + testElement.getDirectory());
        }
        InputStream is = this.getClass().getResourceAsStream(testElement.getZipFile());
        String zipFileLocation = tmp.getRoot().getAbsolutePath() + File.separator + testElement.getZipFile();
        File zipFile = new File(zipFileLocation);
        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            MetsUtils.copyStream(is, fos);
            ZipFile zip = new ZipFile(zipFile);
            zip.extractAll(destination.getAbsolutePath());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Reads and FoXML document from file system and gets its PID
     * 
     * @throws Exception
     */
    @Test
    public void readFoXMLTest() throws Exception {
        for (MetsExportTestElement element : testElements) {
            copyFiles(element);
            String path = tmp.getRoot().getAbsolutePath() + File.separator + element.getDirectory() + File.separator + element.getInitialDocument();
            DigitalObject dbObj = MetsUtils.readFoXML(path);
            LOG.log(Level.INFO, dbObj.getPID());
            String fileName = element.getInitialDocument();
            assertEquals(dbObj.getPID().substring(dbObj.getPID().indexOf(":") + 1), fileName.substring(0, fileName.indexOf(".")));
        }
    }

    /**
     * 
     * Saves a mets document and test it for different parameters (size, number
     * of files, ...)
     * 
     * @throws Exception
     */
    @Test
    public void saveMetsTest() throws Exception {
        for (MetsExportTestElement testElement : testElements) {
            copyFiles(testElement);
            String sourceDirPath = tmp.getRoot().getAbsolutePath() + File.separator + testElement.getDirectory() + File.separator;
            File resultDir = tmp.newFolder("result" + testElement.getDirectory());
            String path = sourceDirPath + testElement.getInitialDocument();
            DigitalObject dbObj = MetsUtils.readFoXML(path);
            MetsInfo metsInfo = new MetsEntity(dbObj, sourceDirPath, "SAMPLE", parents);
            metsInfo.insertIntoMets(resultDir.getAbsolutePath(), true);
            metsInfo.save();
            File infoFile = new File(resultDir.getAbsolutePath() + File.separator + "info.xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(Info.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Info info = (Info) unmarshaller.unmarshal(infoFile);
            assertEquals(testElement.getTotalItems(), info.getItemlist().getItemtotal().intValue());
            assertEquals(testElement.getSize(), info.getSize());
            File metsFile = new File(resultDir.getAbsolutePath() + File.separator + "METS_SAMPLE.xml");
            JAXBContext jaxbContextMets = JAXBContext.newInstance(Mets.class);
            Unmarshaller unmarshallerMets = jaxbContextMets.createUnmarshaller();
            Mets mets = (Mets) unmarshallerMets.unmarshal(metsFile);
            assertEquals(testElement.getNumberOfFiles(), mets.getFileSec().getFileGrp().size());
            assertEquals(testElement.getType(), mets.getTYPE());
        }
    }
}
