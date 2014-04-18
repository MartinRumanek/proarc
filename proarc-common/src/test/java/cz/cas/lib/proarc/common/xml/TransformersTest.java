/*
 * Copyright (C) 2012 Jan Pokorsky
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
package cz.cas.lib.proarc.common.xml;

import cz.cas.lib.proarc.common.mods.Mods33Utils;
import cz.cas.lib.proarc.common.mods.ModsUtils;
import cz.cas.lib.proarc.common.mods.custom.PageMapperTest;
import cz.fi.muni.xkremser.editor.server.mods.ModsType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamSource;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;

/**
 *
 * @author Jan Pokorsky
 */
public class TransformersTest {

    private static final Logger LOG = Logger.getLogger(TransformersTest.class.getName());
    private static ProxySelector defaultProxy;
    private static List<URI> externalConnections;

    public TransformersTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        defaultProxy = ProxySelector.getDefault();
        // detect external connections
        ProxySelector.setDefault(new ProxySelector() {

            @Override
            public List<Proxy> select(URI uri) {
                externalConnections.add(uri);
                return defaultProxy.select(uri);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                defaultProxy.connectFailed(uri, sa, ioe);
            }
        });
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        externalConnections = new ArrayList<URI>();
        XMLUnit.setIgnoreWhitespace(true);
    }

    @After
    public void tearDown() {
        assertTrue(externalConnections.toString(), externalConnections.isEmpty());
        XMLUnit.setIgnoreWhitespace(false);
        XMLUnit.setNormalizeWhitespace(false);
    }

    @Test
    public void testMarcAsMods() throws Exception {
        XMLUnit.setNormalizeWhitespace(true);
        InputStream goldenIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsMods.xml");
        assertNotNull(goldenIS);
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsMarcXml.xml");// from test
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers();

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.MarcxmlAsMods34);
            assertNotNull(contents);
//            System.out.println(new String(contents, "UTF-8"));
            XMLAssert.assertXMLEqual(new InputSource(goldenIS), new InputSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
            close(goldenIS);
        }
    }

    @Test
    public void testOaiMarcAsMarc() throws Exception {
        InputStream goldenIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsMarcXml.xml");
        assertNotNull(goldenIS);
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsOaiMarc.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers();

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.OaimarcAsMarc21slim);
            assertNotNull(contents);
//            System.out.println(new String(contents, "UTF-8"));
            XMLAssert.assertXMLEqual(new InputSource(goldenIS), new InputSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
            close(goldenIS);
        }
    }

    @Test
    public void testAlephXServerDetailNamespaceFix() throws Exception {
        InputStream goldenIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseFixed.xml");
        assertNotNull(goldenIS);
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("../catalog/alephXServerDetailResponse.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers();

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.AlephOaiMarcFix);
            assertNotNull(contents);
//            System.out.println(new String(contents, "UTF-8"));
            XMLAssert.assertXMLEqual(new InputSource(goldenIS), new InputSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
            close(goldenIS);
        }
    }

    @Test
    public void testModsAsHtml() throws Exception {
//        XMLUnit.setNormalizeWhitespace(true);
//        InputStream goldenIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsMods.xml");
//        assertNotNull(goldenIS);
        InputStream xmlIS = TransformersTest.class.getResourceAsStream("alephXServerDetailResponseAsMods.xml");
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers();
        Map<String, Object> params = ModsUtils.modsAsHtmlParameters(Locale.ENGLISH);

        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.ModsAsHtml, params);
            assertNotNull(contents);
            System.out.println(new String(contents, "UTF-8"));
//            XMLAssert.assertXMLEqual(new InputSource(goldenIS), new InputSource(new ByteArrayInputStream(contents)));
        } finally {
            close(xmlIS);
//            close(goldenIS);
        }
    }

    @Test
    public void testModsAsFedoraLabel_Page() throws Exception {
        assertEquals("[1], Blank",
                modsAsFedoraLabel(PageMapperTest.class.getResourceAsStream("page_mods.xml"), "model:page"));
    }

    @Test
    public void testModsAsFedoraLabel_Issue() throws Exception {
        assertEquals("1",
                modsAsFedoraLabel(PageMapperTest.class.getResourceAsStream("issue_mods.xml"), "model:periodicalitem"));
    }

    @Test
    public void testModsAsFedoraLabel_Volume() throws Exception {
        assertEquals("1893, 1",
                modsAsFedoraLabel(PageMapperTest.class.getResourceAsStream("volume_mods.xml"), "model:periodicalvolume"));
    }

    @Test
    public void testModsAsFedoraLabel_Periodical() throws Exception {
        assertEquals("MTITLE[0]: STITLE[0]",
                modsAsFedoraLabel(PageMapperTest.class.getResourceAsStream("periodical_mods.xml"), "model:periodical"));
    }

    @Test
    public void testModsAsFedoraLabel_Empty() throws Exception {
        String label = Mods33Utils.getLabel(new ModsType(), "model:page");
        assertEquals("?", label);
    }

    private String modsAsFedoraLabel(InputStream xmlIS, String model) throws Exception {
        assertNotNull(xmlIS);
        StreamSource streamSource = new StreamSource(xmlIS);
        Transformers mt = new Transformers();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("MODEL", model);
        try {
            byte[] contents = mt.transformAsBytes(streamSource, Transformers.Format.ModsAsFedoraLabel, params);
            assertNotNull(contents);
            String label = new String(contents, "UTF-8");
            System.out.println(label);
            return label;
        } finally {
            close(xmlIS);
        }
    }

    private static void close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

}
