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

import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RestDataSource;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.FetchMode;
import cz.incad.pas.editor.shared.rest.DigitalObjectResourceApi;

/**
 * Lists Fedora models.
 *
 * @author Jan Pokorsky
 */
public class MetaModelDataSource extends RestDataSource {

    public static final String ID = "MetaModelDataSource";
    public static final String FIELD_PID = DigitalObjectResourceApi.METAMODEL_PID_PARAM;
    public static final String FIELD_DISPLAY_NAME = DigitalObjectResourceApi.METAMODEL_DISPLAYNAME_PARAM;
    public static final String FIELD_IS_ROOT = DigitalObjectResourceApi.METAMODEL_ROOT_PARAM;
    public static final String FIELD_IS_LEAF = DigitalObjectResourceApi.METAMODEL_LEAF_PARAM;
    public static final String FIELD_EDITOR = DigitalObjectResourceApi.METAMODEL_EDITORID_PARAM;

    public static final String EDITOR_PAGE = "cz.incad.pas.editor.client.widget.mods.PageForm";
    public static final String EDITOR_PERIODICAL = "cz.incad.pas.editor.client.widget.mods.PeriodicalForm";
    public static final String EDITOR_PERIODICAL_VOLUME = "cz.incad.pas.editor.client.widget.mods.PeriodicalVolumeForm";
    public static final String EDITOR_PERIODICAL_ISSUE = "cz.incad.pas.editor.client.widget.mods.PeriodicalIssueForm";
    public static final String EDITOR_MONOGRAPH = "cz.incad.pas.editor.client.widget.mods.MonographForm";
    public static final String EDITOR_MONOGRAPH_UNIT = "cz.incad.pas.editor.client.widget.mods.MonographUnitForm";
    private static ResultSet resultSet;

    public MetaModelDataSource() {
        setID(ID);

        setDataFormat(DSDataFormat.JSON);
        
        setDataURL(RestConfig.URL_DIGOBJECT_METAMODEL);

        DataSourceTextField pid = new DataSourceTextField(FIELD_PID);
        pid.setPrimaryKey(true);

        DataSourceTextField displayName = new DataSourceTextField(FIELD_DISPLAY_NAME);

        DataSourceBooleanField isRoot = new DataSourceBooleanField(FIELD_IS_ROOT);

        DataSourceBooleanField isLeaf = new DataSourceBooleanField(FIELD_IS_LEAF);

        DataSourceTextField editor = new DataSourceTextField(FIELD_EDITOR);

        setFields(pid, displayName, isRoot, isLeaf, editor);

        setRequestProperties(RestConfig.createRestRequest(getDataFormat()));
    }

    public static MetaModelDataSource getInstance() {
        MetaModelDataSource ds = (MetaModelDataSource) DataSource.get(ID);
        ds = ds != null ? ds : new MetaModelDataSource();
        return ds;
    }

    public static ResultSet getModels() {
        return getModels(false);
    }

    public static ResultSet getModels(boolean reload) {
        if (resultSet == null) {
            resultSet = new ResultSet(getInstance());
            resultSet.setFetchMode(FetchMode.LOCAL);
            resultSet.get(0);
        } else if (reload) {
            resultSet.invalidateCache();
            resultSet.get(0);
        }
        return resultSet;
    }

    public static final class MetaModelRecord {
        
        private final Record record;

        public static MetaModelRecord get(Record r) {
            return r == null ? null : new MetaModelRecord(r);
        }
        
        public MetaModelRecord(Record r) {
            this.record = r;
        }

        public String getId() {
            return record.getAttribute(FIELD_PID);
        }

        public boolean isRoot() {
            return record.getAttributeAsBoolean(FIELD_IS_ROOT);
        }

        public String getEditorId() {
            return record.getAttribute(FIELD_EDITOR);
        }

        public String getDisplayName() {
            return record.getAttribute(FIELD_DISPLAY_NAME);
        }
    }

}
