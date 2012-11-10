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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.ColorPicker;
import com.smartgwt.client.widgets.form.events.ColorSelectedEvent;
import com.smartgwt.client.widgets.form.events.ColorSelectedHandler;
import cz.incad.pas.editor.client.ClientMessages;
import cz.incad.pas.editor.client.ClientUtils;
import cz.incad.pas.editor.client.action.AbstractAction;
import cz.incad.pas.editor.client.action.Action;
import cz.incad.pas.editor.client.action.ActionEvent;
import cz.incad.pas.editor.client.action.Actions;
import cz.incad.pas.editor.client.action.RefreshAction.Refreshable;
import cz.incad.pas.editor.client.ds.MetaModelDataSource.MetaModelRecord;
import cz.incad.pas.editor.client.ds.RestConfig;
import cz.incad.pas.editor.shared.rest.DigitalObjectResourceApi;
import java.util.ArrayList;

/**
 * Edits data streams containing digitized multimedia content.
 *
 * <p>For now it just shows the content. Later it could allow to upload content.
 *
 * @author Jan Pokorsky
 */
public final class MediaEditor implements DatastreamEditor, Refreshable {

    private final DigitalObjectPreview doPreview;
    private String imgParams;
    private final ArrayList<Canvas> backgroundListeners = new ArrayList<Canvas>();
    private Action fullAction;
    private AbstractAction rawAction;
    private AbstractAction backgroundAction;

    public MediaEditor(ClientMessages i18n) {
        doPreview = new DigitalObjectPreview(i18n);
        initActions(i18n);
    }

    @Override
    public void edit(String pid, String batchId, MetaModelRecord model) {
        if (pid == null) {
            throw new NullPointerException();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(DigitalObjectResourceApi.DIGITALOBJECT_PID).append('=').append(pid);
        if (batchId != null) {
            sb.append('&').append(DigitalObjectResourceApi.BATCHID_PARAM).append('=').append(batchId);
        }
        imgParams = sb.toString();
        String previewUrl = buildResourceUrl(RestConfig.URL_DIGOBJECT_PREVIEW, imgParams);
        doPreview.show(previewUrl);
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
        return new Canvas[] {
            Actions.asIconButton(fullAction, this),
            Actions.asIconButton(rawAction, this),
            Actions.asIconButton(backgroundAction, this),
            doPreview.getPreviewZoomer(),
        };
    }

    @Override
    public Canvas getUI() {
        return doPreview.asCanvas();
    }

    @Override
    public void refresh() {
        String previewUrl = buildResourceUrl(RestConfig.URL_DIGOBJECT_PREVIEW, imgParams);
        doPreview.show(previewUrl);
//        doPreview.show(previewUrl + "&reload=" + System.currentTimeMillis());
    }

    public void addBackgroundColorListeners(Canvas c) {
        backgroundListeners.add(c);
    }

    public void setBackgroundColor(String color) {
        doPreview.setBackgroundColor(color);
        for (Canvas canvas : backgroundListeners) {
            canvas.setBackgroundColor(color);
        }
    }

    private void initActions(ClientMessages i18n) {
        fullAction = new AbstractAction(
                i18n.DigitalObjectPreview_ViewFullAction_Title(),
                "[SKIN]/actions/view.png",
                i18n.DigitalObjectPreview_ViewFullAction_Hint()) {

            @Override
            public void performAction(ActionEvent event) {
                String url = buildResourceUrl(RestConfig.URL_DIGOBJECT_FULL, imgParams);
                doPreview.showInWindow(url);
            }
        };

        rawAction = new AbstractAction(
                i18n.DigitalObjectPreview_ViewRawAction_Title(),
                "[SKIN]/actions/download.png",
                i18n.DigitalObjectPreview_ViewRawAction_Hint()) {

            @Override
            public void performAction(ActionEvent event) {
                String url = buildResourceUrl(RestConfig.URL_DIGOBJECT_RAW, imgParams);
                doPreview.showInNewWindow(url);
            }
        };

        backgroundAction = new AbstractAction(
                i18n.DigitalObjectPreview_ColorChooserAction_Title(),
                "[SKIN]/actions/color_swatch.png",
                i18n.DigitalObjectPreview_ColorChooserAction_Hint()) {

            @Override
            public void performAction(ActionEvent event) {
                ColorPicker picker = new ColorPicker();
                picker.addColorSelectedHandler(new ColorSelectedHandler() {

                    @Override
                    public void onColorSelected(ColorSelectedEvent event) {
                        setBackgroundColor(event.getColor());
                    }
                });
                picker.setDefaultColor(DigitalObjectPreview.BACKGROUND_COLOR);
                picker.setKeepInParentRect(true);
                picker.show();
            }
        };
    }

    /**
     *
     * @param datastreamUrl data stream URL
     * @param objectParams PID and batch ID as URL parameters
     * @return URL
     */
    private static String buildResourceUrl(String datastreamUrl, String objectParams) {
        String url = ClientUtils.format("%s?%s", datastreamUrl, objectParams);
        return url;
    }

}
