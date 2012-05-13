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
package cz.incad.pas.editor.client.ds;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.OperationBinding;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceDateTimeField;
import com.smartgwt.client.data.fields.DataSourceEnumField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.DSProtocol;
import cz.incad.pas.editor.client.PasEditorMessages;
import java.util.LinkedHashMap;

/**
 *
 * @author Jan Pokorsky
 */
public class ImportBatchDataSource extends DataSource {

    public static final String ID = "ImportBatchDataSource";
    public static final String FIELD_ID = "id";
    public static final String FIELD_PATH = "folderPath";
    public static final String FIELD_TIMESTAMP = "timeStamp";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_USER_DISPLAYNAME = "user";
    public static final String FIELD_PARENT = "parentPid";

    public ImportBatchDataSource() {
        setID(ID);

        setDataFormat(DSDataFormat.JSON);
        setRecordXPath("/batches/batch");

        setDataURL(RestConfig.URL_IMPORT_BATCH);

        PasEditorMessages i18nPas = GWT.create(PasEditorMessages.class);

        DataSourceIntegerField id = new DataSourceIntegerField(FIELD_ID);
        id.setPrimaryKey(true);

        DataSourceTextField path = new DataSourceTextField(FIELD_PATH);

        DataSourceTextField user = new DataSourceTextField(FIELD_USER_DISPLAYNAME);

        DataSourceIntegerField userId = new DataSourceIntegerField(FIELD_USER_ID);
        userId.setForeignKey(UserDataSource.ID + '.' + UserDataSource.FIELD_ID);

        DataSourceDateTimeField timestamp = new DataSourceDateTimeField(FIELD_TIMESTAMP);

        DataSourceEnumField state = new DataSourceEnumField(FIELD_STATE);
        LinkedHashMap<String, String> states = new LinkedHashMap<String, String>();
        states.put("LOADING", i18nPas.ImportBatchDataSource_State_LOADING());
        states.put("LOADING_FAILED", i18nPas.ImportBatchDataSource_State_LOADING_FAILED());
        states.put("LOADED", i18nPas.ImportBatchDataSource_State_LOADED());
        states.put("INGESTING", i18nPas.ImportBatchDataSource_State_INGESTING());
        states.put("INGESTING_FAILED", i18nPas.ImportBatchDataSource_State_INGESTING_FAILED());
        states.put("INGESTED", i18nPas.ImportBatchDataSource_State_INGESTED());
        states.put(null, "Unknown"); // should not occur
        state.setValueMap(states);

        DataSourceTextField parent = new DataSourceTextField(FIELD_PARENT);
        parent.setHidden(true);

        setFields(id, path, userId, user, timestamp, state, parent);
        
        OperationBinding addOp = new OperationBinding();
        addOp.setOperationType(DSOperationType.ADD);
        addOp.setDataProtocol(DSProtocol.POSTPARAMS);
        addOp.setRecordXPath("/batch");

        OperationBinding updateOp = new OperationBinding();
        updateOp.setOperationType(DSOperationType.UPDATE);
        updateOp.setDataProtocol(DSProtocol.POSTPARAMS);
        DSRequest updateRequest = new DSRequest();
        updateRequest.setHttpMethod("PUT");
        updateOp.setRequestProperties(updateRequest);

        setOperationBindings(addOp, updateOp);
        
        setRequestProperties(RestConfig.createRestRequest(getDataFormat()));
    }

    public Record newBatch(String folderPath, String model) {
        Record r = new Record();
        r.setAttribute(FIELD_PATH, folderPath);
        r.setAttribute("model", model);
        return r;
    }

    public static ImportBatchDataSource getInstance() {
        ImportBatchDataSource ds = (ImportBatchDataSource) DataSource.get(ID);
        ds = ds != null ? ds : new ImportBatchDataSource();
        return ds;
    }

    public static class BatchRecord {

        private final Record delegate;

        public BatchRecord(Record delegate) {
            this.delegate = delegate;
        }

        public String getId() {
            return delegate.getAttribute(FIELD_ID);
        }

        public String getParentPid() {
            return delegate.getAttribute(FIELD_PARENT);
        }

        public void setParentPid(String pid) {
            delegate.setAttribute(FIELD_PARENT, pid);
        }
    }

}
