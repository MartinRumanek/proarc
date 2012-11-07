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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.pas.editor.client.presenter;

import com.google.gwt.place.shared.Place;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import cz.incad.pas.editor.client.ClientMessages;
import cz.incad.pas.editor.client.ClientUtils;
import cz.incad.pas.editor.client.Editor;
import cz.incad.pas.editor.client.action.Actions;
import cz.incad.pas.editor.client.action.DigitalObjectEditAction;
import cz.incad.pas.editor.client.action.RefreshAction;
import cz.incad.pas.editor.client.action.RefreshAction.Refreshable;
import cz.incad.pas.editor.client.action.Selectable;
import cz.incad.pas.editor.client.ds.MetaModelDataSource;
import cz.incad.pas.editor.client.ds.MetaModelDataSource.MetaModelRecord;
import cz.incad.pas.editor.client.ds.RestConfig;
import cz.incad.pas.editor.client.ds.SearchDataSource;
import cz.incad.pas.editor.client.widget.DatastreamEditor;
import cz.incad.pas.editor.client.widget.DigitalObjectParentEditor;
import cz.incad.pas.editor.client.widget.MediaEditor;
import cz.incad.pas.editor.client.widget.TextEditor;
import java.util.HashMap;

/**
 * Edits digital object data streams.
 *
 * @author Jan Pokorsky
 */
public final class DigitalObjectEditor implements Refreshable, Selectable<Record> {

    private final ClientMessages i18n;
    private final VLayout widget;
    private final Label lblHeader;
    private final ToolStrip toolbar;
    private final VLayout editorContainer;
    private EditorDescriptor currentEditor;
    private Record selection;
    private HashMap<Type, EditorDescriptor> editorCache = new HashMap<Type, EditorDescriptor>();

    public DigitalObjectEditor(ClientMessages i18n) {
        this.i18n = i18n;
        this.widget = new VLayout();
        this.lblHeader = new Label();
        lblHeader.setAutoHeight();
        lblHeader.setPadding(4);
        lblHeader.setStyleName("pasWizardTitle");
        this.toolbar = createToolbar();
        this.editorContainer = new VLayout();
        editorContainer.setWidth100();
        editorContainer.setHeight100();
        widget.addMember(lblHeader);
        widget.addMember(toolbar);
        widget.addMember(editorContainer);
    }

    public Canvas getUI() {
        return widget;
    }

    public void edit(Type type, Record selection) {
        this.selection = selection;
        final String pid = selection.getAttribute(SearchDataSource.FIELD_PID);
        final String modelId = selection.getAttribute(SearchDataSource.FIELD_MODEL);

        editorContainer.hide();
        if (type == null) {
            // this should occur just in case someone breakes URL in browser.
            SC.warn("Missing or invalid editor type!");
            Editor.getInstance().getEditorWorkFlow().getPlaceController().goTo(Place.NOWHERE);
            return ;
        }

        MetaModelDataSource.getInstance().fetchData(null, new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (RestConfig.isStatusOk(response)) {
                    MetaModelRecord mr = MetaModelRecord.get(
                            response.getDataAsRecordList().find(MetaModelDataSource.FIELD_PID, modelId));
                    if (mr == null) {
                        throw new IllegalStateException("Invalid model ID: " + modelId);
                    }
                    edit(pid, mr);
                }
            }
        });

        EditorDescriptor previousEditor = currentEditor;
        currentEditor = getDatastreamEditor(type);
        updateToolbar(previousEditor, currentEditor);
    }

    private void edit(String pid, MetaModelRecord mr) {
        setDesctiption(currentEditor.getTitle(), selection, mr);
        DatastreamEditor editor = currentEditor.getEditor();
        editor.edit(pid, null, mr);
        editorContainer.setMembers(editor.getUI());
        editorContainer.show();
    }

    @Override
    public void refresh() {
        Refreshable r = currentEditor.getEditor().getCapability(Refreshable.class);
        if (r != null) {
            r.refresh();
        }
    }

    @Override
    public Record[] getSelection() {
        return new Record[] { selection };
    }

    private void updateToolbar(EditorDescriptor oldEditor, EditorDescriptor newEditor) {
        if (oldEditor != null) {
            toolbar.removeMembers(oldEditor.getToolbarItems());
        }
        Canvas[] customToolbar = newEditor.getToolbarItems();
        for (Canvas item : customToolbar) {
            toolbar.addMember(item);
        }
    }

    private ToolStrip createToolbar() {
        RefreshAction refreshAction = new RefreshAction(i18n);
        DigitalObjectEditAction modsEditAction = new DigitalObjectEditAction(
                i18n.ImportBatchItemEditor_TabMods_Title(), Type.MODS, i18n);
        DigitalObjectEditAction ocrEditAction = new DigitalObjectEditAction(
                i18n.ImportBatchItemEditor_TabOcr_Title(), Type.OCR, i18n);
        DigitalObjectEditAction noteEditAction = new DigitalObjectEditAction(
                i18n.ImportBatchItemEditor_TabNote_Title(), Type.NOTE, i18n);
        DigitalObjectEditAction parentEditAction = new DigitalObjectEditAction(
                i18n.DigitalObjectEditor_ParentAction_Title(), Type.PARENT, i18n);
        DigitalObjectEditAction mediaEditAction = new DigitalObjectEditAction(
                i18n.DigitalObjectEditor_MediaAction_Title(),
                i18n.DigitalObjectEditor_MediaAction_Hint(),
                Type.MEDIA);
        ToolStrip t = Actions.createToolStrip();
        t.addMember(Actions.asIconButton(refreshAction, this));
        t.addMember(Actions.asIconButton(modsEditAction, this));
        t.addMember(Actions.asIconButton(ocrEditAction, this));
        t.addMember(Actions.asIconButton(noteEditAction, this));
        t.addMember(Actions.asIconButton(parentEditAction, this));
        t.addMember(Actions.asIconButton(mediaEditAction, this));
        return t;
    }

    private EditorDescriptor getDatastreamEditor(Type type) {
        EditorDescriptor desc = editorCache.get(type);
        if (desc != null) {
            return desc;
        }
        DatastreamEditor deditor = null;
        String title = "";
        switch (type) {
            case OCR:
                title = i18n.ImportBatchItemEditor_TabOcr_Title();
                deditor = TextEditor.ocr(i18n);
                break;
            case NOTE:
                title = i18n.ImportBatchItemEditor_TabNote_Title();
                deditor = TextEditor.note(i18n);
                break;
            case MEDIA:
                title = i18n.DigitalObjectEditor_MediaEditor_Title();
                deditor = new MediaEditor(i18n);
                break;
            case MODS:
                title = i18n.ImportBatchItemEditor_TabMods_Title();
                deditor = new ModsFullEditor(i18n);
                break;
            case PARENT:
                title = i18n.DigitalObjectEditor_ParentEditor_Title();
                deditor = new DigitalObjectParentEditor(i18n);
                break;
        }
        title = ClientUtils.format("<b>%s</b>", title);
        desc = new EditorDescriptor(deditor, title);
        editorCache.put(type, desc);
        return desc;
    }

    private void setDesctiption(String editorTitle, Record r, MetaModelRecord mr) {
        // Editor Name - Model - Label
        String label = r.getAttribute(SearchDataSource.FIELD_LABEL);
        String modelId = r.getAttribute(SearchDataSource.FIELD_MODEL);
        String model = mr == null ? modelId : mr.getDisplayName();
        String pid = r.getAttribute(SearchDataSource.FIELD_PID);
        String content = ClientUtils.format("%s - %s: %s", editorTitle, model, label);
        lblHeader.setContents(content);
    }

    /**
     * Supported editors.
     */
    public enum Type {
        NOTE, OCR, MEDIA, MODS, PARENT
    }

    /** Holds already created editor end its toolbar */
    private static final class EditorDescriptor {

        private final DatastreamEditor editor;
        private final Canvas[] toolbarItems;
        private final String title;

        EditorDescriptor(DatastreamEditor editor, String title) {
            this.editor = editor;
            toolbarItems = editor.getToolbarItems();
            this.title = title;
        }

        public DatastreamEditor getEditor() {
            return editor;
        }

        public String getTitle() {
            return title;
        }

        public Canvas[] getToolbarItems() {
            return toolbarItems;
        }

    }

}
