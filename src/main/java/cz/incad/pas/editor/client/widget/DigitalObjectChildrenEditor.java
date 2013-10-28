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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.pas.editor.client.widget;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.data.events.DataChangedEvent;
import com.smartgwt.client.data.events.DataChangedHandler;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IconButton;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.RecordDropEvent;
import com.smartgwt.client.widgets.grid.events.RecordDropHandler;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedEvent;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.IconMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.events.ItemClickEvent;
import com.smartgwt.client.widgets.menu.events.ItemClickHandler;
import cz.incad.pas.editor.client.ClientMessages;
import cz.incad.pas.editor.client.ClientUtils;
import cz.incad.pas.editor.client.action.Action;
import cz.incad.pas.editor.client.action.ActionEvent;
import cz.incad.pas.editor.client.action.Actions;
import cz.incad.pas.editor.client.action.Actions.ActionSource;
import cz.incad.pas.editor.client.action.DeleteAction;
import cz.incad.pas.editor.client.action.DigitalObjectEditAction;
import cz.incad.pas.editor.client.action.DigitalObjectFormValidateAction;
import cz.incad.pas.editor.client.action.DigitalObjectFormValidateAction.ValidatableList;
import cz.incad.pas.editor.client.action.RefreshAction.Refreshable;
import cz.incad.pas.editor.client.action.SaveAction;
import cz.incad.pas.editor.client.action.Selectable;
import cz.incad.pas.editor.client.ds.DigitalObjectDataSource;
import cz.incad.pas.editor.client.ds.DigitalObjectDataSource.DigitalObject;
import cz.incad.pas.editor.client.ds.MetaModelDataSource;
import cz.incad.pas.editor.client.ds.MetaModelDataSource.MetaModelRecord;
import cz.incad.pas.editor.client.ds.RelationDataSource;
import cz.incad.pas.editor.client.ds.RelationDataSource.RelationChangeEvent;
import cz.incad.pas.editor.client.ds.RelationDataSource.RelationChangeHandler;
import cz.incad.pas.editor.client.ds.RestConfig;
import cz.incad.pas.editor.client.presenter.DigitalObjectEditing;
import cz.incad.pas.editor.client.presenter.DigitalObjectEditing.DigitalObjectEditorPlace;
import cz.incad.pas.editor.client.presenter.DigitalObjectEditor;
import cz.incad.pas.editor.shared.rest.DigitalObjectResourceApi.DatastreamEditorType;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Edits children objects of the digital object.
 * It allows to change order of children, to edit a selected child using
 * particular data stream editor.
 *
 * @author Jan Pokorsky
 */
public final class DigitalObjectChildrenEditor
        implements DatastreamEditor, Refreshable, Selectable<Record> {

    private static final Logger LOG = Logger.getLogger(DigitalObjectChildrenEditor.class.getName());
    private final ClientMessages i18n;
    /** A controller of the enclosing editor. */
    private final PlaceController places;
    private final ListGrid childrenListGrid;
    private final DigitalObjectEditor childEditor;
    private final HLayout widget;
    private DigitalObject digitalObject;
    private final PlaceController childPlaces;
    private HandlerRegistration listDataChangedHandler;
    private Record[] originChildren;
    private IconMenuButton addActionButton;
    private IconButton saveActionButton;
    private HandlerRegistration childrenSelectionHandler;
    private RelationDataSource relationDataSource;
    /** Notifies changes in list of children that should be reflected by actions. */
    private final ActionSource actionSource;

    public DigitalObjectChildrenEditor(ClientMessages i18n, PlaceController places) {
        this.i18n = i18n;
        this.places = places;
        this.actionSource = new ActionSource(this);
        relationDataSource = RelationDataSource.getInstance();
        childrenListGrid = initChildrenListGrid();
        VLayout childrenLayout = new VLayout();
        childrenLayout.setMembers(childrenListGrid);
        childrenLayout.setWidth("40%");

        SimpleEventBus eventBus = new SimpleEventBus();
        childPlaces = new PlaceController(eventBus);
        childEditor = new DigitalObjectEditor(i18n, childPlaces, true);
        ActivityManager activityManager = new ActivityManager(
                new ChildActivities(childEditor), eventBus);

        VLayout editorsLayout = new VLayout();
        VLayout editorsOuterLayout = new VLayout();
//        editorsLayout.setBorder("1px solid grey");
        editorsLayout.addStyleName("defaultBorder");
        editorsOuterLayout.setLayoutLeftMargin(4);
        editorsOuterLayout.setMembers(editorsLayout);
        activityManager.setDisplay(new ChildEditorDisplay(editorsLayout));

        widget = new HLayout();
        widget.setMembers(childrenLayout, editorsOuterLayout);
        relationDataSource.addRelationChangeHandler(new RelationChangeHandler() {

            @Override
            public void onRelationChange(RelationChangeEvent event) {
                if (digitalObject != null && childrenListGrid.isVisible()) {
                    relationDataSource.updateCaches(digitalObject.getPid());
                }
            }
        });
    }

    @Override
    public void edit(DigitalObject digitalObject) {
        this.digitalObject = digitalObject;
        if (digitalObject == null) {
            return ;
        }
        detachListFromEditor();
        detachListResultSet();
        String pid = digitalObject.getPid();
        Criteria criteria = new Criteria(RelationDataSource.FIELD_ROOT, pid);
        criteria.addCriteria(RelationDataSource.FIELD_PARENT, pid);
        ResultSet resultSet = childrenListGrid.getResultSet();
        if (resultSet != null) {
            Boolean willFetchData = resultSet.willFetchData(criteria);
            // init editor for cached record when DataArrivedHandler is not called
            if (!willFetchData) {
                initOnEdit();
            }
        }
        // use DataArrivedHandler instead of callback as it is not called
        // for refresh in SmartGWT 3.0
        childrenListGrid.fetchData(criteria);
        MetaModelDataSource.getModels(false, new Callback<ResultSet, Void>() {

            @Override
            public void onFailure(Void reason) {
            }

            @Override
            public void onSuccess(ResultSet result) {
                Map<?,?> valueMap = result.getValueMap(
                        MetaModelDataSource.FIELD_PID, MetaModelDataSource.FIELD_DISPLAY_NAME);
                childrenListGrid.getField(RelationDataSource.FIELD_MODEL).setValueMap(valueMap);
                createAddMenu(result);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Class<T> clazz) {
        T c = null;
        if (Refreshable.class.equals(clazz)) {
            c = (T) this;
        }
        return c;
    }

    @Override
    public Canvas[] getToolbarItems() {
        Action addAction = Actions.emptyAction(i18n.DigitalObjectEditor_ChildrenEditor_AddAction_Title(),
                "[SKIN]/actions/add.png",
                i18n.DigitalObjectEditor_ChildrenEditor_AddAction_Hint());
        Action saveAction = new SaveAction(i18n) {

            @Override
            public void performAction(ActionEvent event) {
                save();
            }
        };
        DeleteAction deleteAction = new DeleteAction(DigitalObjectDataSource.createDeletable(), i18n);
        addActionButton = Actions.asIconMenuButton(addAction, this);
        return new Canvas[] {
            addActionButton,
            Actions.asIconButton(deleteAction, actionSource),
            Actions.asIconButton(new DigitalObjectEditAction(
                        i18n.DigitalObjectEditor_ChildrenEditor_ChildAction_Title(),
                        i18n.DigitalObjectEditor_ChildrenEditor_ChildAction_Hint(),
                        "[SKIN]/FileBrowser/folder.png",
                        DatastreamEditorType.CHILDREN, places),
                actionSource),
            Actions.asIconButton(DigitalObjectFormValidateAction.getInstance(i18n),
                    new ValidatableList(childrenListGrid)),
            saveActionButton = Actions.asIconButton(saveAction, this),
        };
    }

    @Override
    public Canvas getUI() {
        return widget;
    }

    @Override
    public void refresh() {
        ValidatableList.clearRowErrors(childrenListGrid);
        childrenListGrid.invalidateCache();
        edit(digitalObject);
    }

    @Override
    public Record[] getSelection() {
        return originChildren != null ? null : childrenListGrid.getSelectedRecords();
    }

    private void save() {
        if (originChildren == null) {
            return ;
        }
        Record[] rs = childrenListGrid.getOriginalResultSet().toArray();
        String[] childPids = ClientUtils.toFieldValues(rs, RelationDataSource.FIELD_PID);
        relationDataSource.reorderChildren(digitalObject, childPids, new BooleanCallback() {

            @Override
            public void execute(Boolean value) {
                if (value != null && value) {
                    originChildren = null;
                    updateReorderUi(false);
                    StatusView.getInstance().show(i18n.SaveAction_Done_Msg());
                }
            }
        });
    }

    /**
     * Handles a new children selection.
     */
    private void onChildSelection(Record[] records) {
        actionSource.fireEvent();
        if (records == null || records.length == 0 || originChildren != null) {
            childPlaces.goTo(Place.NOWHERE);
        } else {
            Place lastPlace = childPlaces.getWhere();
            DatastreamEditorType lastEditorType = null;
            if (lastPlace instanceof DigitalObjectEditorPlace) {
                DigitalObjectEditorPlace lastDOEPlace = (DigitalObjectEditorPlace) lastPlace;
                lastEditorType = lastDOEPlace.getEditorId();
            }
            lastEditorType = lastEditorType != null
                    ? lastEditorType
                    : records.length > 1 ? DatastreamEditorType.PARENT : DatastreamEditorType.MODS;
            childPlaces.goTo(new DigitalObjectEditorPlace(lastEditorType, records));
        }
    }

    private ListGrid initChildrenListGrid() {
        ListGrid lg = new ListGrid();
        lg.setDataSource(relationDataSource);
        lg.setFields(
                new ListGridField(RelationDataSource.FIELD_LABEL,
                        i18n.DigitalObjectSearchView_ListHeaderLabel_Title()),
                new ListGridField(RelationDataSource.FIELD_MODEL,
                        i18n.DigitalObjectSearchView_ListHeaderModel_Title()),
                new ListGridField(RelationDataSource.FIELD_PID,
                        i18n.DigitalObjectSearchView_ListHeaderPid_Title())
                );
        lg.getField(RelationDataSource.FIELD_PID).setHidden(true);
        lg.setCanSort(Boolean.FALSE);
        lg.setCanReorderRecords(Boolean.TRUE);
        lg.setShowRollOver(Boolean.FALSE);
        // ListGrid with enabled grouping prevents record reoredering by dragging! (SmartGWT 3.0)
        // lg.setGroupByField(RelationDataSource.FIELD_MODEL);
        // lg.setGroupStartOpen(GroupStartOpen.ALL);
        lg.addRecordDropHandler(new RecordDropHandler() {

            @Override
            public void onRecordDrop(RecordDropEvent event) {
                Record[] records = childrenListGrid.getOriginalResultSet().toArray();
                if (originChildren == null) {
                    // takes the list snapshot before first reorder
                    originChildren = records;
                }
            }
        });
        lg.addDataArrivedHandler(new DataArrivedHandler() {

            @Override
            public void onDataArrived(DataArrivedEvent event) {
                if (event.getStartRow() == 0) {
                    initOnEdit();
                }
            }
        });
        return lg;
    }

    private void initOnEdit() {
//        LOG.info("initOnEdit");
        originChildren = null;
        updateReorderUi(false);
        attachListResultSet();
        // select first
        if (!childrenListGrid.getOriginalResultSet().isEmpty()) {
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                @Override
                public void execute() {
                    // defer the select as it is ignored after refresh in onDataArrived
                    childrenListGrid.scrollToRow(0);
                    childrenListGrid.selectSingleRecord(0);
                    childrenListGrid.focus();
                }
            });
        }
    }

    /**
     * Opens child editor according to list selection.
     */
    private void attachListToEditor() {
        childrenSelectionHandler = childrenListGrid.addSelectionUpdatedHandler(new SelectionUpdatedHandler() {

            @Override
            public void onSelectionUpdated(SelectionUpdatedEvent event) {
                ListGridRecord[] records = childrenListGrid.getSelectedRecords();
                onChildSelection(records);
            }
        });
    }

    private void detachListFromEditor() {
        if (childrenSelectionHandler != null) {
            childrenSelectionHandler.removeHandler();
            childrenSelectionHandler = null;
            childPlaces.goTo(Place.NOWHERE);
        }
    }

    /**
     * Listens to changes of original list result set (reorder, fetch).
     * It tracks order changes together with {@link ListGrid#addRecordDropHandler }
     * in {@link #initChildrenListGrid() }
     */
    private void attachListResultSet() {
        listDataChangedHandler = childrenListGrid.getOriginalResultSet().addDataChangedHandler(new DataChangedHandler() {

            @Override
            public void onDataChanged(DataChangedEvent event) {
                if (originChildren != null) {
                    // compare origin with new children
                    boolean equals = RelationDataSource.equals(
                            originChildren,
                            childrenListGrid.getOriginalResultSet().toArray());
                    // enable Save button
                    // disable remove and add
                    updateReorderUi(!equals);
                }
            }
        });
    }

    private void detachListResultSet() {
        if (listDataChangedHandler != null) {
            listDataChangedHandler.removeHandler();
            listDataChangedHandler = null;
        }
    }

    private void updateReorderUi(boolean reordered) {
        actionSource.fireEvent();
        saveActionButton.setVisible(reordered);
        addActionButton.setVisible(!reordered);
        if (reordered) {
            detachListFromEditor();
        } else {
            attachListToEditor();
        }
    }

    private void createAddMenu(ResultSet models) {
        Menu menuAdd = MetaModelDataSource.createMenu(models, false);
        addActionButton.setMenu(menuAdd);
        menuAdd.addItemClickHandler(new ItemClickHandler() {

            @Override
            public void onItemClick(ItemClickEvent event) {
                MetaModelRecord mmr = MetaModelRecord.get(event.getItem());
                addChild(mmr);
            }
        });
    }

    private void addChild(MetaModelRecord model) {
        Record record = new Record();
        record.setAttribute(DigitalObjectDataSource.FIELD_MODEL, model.getId());
        record.setAttribute(RelationDataSource.FIELD_PARENT, digitalObject.getPid());
        DigitalObjectDataSource.getInstance().addData(record, new DSCallback() {

            @Override
            public void execute(DSResponse response, Object rawData, DSRequest request) {
                if (RestConfig.isStatusOk(response)) {
                    Record[] data = response.getData();
                    final Record r = data[0];
                    DSRequest dsRequest = new DSRequest();
                    dsRequest.setOperationType(DSOperationType.ADD);
                    RelationDataSource.getInstance().updateCaches(response, dsRequest);
                    Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                        @Override
                        public void execute() {
                            childrenListGrid.selectSingleRecord(r);
                            int recordIndex = childrenListGrid.getRecordIndex(r);
                            childrenListGrid.scrollToRow(recordIndex);
                        }
                    });
                }
            }
        });
    }

    public static final class ChildActivities implements ActivityMapper {

        private final DigitalObjectEditor childEditor;

        public ChildActivities(DigitalObjectEditor childEditor) {
            this.childEditor = childEditor;
        }

        @Override
        public Activity getActivity(Place place) {
            if (place instanceof DigitalObjectEditorPlace) {
                DigitalObjectEditorPlace editorPlace = (DigitalObjectEditorPlace) place;
                return new DigitalObjectEditing(editorPlace, childEditor);
            }
            return null;
        }

    }

    public static final class ChildEditorDisplay implements AcceptsOneWidget {

        private final Layout display;

        public ChildEditorDisplay(Layout display) {
            this.display = display;
        }

        @Override
        public void setWidget(IsWidget w) {
            Widget asWidget = Widget.asWidgetOrNull(w);
            if (asWidget instanceof Canvas) {
                display.setMembers((Canvas) asWidget);
            } else if (asWidget == null) {
                display.removeMembers(display.getMembers());
            } else {
                throw new IllegalStateException("Unsupported widget: " + asWidget.getClass());
            }
        }

    }

}
