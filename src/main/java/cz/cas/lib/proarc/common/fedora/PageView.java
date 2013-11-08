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
package cz.cas.lib.proarc.common.fedora;

import cz.cas.lib.proarc.common.dublincore.DcStreamEditor;
import cz.cas.lib.proarc.common.fedora.LocalStorage.LocalObject;
import cz.cas.lib.proarc.common.fedora.relation.RelationEditor;
import cz.cas.lib.proarc.common.imports.ImportBatchManager.BatchItemObject;
import cz.cas.lib.proarc.common.mods.ModsStreamEditor;
import cz.cas.lib.proarc.common.mods.ModsUtils;
import cz.cas.lib.proarc.common.mods.custom.PageMapper;
import cz.cas.lib.proarc.common.mods.custom.PageMapper.Page;
import cz.fi.muni.xkremser.editor.server.mods.ModsType;
import cz.incad.pas.editor.shared.rest.DigitalObjectResourceApi;
import cz.incad.pas.editor.shared.rest.ImportResourceApi;
import cz.incad.pas.editor.shared.rest.LocalizationResourceApi;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author Jan Pokorsky
 */
public final class PageView {

    public List<Item> list(int batchId, Collection<BatchItemObject> imports) throws DigitalObjectException {
        ArrayList<Item> result = new ArrayList<Item>(imports.size());
        LocalStorage storage = new LocalStorage();
//        Mapping mapping = new Mapping();
        PageMapper mapper = new PageMapper();
        for (BatchItemObject imp : imports) {
            File foxml = imp.getFile();
            LocalObject local = storage.load(imp.getPid(), foxml);
            ModsStreamEditor editor = new ModsStreamEditor(local);
            Page page = mapper.map(editor.read());
//            Object custom = mapping.read(record.getMods(), MetaModelDataSource.EDITOR_PAGE);
            RelationEditor relsExt = new RelationEditor(local);
            String model = relsExt.getModel();
            String filename = relsExt.getImportFile();
            result.add(new Item(batchId, filename, imp.getPid(),
                    model, page.getIndex(), page.getNumber(), page.getType(),
                    editor.getLastModified(), local.getOwner(), local.getLabel()));
        }
        return result;
    }

    public Item updateItem(int batchId, BatchItemObject item, long timestamp, String message,
            String pageIndex, String pageNumber, String pageType)
            throws DigitalObjectException {
        
        LocalStorage storage = new LocalStorage();
//        PageMapper mapper = new PageMapper();
        LocalObject local = storage.load(item.getPid(), item.getFile());

        // MODS
        ModsStreamEditor editor = new ModsStreamEditor(local);
        ModsType mods = editor.read();
        editor.updatePage(mods, pageIndex, pageNumber, pageType);
        editor.write(mods, timestamp, message);

        RelationEditor relsExt = new RelationEditor(local);
        String model = relsExt.getModel();
        String filename = relsExt.getImportFile();

        // DC
        DcStreamEditor dcEditor = new DcStreamEditor(local);
        dcEditor.write(mods, model, dcEditor.getLastModified(), message);

        local.setLabel(ModsUtils.getLabel(mods, model));

        local.flush();
        Item update = new Item(batchId, filename, item.getPid(), model,
                pageIndex, pageNumber, pageType,
                editor.getLastModified(), local.getOwner(), local.getLabel());
        return update;
    }

    private static ResourceBundle getPageTypeTitles(Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle(
                LocalizationResourceApi.BundleName.MODS_PAGE_TYPES.toString(), locale);
        return rb;
    }

    /**
     * Gets localized label of fedora object containing page.
     * It relies on page label syntax: {@code <pageLabel>, <pageType>}
     *
     * @param label label of fedora object
     * @param locale target locale
     * @return localized label
     *
     * @see ModsUtils#getLabel
     */
    public static String resolveFedoraObjectLabel(String label, Locale locale) {
        ResourceBundle pageTypeTitles = getPageTypeTitles(locale);
        int typeIndex = label.lastIndexOf(", ") + 2;
        if (typeIndex - 2 >= 0 && typeIndex < label.length()) {
            String typeCode = label.substring(typeIndex);
            try {
                if (!typeCode.isEmpty()) {
                    String typeName = pageTypeTitles.getString(typeCode);
                    label = label.substring(0, typeIndex) + typeName;
                }
            } catch (MissingResourceException e) {
                // ignore
            }
        }
        return label;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Item {

        @XmlElement(name = ImportResourceApi.BATCHITEM_BATCHID)
        private Integer batchId;
        @XmlElement(name = ImportResourceApi.BATCHITEM_FILENAME)
        private String filename;
        @XmlElement(name = ImportResourceApi.BATCHITEM_PID)
        private String pid;
        @XmlElement(name = ImportResourceApi.BATCHITEM_MODEL)
        private String model;
        @XmlElement(name = ImportResourceApi.BATCHITEM_PAGEINDEX)
        private String pageIndex;
        @XmlElement(name = ImportResourceApi.BATCHITEM_PAGENUMBER)
        private String pageNumber;
        @XmlElement(name = ImportResourceApi.BATCHITEM_PAGETYPE)
        private String pageType;
        @XmlElement(name = ImportResourceApi.BATCHITEM_TIMESTAMP)
        private long timestamp;
        @XmlElement(name = ImportResourceApi.BATCHITEM_USER)
        private String user;
        @XmlElement(name = DigitalObjectResourceApi.MEMBERS_ITEM_LABEL)
        private String label;

        public Item(Integer batchId, String filename, String pid, String model,
                String pageIndex, String pageNumber, String pageType,
                long timestamp, String user, String label) {
            this.batchId = batchId;
            this.filename = filename;
            this.pid = pid;
            this.model = model;
            this.pageIndex = pageIndex;
            this.pageNumber = pageNumber;
            this.pageType = pageType;
            this.timestamp = timestamp;
            this.user = user;
            this.label = label;
        }

        public Item() {
        }

    }

}
