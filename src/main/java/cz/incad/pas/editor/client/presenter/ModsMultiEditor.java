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

import com.smartgwt.client.data.Record;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStripSeparator;
import cz.incad.pas.editor.client.ClientMessages;
import cz.incad.pas.editor.client.ClientUtils;
import cz.incad.pas.editor.client.action.AbstractAction;
import cz.incad.pas.editor.client.action.ActionEvent;
import cz.incad.pas.editor.client.action.Actions;
import cz.incad.pas.editor.client.action.Actions.ActionSource;
import cz.incad.pas.editor.client.action.RefreshAction.Refreshable;
import cz.incad.pas.editor.client.action.SaveAction;
import cz.incad.pas.editor.client.action.Selectable;
import cz.incad.pas.editor.client.ds.DigitalObjectDataSource.DigitalObject;
import cz.incad.pas.editor.client.ds.MetaModelDataSource.MetaModelRecord;
import cz.incad.pas.editor.client.ds.RelationDataSource;
import cz.incad.pas.editor.client.widget.BatchDatastreamEditor;
import cz.incad.pas.editor.client.widget.DatastreamEditor;
import cz.incad.pas.editor.client.widget.StatusView;
import java.util.logging.Logger;

/**
 * Edits MODS data in multiple custom editor (all fields, selected fields, plain XML).
 *
 * @author Jan Pokorsky
 */
public final class ModsMultiEditor implements BatchDatastreamEditor, Refreshable, Selectable<DigitalObject> {

    private static final Logger LOG = Logger.getLogger(ModsMultiEditor.class.getName());

    private final VLayout uiContainer;
    private final ModsCustomEditor modsCustomEditor;
    private final ModsFullEditor modsFullEditor;
    private final ModsXmlEditor modsSourceEditor;
    private final ModsBatchEditor modsBatchEditor;
    private DatastreamEditor activeEditor;
    private final ClientMessages i18n;
    private DigitalObject[] digitalObjects;
    private String pid;
    private String batchId;
    private MetaModelRecord model;
    private Canvas customEditorButton;
    private final ActionSource actionSource;

    public ModsMultiEditor(ClientMessages i18n) {
        this.i18n = i18n;
        uiContainer = new VLayout();
        modsFullEditor = new ModsFullEditor(i18n);
        modsCustomEditor = new ModsCustomEditor(i18n);
        modsSourceEditor = new ModsXmlEditor();
        modsBatchEditor = new ModsBatchEditor(i18n);
        actionSource = new ActionSource(this);
    }

    @Override
    public void edit(String pid, String batchId, MetaModelRecord model) {
        ClientUtils.fine(LOG, "edit pid: %s, batchId: %s", pid, batchId);
        DigitalObject dobj = DigitalObject.create(pid, batchId, model);
        edit(new DigitalObject[] { dobj }, batchId);
    }

    @Override
    public void edit(Record[] items, String batchId) {
        if (items != null) {
            DigitalObject[] dobjs = new DigitalObject[items.length];
            for (int i = 0; i < items.length; i++) {
                dobjs[i] = DigitalObject.create(items[i]);
            }
            edit(dobjs, batchId);
        }
    }

    public void edit(DigitalObject[] items, String batchId) {
        this.digitalObjects = items;
        this.batchId = batchId;
        if (items == null || items.length == 0) {
            // show nothing or throw exception!
        } else if (items.length == 1) {
            DigitalObject dobj = items[0];
            this.pid = dobj.getPid();
            this.model = dobj.getModel();
            loadCustom(pid, batchId, model);
        } else {
            String modelId = "model:page";
            boolean unsupportedBatch = false;
            for (DigitalObject dobj : items) {
                if (!modelId.equals(dobj.getModelId())) {
                    unsupportedBatch = true;
                    break;
                }
            }
            if (unsupportedBatch) {
                setActiveEditor(null);
            } else {
                loadBatch();
            }
        }
        actionSource.fireEvent();
    }

    public void save(BooleanCallback callback) {
        callback = wrapSaveCallback(callback);
        if (activeEditor == modsCustomEditor) {
            saveCustomData(callback);
        } else if (activeEditor == modsFullEditor) {
            saveFullData(callback);
        } else if (activeEditor == modsBatchEditor) {
            saveBatchData(callback);
        } else {
            callback.execute(Boolean.TRUE);
        }
    }

    /**
     * Notifies other data sources to update its caches with object label.
     */
    private BooleanCallback wrapSaveCallback(final BooleanCallback callback) {
        BooleanCallback bc = new BooleanCallback() {

            @Override
            public void execute(Boolean value) {
                RelationDataSource.getInstance().fireRelationChange(pid);
                callback.execute(value);
            }
        };
        return bc;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Class<T> clazz) {
        T c = null;
        if (Refreshable.class.equals(clazz) || BatchDatastreamEditor.class.equals(clazz)) {
            c = (T) this;
        }
        return c;
    }

    @Override
    public Canvas[] getToolbarItems() {
        SaveAction saveAction = new SaveAction(i18n) {

            @Override
            public void performAction(ActionEvent event) {
                save(new BooleanCallback() {

                    @Override
                    public void execute(Boolean value) {
                        if (value != null && value) {
                            StatusView.getInstance().show(i18n.SaveAction_Done_Msg());
                        }
                    }
                });
            }

            @Override
            public boolean accept(ActionEvent event) {
                return activeEditor != null;
            }
        };

        return new Canvas[] {
            customEditorButton = Actions.asIconButton(
                new SwitchAction(modsCustomEditor,
                        i18n.ModsMultiEditor_TabSimple_Title(),
                        Page.getAppDir() + "images/silk/16/application_form_edit.png",
                        i18n.ModsMultiEditor_TabSimple_Hint()
                ), actionSource),
            Actions.asIconButton(
                new SwitchAction(modsFullEditor,
                        i18n.ModsMultiEditor_TabFull_Title(),
                        Page.getAppDir() + "images/silk/16/container.png",
                        i18n.ModsMultiEditor_TabFull_Hint()
                ), actionSource),
            Actions.asIconButton(
                new SwitchAction(modsSourceEditor,
                        i18n.ModsMultiEditor_TabSource_Title(),
                        Page.getAppDir() + "images/oxygen/16/application_xml.png",
                        i18n.ModsMultiEditor_TabSource_Hint()
                ), actionSource),
            new ToolStripSeparator(),
            Actions.asIconButton(saveAction, actionSource)
        };

    }

    @Override
    public Canvas getUI() {
        return uiContainer;
    }

    @Override
    public void refresh() {
        if (activeEditor != null) {
            Refreshable refreshable = activeEditor.getCapability(Refreshable.class);
            if (refreshable != null) {
                refreshable.refresh();
            } else {
                loadTabData(activeEditor, pid, batchId);
            }
        }
    }

    @Override
    public DigitalObject[] getSelection() {
        return digitalObjects;
    }

    private void loadTabData(DatastreamEditor tab, String pid, String batchId) {
        if (tab == modsFullEditor) {
            loadFull(pid, batchId);
        } else if (tab == modsSourceEditor) {
            loadSource(pid, batchId);
        } else {
            loadCustom(pid, batchId, model);
        }
    }

    private void loadCustom(String pid, String batchId, MetaModelRecord model) {
        modsCustomEditor.edit(pid, batchId, model);
        if (modsCustomEditor.getCustomForm() != null) {
            setActiveEditor(modsCustomEditor);
            setEnabledCustom(true);
        } else {
            // unknown model, use full form
            setEnabledCustom(false);
            loadFull(pid, batchId);
        }
    }

    private void loadFull(String pid, String batchId) {
        setActiveEditor(modsFullEditor);
        modsFullEditor.edit(pid, batchId, model);
    }

    private void loadSource(String pid, String batchId) {
        setActiveEditor(modsSourceEditor);
        modsSourceEditor.edit(pid, batchId, model);
    }

    private void loadBatch() {
        if (activeEditor != modsBatchEditor) {
            modsBatchEditor.refresh();
        }
        setActiveEditor(modsBatchEditor);
        modsBatchEditor.edit(digitalObjects, batchId);
    }

    private void setActiveEditor(DatastreamEditor newEditor) {
        if (newEditor != activeEditor) {
            if (newEditor != null) {
                uiContainer.setMembers(newEditor.getUI());
            } else {
                uiContainer.setMembers(new Canvas[0]);
            }
            activeEditor = newEditor;
        }
    }

    private void setEnabledCustom(boolean enabled) {
        if (customEditorButton != null) {
            customEditorButton.setVisible(enabled);
        }
    }

    private void saveFullData(BooleanCallback callback) {
        modsFullEditor.save(callback);
    }

    private void saveCustomData(BooleanCallback callback) {
        modsCustomEditor.save(callback);
    }

    private void saveBatchData(BooleanCallback callback) {
        modsBatchEditor.save(callback);
    }

    private final class SwitchAction extends AbstractAction {

        private final DatastreamEditor tab;

        public SwitchAction(DatastreamEditor tab, String title, String icon, String tooltip) {
            super(title, icon, tooltip);
            this.tab = tab;
        }

        @Override
        public void performAction(ActionEvent event) {
            loadTabData(tab, pid, batchId);
        }

        @Override
        public boolean accept(ActionEvent event) {
            DigitalObject[] selections = getSelection();
            return selections != null && selections.length == 1;
        }

    }

}
