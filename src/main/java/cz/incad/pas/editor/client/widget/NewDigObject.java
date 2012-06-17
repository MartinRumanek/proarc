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

import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Criterion;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.OperatorId;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.TopOperatorAppearance;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FilterBuilder;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import cz.incad.pas.editor.client.ClientUtils;
import cz.incad.pas.editor.client.ClientUtils.DataSourceFieldBuilder;
import cz.incad.pas.editor.client.PasEditorMessages;
import cz.incad.pas.editor.client.ds.MetaModelDataSource;
import cz.incad.pas.editor.client.ds.MetaModelDataSource.MetaModelRecord;
import cz.incad.pas.editor.client.ds.RemoteMetadataDataSource;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Widget to select type of a newly created digital object.
 *
 * @author Jan Pokorsky
 */
public final class NewDigObject extends VLayout {

    private static final Logger LOG = Logger.getLogger(NewDigObject.class.getName());

    private FilterBuilder filter;
    private final SectionStack sections;
    private final DynamicForm optionsForm;
    private final PasEditorMessages i18nPas;
    private ListGrid lgResult;

    public NewDigObject(PasEditorMessages i18nPas) {
        this.i18nPas = i18nPas;
        setHeight100();
        setWidth100();

        optionsForm = createOptionsForm();

        SectionStackSection sectionMain = new SectionStackSection(
                i18nPas.NewDigObject_SectionOptions_Title());
        sectionMain.setExpanded(true);
        sectionMain.setCanCollapse(false);
        sectionMain.setItems(optionsForm);

        SectionStackSection sectionAdvanced = new SectionStackSection(
                i18nPas.NewDigObject_SectionAdvancedOptions_Title());
        sectionAdvanced.setItems(createAdvancedOptions());

        sections = new SectionStack();
        sections.setVisibilityMode(VisibilityMode.MULTIPLE);
        sections.setSections(sectionMain, sectionAdvanced);

        setMembers(sections);
    }

    public void bind(AdvancedCriteria criteria) {
        optionsForm.editNewRecord();
        lgResult.setData(new Record[0]);
        if (criteria == null) {
//            sections.collapseSection(1);
            sections.expandSection(1);
            filter.setCriteria(new AdvancedCriteria());
        } else {
            sections.expandSection(1);
            filter.setCriteria(criteria);
        }

    }

    public MetaModelRecord getModel() {
        FormItem field = optionsForm.getField(MetaModelDataSource.FIELD_PID);
        ListGridRecord selectedRecord = field.getSelectedRecord();
        Map values = selectedRecord.toMap();
        ClientUtils.info(LOG, "getModel: %s", values);
        return new MetaModelRecord(selectedRecord);
    }

    public String getMods() {
        ListGridRecord r = lgResult.getSelectedRecord();
        String mods = (r == null) ? null : r.getAttribute(RemoteMetadataDataSource.FIELD_MODS);
        ClientUtils.info(LOG, "getMods: %s", mods);
        return mods;
    }

    private DynamicForm createOptionsForm() {
        SelectItem selectModel = new SelectItem(MetaModelDataSource.FIELD_PID,
                i18nPas.NewDigObject_OptionModel_Title());
        selectModel.setRequired(true);
        selectModel.setDefaultToFirstOption(true);
        selectModel.setOptionDataSource(MetaModelDataSource.getInstance());
//        selectModel.setShowOptionsFromDataSource(true);
        selectModel.setValueField(MetaModelDataSource.FIELD_PID);
        selectModel.setDisplayField(MetaModelDataSource.FIELD_DISPLAY_NAME);
        selectModel.setAutoFetchData(true);
        DynamicForm form = new DynamicForm();
        form.setFields(selectModel);
        return form;
    }

    private Canvas createAdvancedOptions() {
        DynamicForm formCatalog = createCatalogForm();
        DataSource ds = new DataSource();
        ds.setFields(
                new DataSourceFieldBuilder(new DataSourceTextField("id", "ID"))
                        .hidden().build(),
                new DataSourceFieldBuilder(new DataSourceTextField("mods", "Preview"))
                        .filter(false).build(),
                new DataSourceFieldBuilder(new DataSourceTextField("modsDetail", "MODS"))
                        .filter(false).build(),
                new DataSourceFieldBuilder(new DataSourceTextField("issn", "ISSN"))
                        .validOperators(OperatorId.ICONTAINS).build(),
                new DataSourceFieldBuilder(new DataSourceTextField("isbn", "ISBN"))
                        .validOperators(OperatorId.ICONTAINS).build(),
                new DataSourceFieldBuilder(new DataSourceTextField("ccnb", "čČNB"))
                        .validOperators(OperatorId.ICONTAINS).build()
                );
        
        ds.setClientOnly(true);

        filter = new FilterBuilder();
        filter.setDataSource(ds);
        // now does not support operators; later use RADIO
        filter.setTopOperatorAppearance(TopOperatorAppearance.NONE);
        filter.setShowAddButton(false);
        filter.setShowSubClauseButton(false);
        filter.setShowRemoveButton(false);

        IButton find = new IButton("Find", new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                AdvancedCriteria criteria = filter.getCriteria(false);
                Criterion[] criterions = criteria.getCriteria();
                if (criterions == null || criterions.length == 0) {
                    SC.say("Missing query parameter.");
                } else {
                    Criteria plain = new Criteria("catalog", "aleph");
                    plain.addCriteria(criterions[0]);
                    // for AdvancedCriteria it will require to parse its format on server side
                    lgResult.fetchData(plain);
                }
            }
        });

        HLayout filterLayout = new HLayout();
        filterLayout.setMembers(filter, find);
        filterLayout.setLayoutRightMargin(4);

        lgResult = new ListGrid();
        lgResult.setDataSource(RemoteMetadataDataSource.getInstance());
//        lgResult.setUseAllDataSourceFields(true);
        ListGridField preview = new ListGridField(RemoteMetadataDataSource.FIELD_PREVIEW);
        ListGridField title = new ListGridField(RemoteMetadataDataSource.FIELD_TITLE);
        lgResult.setDetailField(RemoteMetadataDataSource.FIELD_PREVIEW);
        lgResult.setFields(title, preview);
//        lgResult.setAutoFetchData(true);
        lgResult.setHeight100();
        lgResult.setWidth100();
        lgResult.setCanExpandRecords(true);
        lgResult.setCanExpandMultipleRecords(false);
        lgResult.setExpansionMode(ExpansionMode.DETAIL_FIELD);
        lgResult.setSelectionType(SelectionStyle.SINGLE);
//        lgResult.setSelectionAppearance(SelectionAppearance.CHECKBOX);
        lgResult.setAlternateRecordStyles(true);

        VLayout layout = new VLayout(2);
        layout.setMembers(formCatalog, filterLayout, lgResult);
        return layout;
    }

    private DynamicForm createCatalogForm() {
        DataSource ds = new DataSource();
        ds.setFields(
                new DataSourceTextField("id", "ID"),
                new DataSourceTextField("catalog", "Catalog")
                );
        ds.setClientOnly(true);
        ds.setTestData(new Record[] {
            new Record() {{
                setAttribute("id", "id:aleph");
                setAttribute("catalog", "Aleph NKP");
            }},
            new Record() {{
                setAttribute("id", "id:registrdigitalizace.cz");
                setAttribute("catalog", "registrdigitalizace.cz");
            }},
        });

        SelectItem selection = new SelectItem("catalog", i18nPas.NewDigObject_OptionCatalog_Title());
        selection.setRequired(true);
        selection.setOptionDataSource(ds);
//        selectModel.setShowOptionsFromDataSource(true);
        selection.setValueField("id");
        selection.setDisplayField("catalog");
        selection.setAutoFetchData(true);
        selection.setDefaultToFirstOption(true);

        DynamicForm form = new DynamicForm();
        form.setFields(selection);
        return form;
    }
}
