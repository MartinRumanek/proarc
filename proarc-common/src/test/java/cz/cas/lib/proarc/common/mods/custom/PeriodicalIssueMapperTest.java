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
package cz.cas.lib.proarc.common.mods.custom;

import cz.cas.lib.proarc.common.mods.Mods33Utils;
import cz.cas.lib.proarc.common.mods.custom.IdentifierMapper.IdentifierItem;
import cz.cas.lib.proarc.common.mods.custom.PeriodicalIssueMapper.PeriodicalIssue;
import cz.fi.muni.xkremser.editor.server.mods.ModsType;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Jan Pokorsky
 */
public class PeriodicalIssueMapperTest {

    public PeriodicalIssueMapperTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testRead() throws Exception {
        ModsType mods = Mods33Utils.unmarshal(PeriodicalIssueMapperTest.class.getResource("issue_mods.xml"), ModsType.class);
        PeriodicalIssueMapper instance = new PeriodicalIssueMapper();
        PeriodicalIssue result = instance.map(mods);
        assertNotNull(result);
        assertEquals("1", result.getIssueNumber());
        assertEquals("2", result.getIssueSortingNumber());
        assertEquals("16.12.1893", result.getIssueDate());
        assertEquals("note", result.getNote());
    }

    @Test
    public void testWriteRead() throws Exception {
        ModsType mods = new ModsType();
        PeriodicalIssue value = new PeriodicalIssue();
        value.setIssueNumber("1");
        value.setIssueSortingNumber("2");
        value.setIssueDate("16.12.1893");
        value.setNote("note");
        value.setIdentifiers(Arrays.asList(new IdentifierMapper.IdentifierItem(null, "uuid", "1")));

        PeriodicalIssueMapper instance = new PeriodicalIssueMapper();
        instance.map(mods, value);

        String dump = Mods33Utils.toXml(mods, true);
//        System.out.println(dump);

        PeriodicalIssue result = instance.map(Mods33Utils.unmarshal(dump, ModsType.class));
        assertNotNull(result);
        // XXX issue 43: sotingNumber == number
        assertEquals(value.getIssueSortingNumber(), result.getIssueNumber());
        assertEquals(value.getIssueSortingNumber(), result.getIssueSortingNumber());
        assertEquals(value.getIssueDate(), result.getIssueDate());
        assertEquals(value.getNote(), result.getNote());
        assertEquals(Arrays.asList(new IdentifierItem(0, "uuid", "1")), result.getIdentifiers());
    }

}
