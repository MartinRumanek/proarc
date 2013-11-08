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
package cz.cas.lib.proarc.webapp.client.widget;

import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Criterion;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.types.OperatorId;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.TopOperatorAppearance;
import com.smartgwt.client.types.ValueItemType;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FilterBuilder;
import com.smartgwt.client.widgets.form.events.FilterSearchEvent;
import com.smartgwt.client.widgets.form.events.SearchHandler;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.CustomValidator;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ClientUtils;
import cz.cas.lib.proarc.webapp.client.ClientUtils.DataSourceFieldBuilder;
import cz.cas.lib.proarc.webapp.client.action.AbstractAction;
import cz.cas.lib.proarc.webapp.client.action.Action;
import cz.cas.lib.proarc.webapp.client.action.ActionEvent;
import cz.cas.lib.proarc.webapp.client.action.Actions;
import cz.cas.lib.proarc.webapp.client.ds.BibliographyDataSource;
import cz.cas.lib.proarc.webapp.client.ds.BibliographyQueryDataSource;
import cz.cas.lib.proarc.webapp.client.ds.DigitalObjectDataSource;
import cz.cas.lib.proarc.webapp.client.ds.MetaModelDataSource;
import cz.cas.lib.proarc.webapp.client.ds.MetaModelDataSource.MetaModelRecord;
import cz.incad.pas.editor.shared.rest.BibliographicCatalogResourceApi;
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
    private DynamicForm formCatalog;
    private final ClientMessages i18n;
    private ListGrid lgResult;
    private Handler handler;

    public NewDigObject(ClientMessages i18n) {
        this.i18n = i18n;
        setHeight100();
        setWidth100();

        ToolStrip toolbar = createToolbar();

        optionsForm = createOptionsForm();

        SectionStackSection sectionMain = new SectionStackSection(
                i18n.NewDigObject_SectionOptions_Title());
        sectionMain.setExpanded(true);
        sectionMain.setCanCollapse(false);
        sectionMain.setItems(optionsForm);

        SectionStackSection sectionAdvanced = new SectionStackSection(
                i18n.NewDigObject_SectionAdvancedOptions_Title());
        sectionAdvanced.setItems(createAdvancedOptions());

        sections = new SectionStack();
        sections.setVisibilityMode(VisibilityMode.MULTIPLE);
        sections.setSections(sectionMain, sectionAdvanced);

        setMembers(toolbar, sections);
    }

    public void bind(String model, AdvancedCriteria criteria) {
        optionsForm.clearErrors(true);
        optionsForm.editNewRecord();
        if (model != null) {
            optionsForm.setValue(DigitalObjectDataSource.FIELD_MODEL, model);
        }
        fixExpandedListGrid();
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

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public MetaModelRecord getModel() {
        FormItem field = optionsForm.getField(DigitalObjectDataSource.FIELD_MODEL);
        ListGridRecord selectedRecord = field.getSelectedRecord();
//        Map<?, ?> values = selectedRecord.toMap();
//        ClientUtils.info(LOG, "getModel: %s", values);
        return MetaModelRecord.get(selectedRecord);
    }

    public String getMods() {
        ListGridRecord r = lgResult.getSelectedRecord();
        String mods = (r == null) ? null : r.getAttribute(BibliographyQueryDataSource.FIELD_MODS);
        ClientUtils.info(LOG, "getMods: %s", mods);
        return mods;
    }

    public String getNewPid() {
        String newPid = optionsForm.getValueAsString(DigitalObjectDataSource.FIELD_PID);
        return newPid;
    }

    public boolean validate() {
        return optionsForm.validate();
    }

    public void setValidationErrors(Map<?,?> errors) {
        optionsForm.setErrors(errors, true);
    }

    private ToolStrip createToolbar() {
        Action actionNewObject = new AbstractAction(
                i18n.DigitalObjectCreator_FinishedStep_CreateNewObjectButton_Title(),
                "[SKIN]/actions/save.png",
                null) {

            @Override
            public void performAction(ActionEvent event) {
                if (handler != null) {
                    handler.onCreateObject();
                }
            }
        };
        ToolStrip t = Actions.createToolStrip();
        t.addMember(Actions.asIconButton(actionNewObject, this));
        return t;
    }

    private DynamicForm createOptionsForm() {
        SelectItem selectModel = new SelectItem(DigitalObjectDataSource.FIELD_MODEL,
                i18n.NewDigObject_OptionModel_Title());
        selectModel.setRequired(true);
        // issue 46: always start with empty model
        selectModel.setAllowEmptyValue(true);
        selectModel.setEmptyDisplayValue(ClientUtils.format("<i>&lt;%s&gt;</i>", i18n.NewDigObject_OptionModel_EmptyValue_Title()));
        selectModel.setOptionDataSource(MetaModelDataSource.getInstance());
//        selectModel.setShowOptionsFromDataSource(true);
        selectModel.setValueField(MetaModelDataSource.FIELD_PID);
        selectModel.setDisplayField(MetaModelDataSource.FIELD_DISPLAY_NAME);
        selectModel.setAutoFetchData(true);
        selectModel.setValidators(new CustomValidator() {

            @Override
            protected boolean condition(Object value) {
                boolean valid = getFormItem().getSelectedRecord() != null;
                return valid;
            }
        });

        TextItem newPid = new TextItem(DigitalObjectDataSource.FIELD_PID);
        newPid.setTitle(i18n.NewDigObject_OptionPid_Title());
        newPid.setTooltip(i18n.NewDigObject_OptionPid_Hint());
        newPid.setLength(36 + 5);
        newPid.setWidth((36 + 5) * 8);
        newPid.setValidators(new RegExpValidator(
                "uuid:[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}"));
        DynamicForm form = new DynamicForm();
        form.setWrapItemTitles(false);
        form.setAutoFocus(true);
        form.setNumCols(4);
        form.setBrowserSpellCheck(false);
        form.setFields(selectModel, newPid);
        form.setAutoWidth();
        form.setMargin(4);
        return form;
    }

    private Canvas createAdvancedOptions() {
        formCatalog = createCatalogForm();
        DataSource ds = new DataSource();
        ds.setFields(
                DataSourceFieldBuilder.field(new DataSourceTextField(
                            "issn", i18n.NewDigObject_CatalogFieldIssn_Title()))
                        .validOperators(OperatorId.ICONTAINS).build(),
                DataSourceFieldBuilder.field(new DataSourceTextField(
                            "isbn", i18n.NewDigObject_CatalogFieldIsbn_Title()))
                        .validOperators(OperatorId.ICONTAINS).build(),
                DataSourceFieldBuilder.field(new DataSourceTextField(
                            "ccnb", i18n.NewDigObject_CatalogFieldCcnb_Title()))
                        .validOperators(OperatorId.ICONTAINS).build(),
                DataSourceFieldBuilder.field(new DataSourceTextField(
                            "barcode", i18n.NewDigObject_CatalogFieldBarcode_Title()))
                        .validOperators(OperatorId.ICONTAINS).build(),
                DataSourceFieldBuilder.field(new DataSourceTextField(
                            "signature", i18n.NewDigObject_CatalogFieldSignature_Title()))
                        .validOperators(OperatorId.ICONTAINS).build()
                );
        
        ds.setClientOnly(true);

        filter = new FilterBuilder() {

            @Override
            public FormItem getValueFieldProperties(FieldType type, String fieldName, OperatorId operatorId, ValueItemType itemType, String fieldType) {
                FormItem fi = super.getValueFieldProperties(type, fieldName, operatorId, itemType, fieldType);
                if (type == FieldType.TEXT && itemType == ValueItemType.VALUE) {
                    fi = new TextItem(fieldName);
                    fi.setWidth(300);
                }
                return fi;
            }

        };
        filter.setDataSource(ds);
        // now does not support operators; later use RADIO
        filter.setTopOperatorAppearance(TopOperatorAppearance.NONE);
        filter.setShowAddButton(false);
        filter.setShowSubClauseButton(false);
        filter.setShowRemoveButton(false);
        filter.setSaveOnEnter(true);

        filter.addSearchHandler(new SearchHandler() {

            @Override
            public void onSearch(FilterSearchEvent event) {
                queryCatalog();
            }
        });

        IButton find = new IButton(i18n.NewDigObject_CatalogFind_Title(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                queryCatalog();
            }
        });

        HStack filterLayout = new HStack();
        filterLayout.setMembers(filter, find);
        filterLayout.setMembersMargin(4);

        lgResult = new ListGrid();
        lgResult.setDataSource(BibliographyQueryDataSource.getInstance());
//        lgResult.setUseAllDataSourceFields(true);
        ListGridField preview = new ListGridField(BibliographyQueryDataSource.FIELD_PREVIEW,
                i18n.NewDigObject_CatalogHeaderPreview_Title());
        ListGridField title = new ListGridField(BibliographyQueryDataSource.FIELD_TITLE,
                i18n.NewDigObject_CatalogHeaderTitle_Title());
        lgResult.setDetailField(BibliographyQueryDataSource.FIELD_PREVIEW);
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
        lgResult.addDataArrivedHandler(new DataArrivedHandler() {

            @Override
            public void onDataArrived(DataArrivedEvent event) {
                if (event.getStartRow() == 0 && event.getEndRow() > 0) {
                    lgResult.focus();
                    lgResult.selectSingleRecord(0);
                }
            }
        });

        VLayout layout = new VLayout();
        layout.setMembers(formCatalog, filterLayout, lgResult);
        layout.setMargin(4);
        layout.setMembersMargin(4);
        return layout;
    }

    private void queryCatalog() {
        AdvancedCriteria criteria = filter.getCriteria(false);
        Criterion[] criterions = criteria.getCriteria();
        if (criterions == null || criterions.length == 0) {
            SC.warn(i18n.NewDigObject_CatalogFind_MissingParam_Msg());
        } else {
            Criteria plain = formCatalog.getValuesAsCriteria();
            plain.addCriteria(criterions[0]);
            // for AdvancedCriteria it will require to parse its format on server side
            lgResult.invalidateCache();
            lgResult.fetchData(plain);
        }
    }

    private DynamicForm createCatalogForm() {
        SelectItem selection = new SelectItem(BibliographicCatalogResourceApi.FIND_CATALOG_PARAM,
                i18n.NewDigObject_OptionCatalog_Title());
        selection.setRequired(true);
        selection.setOptionDataSource(BibliographyDataSource.getInstance());
//        selectModel.setShowOptionsFromDataSource(true);
        selection.setValueField(BibliographicCatalogResourceApi.CATALOG_ID);
        selection.setDisplayField(BibliographicCatalogResourceApi.CATALOG_NAME);
        selection.setAutoFetchData(true);
        selection.setDefaultToFirstOption(true);
        selection.setWidth(250);

        DynamicForm form = new DynamicForm();
        form.setFields(selection);
        form.setBrowserSpellCheck(false);
        form.setAutoWidth();
        form.setWrapItemTitles(false);
        return form;
    }

    /**
     * Switching panels containing ListGrid with an expanded record results
     * to NPE in GWT javascript code.
     * Steps to reproduce:
     * 1. New Object
     * 2. find catalog records
     * 3. expand some record
     * 4. select whatever item in main menu
     * 5. select New Object again
     * 6. panel content not shown
     * The workaround is to collapse the selected record before rendering.
     */
    private void fixExpandedListGrid() {
        ListGridRecord selectedResult = lgResult.getSelectedRecord();
        if (selectedResult != null) {
            lgResult.collapseRecord(selectedResult);
        }
    }

    public interface Handler {
        void onCreateObject();
    }
    
}
