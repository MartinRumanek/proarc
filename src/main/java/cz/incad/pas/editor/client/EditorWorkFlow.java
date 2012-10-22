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
package cz.incad.pas.editor.client;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.Layout;
import cz.incad.pas.editor.client.presenter.DigitalObjectEditing;
import cz.incad.pas.editor.client.presenter.DigitalObjectEditing.DigitalObjectEditorPlace;

/**
 * Implements editor workflow using GWT Places, Activities and History support.
 *
 * @author Jan Pokorsky
 */
public final class EditorWorkFlow {

    private final EventBus ebus;
    private final PlaceController placeController;
    private final ActivityManager activityManager;
    private final PasEditorMessages i18n;

    public EditorWorkFlow(Layout delegate, PasEditorMessages i18n) {
        this(null, null, null, delegate, i18n);
    }
    
    public EditorWorkFlow(EventBus ebus, PlaceController placeController,
            ActivityManager activityManager, Layout delegate, PasEditorMessages i18n) {

        this.i18n = i18n;
        this.ebus = (ebus != null) ? ebus : new SimpleEventBus();
        // PlaceController uses delegate to ask user with blocking Window.confirm
        // whether to leave the current place.
        // In order to use non blocking SmartGWT dialog
        // it will be necessary to override PlaceController.goto method.
        this.placeController = (placeController != null) ? placeController
                : new PlaceController(this.ebus);
        this.activityManager = (activityManager != null) ? activityManager
                : new ActivityManager(new EditorActivityMapper(), this.ebus);
        this.activityManager.setDisplay(new EditorDisplay(delegate));
        EditorPlaceHistoryMapper historyMapper = GWT.create(EditorPlaceHistoryMapper.class);
        PlaceHistoryHandler placeHistoryHandler = new PlaceHistoryHandler(historyMapper);
        placeHistoryHandler.register(this.placeController, this.ebus, Place.NOWHERE);
        placeHistoryHandler.handleCurrentHistory();
    }

    public PlaceController getPlaceController() {
        return placeController;
    }

    final class EditorActivityMapper implements ActivityMapper {

        @Override
        public Activity getActivity(Place place) {
            Activity a = null;
            if (place instanceof DigitalObjectEditorPlace) {
                a = new DigitalObjectEditing((DigitalObjectEditorPlace) place, placeController, i18n);
            }
            return a;
        }

    }

    static final class EditorDisplay implements AcceptsOneWidget {

        private final Layout display;

        public EditorDisplay(Layout display) {
            this.display = display;
        }

        @Override
        public void setWidget(IsWidget w) {
            Widget asWidget = Widget.asWidgetOrNull(w);
            System.out.println("widget: " + asWidget);
            if (asWidget instanceof Canvas) {
                display.setMembers((Canvas) asWidget);
            } else if (asWidget == null) {
                display.removeMembers(display.getMembers());
            } else {
                throw new IllegalStateException("Unsupported widget: " + asWidget.getClass());
            }
        }

    }

    @WithTokenizers({
        DigitalObjectEditorPlace.Tokenizer.class,
    })
    static interface EditorPlaceHistoryMapper extends PlaceHistoryMapper {
    }

}
