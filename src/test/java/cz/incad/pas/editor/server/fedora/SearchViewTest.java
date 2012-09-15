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
package cz.incad.pas.editor.server.fedora;

import cz.incad.pas.editor.server.fedora.SearchView.Item;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Jan Pokorsky
 */
public class SearchViewTest {

    private RemoteStorage storage;
    private FedoraTestSupport fedora;

    public SearchViewTest() {
    }

    @Before
    public void setUp() throws Exception {
        fedora = new FedoraTestSupport();
        storage = fedora.getRemoteStorage();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFindLastCreated() throws Exception {
        // XXX needs some assertions; it tests no exception now
//        client.debug(true);
        String user = "model:periodical";
//        String user = null;
        SearchView instance = new SearchView(storage);
        List<Item> result = instance.findLastCreated(0, user);
        System.out.println(result);
    }

    @Test
    public void testNormalizePhrase() {
        assertEquals("*query*", SearchView.normalizePhrase("*query*"));
        assertEquals("*query*", SearchView.normalizePhrase("*query"));
        assertEquals("*query*", SearchView.normalizePhrase("query*"));
        assertEquals("*query*", SearchView.normalizePhrase(" * query* **"));
        assertEquals("*", SearchView.normalizePhrase(" ***"));
        assertEquals("*", SearchView.normalizePhrase(""));
        assertEquals("*", SearchView.normalizePhrase(null));
    }

    @Test
    public void tetFindQuery() throws Exception {
//        client.debug(true);
        SearchView instance = new SearchView(storage);
        List<Item> result = instance.findQuery("p", "p", "u", "p", "model:periodical");
        assertFalse(result.isEmpty());
    }

    @Test
    public void tetFindModelQuery() throws Exception {
//        client.debug(true);
        SearchView instance = new SearchView(storage);
        List<Item> result = instance.findQuery(null, null, null, null, "model:periodical");
        assertFalse(result.isEmpty());
    }
}
