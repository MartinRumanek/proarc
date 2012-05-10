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
package cz.incad.pas.editor.client.widget;

import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.AutoFitWidthApproach;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedEvent;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import cz.incad.pas.editor.client.PasEditorMessages;
import cz.incad.pas.editor.client.ds.ImportBatchDataSource;

/**
 * The widget to select a batch from import history. There should be 2 kinds of
 * batch items. The imported and not yet imported batches. The second one can be
 * resumed.
 *
 * @author Jan Pokorsky
 */
public final class ImportBatchChooser extends VLayout {

    private ImportBatchChooserHandler handler;
    private final ListGrid lGridBatches;
    private final PasEditorMessages i18nPas;

    public ImportBatchChooser(PasEditorMessages i18nPas) {
        this.i18nPas = i18nPas;
        
        setWidth100();
        setHeight100();
//        setContents("Import Batch History");

        lGridBatches = initBatchesListGrid();
        lGridBatches.setDataSource(ImportBatchDataSource.getInstance());
        
        addMember(lGridBatches);
    }

    private ListGrid initBatchesListGrid() {
        ListGrid lg = new ListGrid();
        lg.setSelectionType(SelectionStyle.SINGLE);
        lg.setCanReorderFields(false);
        lg.setCanSort(false);
        ListGridField lgfFolder = new ListGridField(ImportBatchDataSource.FIELD_PATH,
                i18nPas.ImportBatchDataSource_FolderFieldTitle());
//        lgfFolder.setAutoFitWidth(false);
        ListGridField lgfDate = new ListGridField(ImportBatchDataSource.FIELD_TIMESTAMP,
                i18nPas.ImportBatchDataSource_ImportDateFieldTitle());
        lgfDate.setAutoFitWidth(true);
        ListGridField lgfImported = new ListGridField(ImportBatchDataSource.FIELD_STATE,
                i18nPas.ImportBatchDataSource_StateFieldTitle());
        lgfImported.setAutoFitWidth(true);
        lgfImported.setAutoFitWidthApproach(AutoFitWidthApproach.TITLE);
        ListGridField lgfUser = new ListGridField(ImportBatchDataSource.FIELD_USER_DISPLAYNAME,
                i18nPas.ImportBatchDataSource_UserFieldTitle());
        lgfUser.setAutoFitWidth(true);
        lgfUser.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);
        lg.setFields(lgfFolder, lgfDate, lgfImported, lgfUser);

        lg.addSelectionUpdatedHandler(new SelectionUpdatedHandler() {

            @Override
            public void onSelectionUpdated(SelectionUpdatedEvent event) {
                updateOnSelection();
            }
        });
        return lg;
    }

    public void bind() {
        lGridBatches.fetchData(null, new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                lGridBatches.selectRecord(0);
                updateOnSelection();
                lGridBatches.focus();
            }
        });
    }

    public void setHandler(ImportBatchChooserHandler handler) {
        this.handler = handler;
    }

    public Record getSelectedBatch() {
        return lGridBatches.getSelectedRecord();
    }

    private void updateOnSelection() {
        if (handler != null) {
            handler.itemSelected();
        }
    }

    public interface ImportBatchChooserHandler {
        void itemSelected();
    }

}
