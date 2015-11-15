/*
 * Copyright (C) 2015 Jan Pokorsky
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
package cz.cas.lib.proarc.webapp.client.presenter;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.types.CriteriaPolicy;
import com.smartgwt.client.types.FetchMode;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.AutoFitTextAreaItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedEvent;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import cz.cas.lib.proarc.common.workflow.model.WorkflowModelConsts;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ClientUtils;
import cz.cas.lib.proarc.webapp.client.Editor;
import cz.cas.lib.proarc.webapp.client.action.AbstractAction;
import cz.cas.lib.proarc.webapp.client.action.ActionEvent;
import cz.cas.lib.proarc.webapp.client.action.Actions;
import cz.cas.lib.proarc.webapp.client.action.RefreshAction;
import cz.cas.lib.proarc.webapp.client.action.RefreshAction.Refreshable;
import cz.cas.lib.proarc.webapp.client.action.SaveAction;
import cz.cas.lib.proarc.webapp.client.ds.UserDataSource;
import cz.cas.lib.proarc.webapp.client.ds.WorkflowJobDataSource;
import cz.cas.lib.proarc.webapp.client.ds.WorkflowProfileDataSource;
import cz.cas.lib.proarc.webapp.client.ds.WorkflowTaskDataSource;
import cz.cas.lib.proarc.webapp.client.presenter.WorkflowManaging.WorkflowNewJobPlace;
import cz.cas.lib.proarc.webapp.client.presenter.WorkflowTasksEditor.WorkflowMaterialView;

/**
 * Edits jobs of the workflow.
 *
 * @author Jan Pokorsky
 */
public class WorkflowJobsEditor {

    private final ClientMessages i18n;
    private WorkflowJobView view;
    private final PlaceController places;

    public WorkflowJobsEditor(ClientMessages i18n, PlaceController places) {
        this.i18n = i18n;
        this.places = places;
    }

    public Canvas getUI() {
        if (view == null) {
            view = new WorkflowJobView(i18n);
            view.init();
            view.setHandler(this);
        }
        return view.getWidget();
    }

    private void onCreateNew() {
        places.goTo(new WorkflowNewJobPlace());
    }

    private static final class WorkflowJobView implements Refreshable {

        private final ClientMessages i18n;
        private final Canvas widget;
        private ListGrid jobGrid;
        private WorkflowJobFormView jobFormView;
        private WorkflowJobsEditor handler;

        public WorkflowJobView(ClientMessages i18n) {
            this.i18n = i18n;
            this.widget = createMainLayout();
        }

        public Canvas getWidget() {
            return widget;
        }

        public void init() {
            jobGrid.fetchData();
        }

        public void setHandler(WorkflowJobsEditor handler) {
            this.handler = handler;
        }

        @Override
        public void refresh() {
            jobGrid.invalidateCache();
        }

        private Canvas createMainLayout() {
            VLayout main = new VLayout();
            main.addMember(createPanelLabel());
            // filter
            main.addMember(createFilter());
            // toolbar
            main.addMember(createJobsToolbar());
            // list + item
            main.addMember(createJobLayout());
            return main;
        }

        private Canvas createJobLayout() {
            HLayout l = new HLayout();
            l.addMember(createJobList());
            l.addMember(createJobFormLayout());
            return l;
        }

        private Label createPanelLabel() {
            Label lblHeader = new Label();
            String title = ClientUtils.format("<b>%s</b>", "Správa záměrů");//i18n.DigitalObjectManager_Title());
            lblHeader.setContents(title);
            lblHeader.setAutoHeight();
            lblHeader.setPadding(4);
            lblHeader.setStyleName(Editor.CSS_PANEL_DESCRIPTION_TITLE);
            return lblHeader;
        }

        private DynamicForm createFilter() {
            DynamicForm form = new DynamicForm();
            form.setBrowserSpellCheck(false);
            form.setValidateOnExit(true);
            form.setSaveOnEnter(true);
            form.setAutoHeight();
            form.setWidth100();
            form.setNumCols(3);
            // ????
            return form;
        }

        private ToolStrip createJobsToolbar() {
            ToolStrip toolbar = Actions.createToolStrip();
            RefreshAction refreshAction = new RefreshAction(i18n);
            SaveAction saveAction = new SaveAction(i18n) {

                @Override
                public void performAction(ActionEvent event) {
                }
            };

            AbstractAction addAction = new AbstractAction("Nový",//i18n.DeviceManager_Add_Title(),
                    "[SKIN]/actions/add.png", "Nový záměr") {//i18n.DeviceManager_Add_Hint()) {

                @Override
                public void performAction(ActionEvent event) {
                    if (handler != null) {
                        handler.onCreateNew();
                    }
                }
            };
            toolbar.addMember(Actions.asIconButton(refreshAction, this));
            toolbar.addMember(Actions.asIconButton(addAction, this));
            toolbar.addMember(Actions.asIconButton(saveAction, this));
            return toolbar;
        }

        private ListGrid createJobList() {
            jobGrid = new ListGrid();
            jobGrid.setSelectionType(SelectionStyle.SINGLE);
            jobGrid.setShowFilterEditor(true);
            jobGrid.setFilterOnKeypress(true);
            jobGrid.setFilterLocalData(false);
            jobGrid.setCanSort(true);
            jobGrid.setDataFetchMode(FetchMode.PAGED);
            ResultSet rs = new ResultSet();
            rs.setCriteriaPolicy(CriteriaPolicy.DROPONCHANGE);
            rs.setUseClientFiltering(false);
            rs.setUseClientSorting(false);
            jobGrid.setDataProperties(rs);
            jobGrid.setDataSource(WorkflowJobDataSource.getInstance(),
                    new ListGridField(WorkflowJobDataSource.FIELD_LABEL),
                    new ListGridField(WorkflowJobDataSource.FIELD_ID, 30),
                    new ListGridField(WorkflowJobDataSource.FIELD_STATE, 50),
                    new ListGridField(WorkflowJobDataSource.FIELD_PROFILE_ID, 80),
                    new ListGridField(WorkflowJobDataSource.FIELD_OWNER, 50),
                    new ListGridField(WorkflowJobDataSource.FIELD_PRIORITY, 50),
                    new ListGridField(WorkflowJobDataSource.FIELD_CREATED, 100),
                    new ListGridField(WorkflowJobDataSource.FIELD_MODIFIED, 100),
                    new ListGridField(WorkflowJobDataSource.FIELD_FINANCED, 100),
                    new ListGridField(WorkflowJobDataSource.FIELD_NOTE)
                    );

            jobGrid.getField(WorkflowJobDataSource.FIELD_LABEL).setWidth("80%");
            jobGrid.getField(WorkflowJobDataSource.FIELD_LABEL).setFilterOnKeypress(false);

            jobGrid.getField(WorkflowJobDataSource.FIELD_STATE).setCanSort(false);

            jobGrid.getField(WorkflowJobDataSource.FIELD_PROFILE_ID).setCanSort(false);
            SelectItem profileFilter = new SelectItem();
            profileFilter.setOptionDataSource(WorkflowProfileDataSource.getInstance());
            profileFilter.setValueField(WorkflowProfileDataSource.FIELD_ID);
            profileFilter.setDisplayField(WorkflowProfileDataSource.FIELD_LABEL);
            jobGrid.getField(WorkflowJobDataSource.FIELD_PROFILE_ID).setFilterEditorProperties(profileFilter);

            jobGrid.getField(WorkflowJobDataSource.FIELD_OWNER).setCanSort(false);
            SelectItem owner = new SelectItem();
            owner.setOptionDataSource(UserDataSource.getInstance());
            owner.setValueField(UserDataSource.FIELD_ID);
            owner.setDisplayField(UserDataSource.FIELD_USERNAME);
            jobGrid.getField(WorkflowJobDataSource.FIELD_OWNER).setFilterEditorProperties(owner);

//            jobGrid.getField(WorkflowJobDataSource.FIELD_CREATED).setCanFilter(true);
//            jobGrid.getField(WorkflowJobDataSource.FIELD_MODIFIED).setCanFilter(true);

            jobGrid.getField(WorkflowJobDataSource.FIELD_FINANCED).setCanFilter(false);
            jobGrid.getField(WorkflowJobDataSource.FIELD_FINANCED).setCanSort(false);

            jobGrid.getField(WorkflowJobDataSource.FIELD_NOTE).setCanFilter(false);
            jobGrid.getField(WorkflowJobDataSource.FIELD_NOTE).setCanSort(false);

            jobGrid.addDataArrivedHandler(new DataArrivedHandler() {

                @Override
                public void onDataArrived(DataArrivedEvent event) {
                    int startRow = event.getStartRow();
                    int endRow = event.getEndRow();
                    if (startRow == 0 && endRow >= 0) {
                        jobGrid.focus();
                        jobGrid.selectSingleRecord(0);
                    } else if (endRow < 0) {
                        jobGrid.deselectAllRecords();
                    }
                }
            });
            jobGrid.addSelectionUpdatedHandler(new SelectionUpdatedHandler() {

                @Override
                public void onSelectionUpdated(SelectionUpdatedEvent event) {
                    jobFormView.setJob(jobGrid.getSelectedRecord());
                }
            });
            return jobGrid;
        }

        private Canvas createJobFormLayout() {
            jobFormView = new WorkflowJobFormView(i18n);
            return jobFormView.getWidget();
        }

    }

    private static final class WorkflowJobFormView {

        private final ClientMessages i18n;
        private final Canvas widget;
        private DynamicForm jobForm;
        private WorkflowMaterialView materialView;
        private ListGrid taskView;

        public WorkflowJobFormView(ClientMessages i18n) {
            this.i18n = i18n;
            this.widget = createMainLayout();
        }

        public Canvas getWidget() {
            return widget;
        }

        public void setJob(Record job) {
            if (job != null) {
                String jobId = job.getAttribute(WorkflowJobDataSource.FIELD_ID);
                jobForm.editRecord(job);
                taskView.fetchData(new Criteria(
                        WorkflowModelConsts.TASK_FILTER_JOBID, jobId
                ));
                materialView.getMaterialGrid().invalidateCache();
                materialView.getMaterialGrid().fetchData(
                        new Criteria(WorkflowModelConsts.MATERIALFILTER_JOBID, jobId));
            } else {
                jobForm.clearValues();
                materialView.getMaterialGrid().setData(new Record[0]);
                taskView.setData(new Record[0]);
            }
        }

        private Canvas createMainLayout() {
            VLayout main = new VLayout();
            main.addMember(createForm());
            main.addMember(createTaskList());
            main.addMember(createMaterialList());
            return main;
        }

        private Widget createForm() {
            jobForm = new DynamicForm();
            jobForm.setDataSource(WorkflowJobDataSource.getInstance());
            jobForm.setNumCols(3);
            jobForm.setColWidths("*", "*", "*");
            jobForm.setTitleOrientation(TitleOrientation.TOP);

            SelectItem owner = new SelectItem(WorkflowJobDataSource.FIELD_OWNER);
            owner.setOptionDataSource(UserDataSource.getInstance());
            owner.setValueField(UserDataSource.FIELD_ID);
            owner.setDisplayField(UserDataSource.FIELD_USERNAME);

            AutoFitTextAreaItem note = new AutoFitTextAreaItem(WorkflowJobDataSource.FIELD_NOTE);
            note.setStartRow(true);
            note.setColSpan("*");
            note.setWidth("*");
            
            TextItem label = new TextItem(WorkflowJobDataSource.FIELD_LABEL);
            label.setColSpan("*");
            label.setWidth("*");

            jobForm.setFields(label,
                    new SelectItem(WorkflowJobDataSource.FIELD_STATE),
                    new SelectItem(WorkflowJobDataSource.FIELD_PRIORITY),
                    new TextItem(WorkflowJobDataSource.FIELD_PROFILE_ID),
                    owner,
                    new TextItem(WorkflowJobDataSource.FIELD_FINANCED),
                    new TextItem(WorkflowJobDataSource.FIELD_ID),
                    new TextItem(WorkflowJobDataSource.FIELD_CREATED),
                    new TextItem(WorkflowJobDataSource.FIELD_MODIFIED),
                    note
                    );
            return jobForm;
        }

        private Widget createTaskList() {
            taskView = new ListGrid();
            taskView.setCanSort(true);
            taskView.setDataFetchMode(FetchMode.BASIC);

            ResultSet rs = new ResultSet();
            rs.setCriteriaPolicy(CriteriaPolicy.DROPONCHANGE);
            rs.setUseClientFiltering(false);
            rs.setUseClientSorting(true);
            taskView.setDataProperties(rs);

            taskView.setDataSource(WorkflowTaskDataSource.getInstance(),
                    new ListGridField(WorkflowTaskDataSource.FIELD_LABEL),
                    new ListGridField(WorkflowTaskDataSource.FIELD_OWNER, 50),
                    new ListGridField(WorkflowTaskDataSource.FIELD_STATE, 50)
            );
            return taskView;
        }

        private Widget createMaterialList() {
            materialView = new WorkflowMaterialView(i18n, true);
            return materialView.getWidget();
        }

    }

}
