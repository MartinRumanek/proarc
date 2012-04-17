package cz.incad.pas.editor.client.widget;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import java.util.HashMap;
import java.util.Map;

import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RestDataSource;
import com.smartgwt.client.data.fields.DataSourceEnumField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.PromptStyle;
import cz.incad.pas.editor.client.ds.RestConfig;

public class ImportTreeRestDataSource extends RestDataSource {
    
    public static final String FIELD_NAME = "name";
    public static final String FIELD_PARENT = "parent";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_STATE = "state";
    
    private static final String ID = "ImportTreeDataSource";
    private static final Map<String, String> states = new HashMap<String, String>();
    
    public static ImportTreeRestDataSource getInstance() {
        ImportTreeRestDataSource ds = (ImportTreeRestDataSource) DataSource.get(ID);
        return ds != null ? ds : new ImportTreeRestDataSource();
    }

    private ImportTreeRestDataSource() {
        setID(ID);
        setDataFormat(DSDataFormat.JSON);
//        setJsonRecordXPath("response/data/folder");
//        setDataFormat(DSDataFormat.XML);
        
        DataSourceTextField path = new DataSourceTextField(FIELD_PATH);
        path.setPrimaryKey(true);
        path.setHidden(true);
        
        DataSourceTextField parent = new DataSourceTextField(FIELD_PARENT);
        parent.setForeignKey(FIELD_PATH);
        parent.setHidden(true);
        
        DataSourceTextField name = new DataSourceTextField(FIELD_NAME);
        
        DataSourceEnumField state = new DataSourceEnumField(FIELD_STATE, "State");
        states.put("IMPORTED", "Imported");
        states.put("NEW", "");
        states.put("RUNNING", "Running");
        state.setValueMap(states);
        
        setFields(path, parent, name, state);
//        setDataURL("ds/ImportSourceTree.js");
//        setDataURL("http://127.0.0.1:8888/Editor/rest/import");
        setDataURL(RestConfig.URL_SCAN_IMPORT);
//        setClientOnly(true);
        Map<String, String> defaultParams = new HashMap<String, String>();
        defaultParams.put("lang", "en");
        setDefaultParams(defaultParams);

        setRequestProperties(RestConfig.createRestRequest(getDataFormat()));

    }

    @Override
    protected Object transformRequest(DSRequest dsRequest) {
        dsRequest.setPromptStyle(PromptStyle.DIALOG);
//        String actionURL = dsRequest.getActionURL();
//        String[] attributes = dsRequest.getAttributes();
//        String dataAsString = dsRequest.getDataAsString();
//        Record oldValues = dsRequest.getOldValues();
//        System.out.println("ITRDS.actionURL: " + actionURL);
//        System.out.println("ITRDS.data: " + dataAsString);
//        System.out.println("ITRDS.oldValues: " + oldValues);
//        System.out.println("ITRDS.attribs: " + Arrays.toString(attributes));

        return super.transformRequest(dsRequest);
    }

    @Override
    protected void transformResponse(DSResponse response, DSRequest request, Object data) {
        int status = response.getStatus();
        if (status == RPCResponse.STATUS_SUCCESS) {
            for (Record record : response.getData()) {
                String path = record.getAttribute(FIELD_PATH);
                RegExp pathRegExp = RegExp.compile("(.*/)?(.*)/$");
                MatchResult mr = pathRegExp.exec(path);
                String parent = mr.getGroup(1);
                String name = mr.getGroup(2);
//                System.out.println("## ITRDS.path: " + path);
//                System.out.println("## ITRDS.parent: " + parent);
//                System.out.println("## ITRDS.name: " + name);

                record.setAttribute(FIELD_NAME, name);
                record.setAttribute(FIELD_PARENT, parent);
            }
        }
        super.transformResponse(response, request, data);
    }

    /**
     * Helper to minimize params send to update the record. Actually it uses just 'path'.
     */
    public static Record createUpdateRecord(Record rec, String model) {
        Record filtered = new Record();
        filtered.setAttribute(FIELD_PATH, rec.getAttribute(FIELD_PATH));
        filtered.setAttribute("model", model);
        return filtered;
    }

//    public void importFolder(Record rec, String model) {
//        up
//    }

    public static final class ImportRecord {

        private final Record delegate;

        public ImportRecord(Record delegate) {
            this.delegate = delegate;
        }

        public String getPath() {
            return delegate.getAttribute(FIELD_PATH);
        }

        public boolean isImported() {
            return "IMPORTED".equals(delegate.getAttribute(FIELD_STATE));
        }

        public boolean isNew() {
            String state = delegate.getAttribute(FIELD_STATE);
            return state == null || "NEW".equals(state);
        }

    }



}
