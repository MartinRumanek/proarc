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
package cz.incad.pas.editor.server.mods;

import cz.fi.muni.xkremser.editor.server.mods.ModsType;
import cz.incad.pas.editor.server.fedora.LocalStorage;
import cz.incad.pas.editor.server.fedora.LocalStorage.LocalObject;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Jan Pokorsky
 */
public class ModsStreamEditorTest {

    @Test
    public void testRead() throws Exception {
        LocalStorage storage = new LocalStorage();
        LocalObject local = storage.create();
        ModsStreamEditor editor = new ModsStreamEditor(local);
        editor.write(new ModsType(), 0);
        ModsType result = editor.read();
        assertNotNull(result);
    }
    
}
