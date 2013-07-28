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
package cz.incad.pas.editor.client.widget;

import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.FolderClickEvent;
import com.smartgwt.client.widgets.tree.events.FolderClickHandler;
import cz.incad.pas.editor.client.ClientMessages;
import cz.incad.pas.editor.client.ClientUtils;
import cz.incad.pas.editor.client.ds.ImportBatchDataSource;
import cz.incad.pas.editor.client.ds.ImportTreeDataSource;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

public final class ImportSourceChooser extends VLayout {

    private static final Logger LOG = Logger.getLogger(ImportSourceChooser.class.getName());

    private final DataSource dataSource = ImportTreeDataSource.getInstance();
//    private final DataSource metaModelSource = MetaModelDataSource.getInstance();
    private final TreeGrid treeGrid;
    private final DynamicForm optionsForm;
    private final Label lblCurrSelection;
    private ImportSourceChooserHandler viewHandler;
    private final ClientMessages i18n;
    
    public ImportSourceChooser(ClientMessages i18n) {
        this.i18n = i18n;
        VLayout layout = this;
        setWidth100();
        setHeight100();
        
        lblCurrSelection = new Label(i18n.ImportSourceChooser_NothingSelected_Title());
        lblCurrSelection.setWidth100();
        lblCurrSelection.setAutoFit(true);
        layout.addMember(lblCurrSelection);
        
        treeGrid = new TreeGrid();
        treeGrid.setHeight100();
        treeGrid.setDataSource(dataSource);
        treeGrid.setFields(
                new TreeGridField(ImportTreeDataSource.FIELD_NAME, i18n.ImportSourceChooser_TreeHeaderFolderName_Title()),
                new TreeGridField(ImportTreeDataSource.FIELD_STATE, i18n.ImportSourceChooser_TreeHeaderImportState_Title()));
        treeGrid.setShowConnectors(true);
        treeGrid.setEmptyMessage(i18n.ImportSourceChooser_NoDataOnServer_Title());
        treeGrid.setAlternateRecordStyles(true);
        treeGrid.setSelectionType(SelectionStyle.SINGLE);

        treeGrid.addFolderClickHandler(new FolderClickHandler() {

            @Override
            public void onFolderClick(FolderClickEvent event) {
                updateOnSelection();
                // issue 41: open node on single click
                TreeNode folder = event.getFolder();
                event.getViewer().getTree().openFolder(folder);
            }
        });

        layout.addMember(treeGrid);

        optionsForm = new DynamicForm();
//        SelectItem selectModel = new SelectItem("model", i18n.ImportSourceChooser_OptionImportModel_Title());
        CheckboxItem cbiPageIndexes = new CheckboxItem(ImportBatchDataSource.FIELD_INDICES,
                i18n.ImportSourceChooser_OptionPageIndices_Title());
        cbiPageIndexes.setValue(true);

        SelectItem selectScanner = new SelectItem(ImportBatchDataSource.FIELD_DEVICE,
                i18n.ImportSourceChooser_OptionScanner_Title());
        LinkedHashMap<String, String> scannerMap = new LinkedHashMap<String, String>();
        scannerMap.put("device:digibook_suprascan_10000rgb", "Digibook Suprascan 10000 RGB");
        scannerMap.put("device:panasonic_kv_s1025c", "Panasonic KV-S1025C");
        scannerMap.put("device:proserv_scanntech_600i", "ProServ ScannTech 600i");
        scannerMap.put("device:scanrobot_sr301", "ScanRobot SR301");
        scannerMap.put("device:zeutschel_7000", "Zeutschel OS 7000");
        selectScanner.setValueMap(scannerMap);
        selectScanner.setAllowEmptyValue(true);
        selectScanner.setEmptyDisplayValue(
                ClientUtils.format("<i>&lt;%s&gt;</i>", i18n.NewDigObject_OptionModel_EmptyValue_Title()));
        selectScanner.setRequired(true);
        
        optionsForm.setFields(cbiPageIndexes, selectScanner);
        layout.addMember(optionsForm);
    }

    public void setViewHandler(ImportSourceChooserHandler handler) {
        this.viewHandler = handler;
    }

    public void setFolderDataSource(DataSource ds) {
        this.treeGrid.fetchData(null, new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                treeGrid.selectRecord(0);
                treeGrid.focus();
                updateOnSelection();
            }
        });
    }

    public void setDigitalObjectModelDataSource(DataSource ds) {
        optionsForm.resetValues();
    }

    public Record getImportSource() {
        return treeGrid.getSelectedRecord();
    }

    public String getImportAsType() {
        return optionsForm.getValueAsString(ImportBatchDataSource.FIELD_MODEL);
    }

    public Boolean getGenerateIndices() {
        return (Boolean) optionsForm.getValue(ImportBatchDataSource.FIELD_INDICES);
    }

    public String getDevice() {
        return optionsForm.getValueAsString(ImportBatchDataSource.FIELD_DEVICE);
    }

    public boolean validateOptions() {
        return optionsForm.validate();
    }

    /**
     * Refreshes selected node or the whole tree.
     */
    public void refresh() {
        Tree tree = treeGrid.getTree();
        TreeNode node = (TreeNode) treeGrid.getSelectedRecord();
        if (node != null) {
            TreeNode parent = tree.getParent(node);
            if (parent != null) {
                tree.reloadChildren(parent);
                return ;
            }
        }
        treeGrid.invalidateCache();
    }

    private void updateOnSelection() {
        ListGridRecord selectedRecord = treeGrid.getSelectedRecord();
        String label = (selectedRecord == null)
                ? i18n.ImportSourceChooser_NothingSelected_Title()
                : selectedRecord.getAttribute(ImportTreeDataSource.FIELD_PATH);
        lblCurrSelection.setContents(label);
        viewHandler.sourceSelected();
    }

    public interface ImportSourceChooserHandler {

        void sourceSelected();
        
    }
}
