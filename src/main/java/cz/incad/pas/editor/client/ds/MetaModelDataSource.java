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

import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.RestDataSource;
import com.smartgwt.client.data.XMLTools;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceSequenceField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;

/**
 *
 * @author Jan Pokorsky
 */
public class MetaModelDataSource extends DataSource {

    public static final String ID = "MetaModelDataSource";
    public static final String FIELD_PID = "pid";
    public static final String FIELD_DISPLAY_NAME = "displayName";
    public static final String FIELD_IS_ROOT = "root";
    public static final String FIELD_IS_LEAF = "leaf";

    public MetaModelDataSource() {
        setID(ID);

//        setDataFormat(DSDataFormat.XML);
        setDataFormat(DSDataFormat.JSON);
//        setRecordXPath("/models/model");
        setRecordXPath("model");
        
        setDataURL(RestConfig.URL_DIGOBJECT_METAMODEL);

        DataSourceTextField pid = new DataSourceTextField(FIELD_PID);
        pid.setPrimaryKey(true);

        DataSourceTextField displayName = new DataSourceTextField(FIELD_DISPLAY_NAME);

        DataSourceBooleanField isRoot = new DataSourceBooleanField(FIELD_IS_ROOT);

        DataSourceBooleanField isLeaf = new DataSourceBooleanField(FIELD_IS_LEAF);

        setFields(pid, displayName, isRoot, isLeaf);

        setRequestProperties(RestConfig.createRestRequest(getDataFormat()));
    }

    public static MetaModelDataSource getInstance() {
        MetaModelDataSource ds = (MetaModelDataSource) DataSource.get(ID);
        ds = ds != null ? ds : new MetaModelDataSource();
//        ds.fetchData(null, new DSCallback() {
//
//            @Override
//            public void execute(DSResponse response, Object rawData, DSRequest request) {
//                String selectString = XMLTools.selectString(rawData, "//model/pid");
//                System.out.println("## result: " + selectString);
//            }
//        });
        return ds;
    }


}
