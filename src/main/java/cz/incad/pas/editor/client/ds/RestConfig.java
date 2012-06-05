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
import com.google.gwt.user.client.Window;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.OperationBinding;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.DSProtocol;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jan Pokorsky
 */
public final class RestConfig {

//    public static final String URL_ROOT =  "/rest";
    public static final String URL_ROOT =  GWT.getHostPageBaseURL() + "rest";
    public static final String URL_SCAN_IMPORT =  URL_ROOT + "/import";
    public static final String URL_IMPORT_BATCH =  URL_SCAN_IMPORT + "/batch";
    public static final String URL_IMPORT_BATCH_ITEM =  URL_IMPORT_BATCH + "/item";
    public static final String URL_DIGOBJECT =  URL_ROOT + "/object";
    public static final String URL_DIGOBJECT_CHILDREN =  URL_DIGOBJECT + "/members";
    public static final String URL_DIGOBJECT_DC =  URL_DIGOBJECT + "/dc";
    public static final String URL_DIGOBJECT_DISSEMINATION =  URL_DIGOBJECT + "/dissemination";
    public static final String URL_DIGOBJECT_FULL =  URL_DIGOBJECT + "/full";
    public static final String URL_DIGOBJECT_MODS =  URL_DIGOBJECT + "/mods";
    public static final String URL_DIGOBJECT_MODS_CUSTOM =  URL_DIGOBJECT_MODS + "/custom";
    public static final String URL_DIGOBJECT_MODS_PLAIN =  URL_DIGOBJECT_MODS + "/plain";
    public static final String URL_DIGOBJECT_OCR =  URL_DIGOBJECT + "/ocr";
    public static final String URL_DIGOBJECT_PREVIEW =  URL_DIGOBJECT + "/preview";
    public static final String URL_DIGOBJECT_PRIVATE_NOTE =  URL_DIGOBJECT + "/privatenote";
    public static final String URL_DIGOBJECT_RAW =  URL_DIGOBJECT + "/raw";
    public static final String URL_DIGOBJECT_SEARCH =  URL_DIGOBJECT + "/search";
    public static final String URL_DIGOBJECT_THUMBNAIL =  URL_DIGOBJECT + "/thumb";
    public static final String URL_DIGOBJECT_METAMODEL =  URL_DIGOBJECT + "/metamodel";
    public static final String URL_METADATACATALOG =  URL_ROOT + "/metadatacatalog";

    public static DSRequest createRestRequest(DSDataFormat format) {
        DSRequest dsr = new DSRequest();
        dsr.setUseSimpleHttp(true);
        Map<String, String> defaultHeaders = new HashMap<String, String>();
        switch (format) {
            case XML:
                defaultHeaders.put("Accept", "application/xml");
                break;
            case JSON:
                defaultHeaders.put("Accept", "application/json");
                break;
        }
        String locale = Window.Location.getParameter("locale");
        if (locale != null) {
            defaultHeaders.put("Accept-Language", locale);
        }
        dsr.setHttpHeaders(defaultHeaders);
        return dsr;
    }

    /**
     * Helper for RESTful POST method.
     *
     * @return add operation
     */
    public static OperationBinding createAddOperation() {
        OperationBinding op = new OperationBinding();
        op.setOperationType(DSOperationType.ADD);
        op.setDataProtocol(DSProtocol.POSTPARAMS);
        DSRequest dsRequest = new DSRequest();
        dsRequest.setHttpMethod("POST");
        op.setRequestProperties(dsRequest);
        return op;
    }

    /**
     * Helper for RESTful PUT method.
     *
     * @return update operation
     */
    public static OperationBinding createUpdateOperation() {
        OperationBinding op = new OperationBinding();
        op.setOperationType(DSOperationType.UPDATE);
        op.setDataProtocol(DSProtocol.POSTPARAMS);
        DSRequest dsRequest = new DSRequest();
        dsRequest.setHttpMethod("PUT");
        op.setRequestProperties(dsRequest);
        return op;
    }

    /**
     * Helper for RESTful DELETE method.
     *
     * @return remove operation
     */
    public static OperationBinding createDeleteOperation() {
        OperationBinding op = new OperationBinding();
        op.setOperationType(DSOperationType.REMOVE);
        op.setDataProtocol(DSProtocol.GETPARAMS);
        DSRequest dsRequest = new DSRequest();
        dsRequest.setHttpMethod("DELETE");
        op.setRequestProperties(dsRequest);
        return op;
    }

}
