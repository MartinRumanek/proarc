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

import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Cursor;
import com.smartgwt.client.types.DragAppearance;
import com.smartgwt.client.types.HeaderControls;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.WidgetCanvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.DragMoveEvent;
import com.smartgwt.client.widgets.events.DragMoveHandler;
import com.smartgwt.client.widgets.events.DragStartEvent;
import com.smartgwt.client.widgets.events.DragStartHandler;
import com.smartgwt.client.widgets.events.DragStopEvent;
import com.smartgwt.client.widgets.events.DragStopHandler;
import com.smartgwt.client.widgets.events.DrawEvent;
import com.smartgwt.client.widgets.events.DrawHandler;
import com.smartgwt.client.widgets.events.ResizedEvent;
import com.smartgwt.client.widgets.events.ResizedHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ClientUtils;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shows preview of digital objects.
 *
 * @author Jan Pokorsky
 */
public final class DigitalObjectPreview {

    // darker variant #E6E6F5
    public static final String BACKGROUND_COLOR = "#F5F5FB";
    private static final Logger LOG = Logger.getLogger(DigitalObjectPreview.class.getName());
    
    private final ClientMessages i18n;
    private final VLayout previewLayout;
    private Window imageWindow;
    private final ImageLoadTask windowLoadTask;
    private final ImageLoadTask previewLoadTask;
    private Zoom zoom;

    public DigitalObjectPreview(ClientMessages i18n) {
        this.i18n = i18n;
        this.zoom = new Zoom();
        VLayout imgContainer = new VLayout();
        imgContainer.setLayoutMargin(4);
        imgContainer.setAlign(Alignment.CENTER);
        imgContainer.setOverflow(Overflow.AUTO);

        VLayout windowContainer = new VLayout();
        windowContainer.setOverflow(Overflow.AUTO);
        windowContainer.setAlign(Alignment.CENTER);
        windowContainer.setWidth100();
        windowContainer.setHeight100();
        windowLoadTask = new ImageLoadTask(windowContainer, new Zoom(Zoom.ZOOM_PREFIX + "100"), true, i18n);
        imageWindow = createFullImageWindow(windowContainer);

        previewLayout = new VLayout();
        previewLayout.setBackgroundColor(BACKGROUND_COLOR);
        previewLayout.addMember(imgContainer);
        previewLoadTask = new ImageLoadTask(imgContainer, zoom, false, i18n);
    }

    public Canvas asCanvas() {
        return previewLayout;
    }

    public void show(String previewUrl) {
        if (previewUrl == null) {
            Layout container = previewLoadTask.getImgContainer();
            container.removeMembers(container.getMembers());
            previewLoadTask.stop();
        } else {
            previewLoadTask.loadImage(previewUrl);
        }
    }

    /**
     * Gets widget to zoom image in the preview panel.
     * @return
     */
    public Canvas getPreviewZoomer() {
        SelectItem zoomItem = createZoomForm();
        zoomItem.addChangedHandler(new ChangedHandler() {

            @Override
            public void onChanged(ChangedEvent event) {
                zoom = new Zoom(String.valueOf(event.getValue()));
                previewLoadTask.resize(zoom);
            }
        });
        DynamicForm form = new DynamicForm();
        form.setFields(zoomItem);
        form.setLayoutAlign(Alignment.CENTER);
        return form;
    }

    /**
     * Gets widget to zoom image in the window.
     * @return
     */
    public Canvas getWindowZoomer() {
        SelectItem zoomItem = createZoomForm();
        zoomItem.setDefaultValue(windowLoadTask.getZoom().getValue());
        zoomItem.setHeight(15);
        zoomItem.setPickerIconSrc("[SKIN]/headerIcons/zoom.png");
        zoomItem.setPickerIconHeight(15);
        zoomItem.setPickerIconWidth(15);
        zoomItem.addChangedHandler(new ChangedHandler() {

            @Override
            public void onChanged(ChangedEvent event) {
                Zoom windowZoom = new Zoom(String.valueOf(event.getValue()));
                windowLoadTask.resize(windowZoom);
            }
        });
        DynamicForm form = new DynamicForm();
        form.setFields(zoomItem);
        form.setLayoutAlign(Alignment.CENTER);
        return form;
    }

    private SelectItem createZoomForm() {
        SelectItem selectItem = new SelectItem();
        selectItem.setShowTitle(Boolean.FALSE);
        selectItem.setValueMap(Zoom.getValueMap(i18n));
        selectItem.setDefaultValue(Zoom.FIT_PANEL);
        return selectItem;
    }

    private Window createFullImageWindow(Canvas content) {
        Window window = new Window();
        window.setWidth(Page.getWidth() - 200);
        window.setHeight(Page.getHeight() - 40);
        window.setAutoCenter(true);
        window.setCanDragResize(true);
        window.setCanDragReposition(true);
        window.setIsModal(true);
        window.setDismissOnEscape(true);
        window.setDismissOnOutsideClick(true);
        window.setKeepInParentRect(true);
        window.setShowMaximizeButton(true);
        window.setShowMinimizeButton(false);

        window.setModalMaskOpacity(10);
        window.setShowModalMask(true);

        window.setShowResizer(true);
        window.setTitle(i18n.DigitalObjectPreview_Window_Title());
        window.setBodyColor(BACKGROUND_COLOR);
        window.setCanFocus(true);
        window.addItem(content);

        window.setHeaderControls(HeaderControls.HEADER_ICON, HeaderControls.HEADER_LABEL,
                getWindowZoomer(), HeaderControls.MAXIMIZE_BUTTON, HeaderControls.CLOSE_BUTTON);
        return window;
    }

    public void setBackgroundColor(String color) {
        previewLayout.setBackgroundColor(color);
        imageWindow.setBodyColor(color);
    }

    public void showInNewWindow(String url) {
        com.google.gwt.user.client.Window.open(url, "_blank", "");
    }

    public void showInWindow(String url) {
        // put focus inside window to enable Window.setDismissOnEscape
        imageWindow.focus();
        windowLoadTask.loadImage(url);
        imageWindow.show();
    }

    /**
     * Scrolls container by dragging the image.
     * @param container image container
     * @param image image
     */
    private static void addContainerMoveListener(final Layout container, final Img image) {
        final int[] lastX = { 0 };
        final int[] lastY = { 0 };
        final Cursor[] cursor = new Cursor[1];
        image.setCanDrag(Boolean.TRUE);
        image.setDragAppearance(DragAppearance.NONE);
        image.addDragStartHandler(new DragStartHandler() {

            @Override
            public void onDragStart(DragStartEvent event) {
                lastX[0] = event.getX();
                lastY[0] = event.getY();
                cursor[0] = image.getCursor();
                image.setCursor(Cursor.MOVE);
                ClientUtils.fine(LOG, "dragStart [%s, %s]", lastX[0], lastY[0]);
            }
        });
        image.addDragMoveHandler(new DragMoveHandler() {

            @Override
            public void onDragMove(DragMoveEvent event) {
                int dx = lastX[0] - event.getX();
                int dy = lastY[0] - event.getY();
                lastX[0] = event.getX();
                lastY[0] = event.getY();
                container.scrollBy(-2 * dx, -2 * dy);
                ClientUtils.fine(LOG, "dragMove: delta[%s, %s], new position[%s, %s]", dx, dy, lastX[0], lastY[0]);
            }
        });
        image.addDragStopHandler(new DragStopHandler() {

            @Override
            public void onDragStop(DragStopEvent event) {
                image.setCursor(cursor[0]);
                ClientUtils.fine(LOG, "dragStop");
            }
        });

    }

    private static final class Zoom {

        public static final String FIT_PANEL = "FitToPanel";
        public static final String FIT_WIDTH = "FitToWidth";
        public static final String FIT_HEIGHT = "FitToHeight";
        private static final String ZOOM_PREFIX = "Zoom-";
        private static LinkedHashMap<String, String> values;

        public static LinkedHashMap<String, String> getValueMap(ClientMessages i18n) {
            if (values == null) {
                values = new LinkedHashMap<String, String>();
                values.put(FIT_PANEL, i18n.DigitalObjectPreview_ZoomFitPanel_Title());
                values.put(FIT_WIDTH, i18n.DigitalObjectPreview_ZoomFitWidth_Title());
                values.put(FIT_HEIGHT, i18n.DigitalObjectPreview_ZoomFitHeight_Title());
                for (int i = 25; i <= 200; i += 25) {
                    values.put(ZOOM_PREFIX + i, i18n.DigitalObjectPreview_ZoomPrefix_Title(String.valueOf(i)));
                }
            }
            return values;
        }

        private final String zoomValue;

        public Zoom() {
            this(FIT_PANEL);
        }

        public Zoom(String zoomValue) {
            this.zoomValue = zoomValue;
        }

        public String getValue() {
            return zoomValue;
        }

        public double ratio(double containerWidth, double containerHeight, double imageWidth, double imageHeight) {
            double ratio;
            double hRatio =  containerHeight / imageHeight;
            double wRatio = containerWidth / imageWidth;

            if (FIT_PANEL.equals(zoomValue)) {
                ratio = Math.min(hRatio, wRatio);
            } else if (FIT_WIDTH.equals(zoomValue)) {
                ratio = wRatio;
            } else if (FIT_HEIGHT.equals(zoomValue)) {
                ratio = hRatio;
            } else if (zoomValue != null && zoomValue.startsWith(ZOOM_PREFIX)) {
                String zoom = zoomValue.substring(ZOOM_PREFIX.length(), zoomValue.length());
                int parsedZoom = Integer.parseInt(zoom);
                ratio = (double) parsedZoom / 100.0;
            } else {
                throw new IllegalStateException(zoomValue);
            }
            ClientUtils.fine(LOG,
                    "wRatio: %s, hRatio: %s, ratio: %s, width: %s, height:%s, iwidth: %s, iheight:%s",
                    wRatio, hRatio, ratio, containerWidth, containerHeight, imageWidth, imageHeight);
            return ratio;
        }

    }

    /**
     * Loads the image to get its parameters in order to zoom and layout it properly.
     */
    private static final class ImageLoadTask extends Timer implements
            LoadHandler, ErrorHandler, DrawHandler, ResizedHandler {

        private final Layout imgContainer;
        private Image image;
        private HandlerRegistration drawHandler;
        private HandlerRegistration resizedHandler;
        private Zoom zoom;
        private final boolean focus;
        private final ClientMessages i18n;
        private boolean loadFailed;
        /**
         * Last ratio of horizontal scrollbar and image width to keep
         * scrollbar position between image reloads. It should help read e.g. page numbers.
         */
        private double scrollHorizontal;
        /**
         * Last ratio of vertical scrollbar and image height to keep
         * scrollbar position between image reloads. It should help read e.g. page numbers.
         */
        private double scrollVertical;

        public ImageLoadTask(Layout imgContainer, Zoom zoom, boolean focus, ClientMessages i18n) {
            this.imgContainer = imgContainer;
            this.i18n = i18n;
            this.zoom = zoom;
            this.focus = focus;
        }

        public Layout getImgContainer() {
            return imgContainer;
        }

        public Zoom getZoom() {
            return zoom;
        }

        public void loadImage(String url) {
            stop();
            loadFailed = false;
            image = new Image();
            image.addLoadHandler(this);
            image.addErrorHandler(this);
            image.setUrl(url);
            drawHandler = imgContainer.addDrawHandler(this);
            resizedHandler = imgContainer.addResizedHandler(this);
            ClientUtils.fine(LOG, "loadImage url: %s, width: %s", url, image.getWidth());
            if (image.getWidth() == 0) {
                WidgetCanvas widgetCanvas = new WidgetCanvas(image);
                widgetCanvas.setVisible(false);
                widgetCanvas.setWidth(1);
                widgetCanvas.setHeight(1);
                widgetCanvas.draw();
                Img loadingImg = new Img("[SKIN]/loadingSmall.gif", 16, 16);
//                Img loadingImg = new Img("[SKIN]/shared/progressCursorTracker.gif", 16, 16);
                loadingImg.setAltText(i18n.ImportBatchDataSource_State_LOADING());
                loadingImg.setPrompt(i18n.ImportBatchDataSource_State_LOADING());
                loadingImg.setLayoutAlign(Alignment.CENTER);
                imgContainer.setMembers(loadingImg, widgetCanvas);
            }
            scheduleForRender();
        }

        public void stop() {
            if (drawHandler != null) {
                drawHandler.removeHandler();
                resizedHandler.removeHandler();
            }
            cancel();
            scrollHorizontal = (double) imgContainer.getScrollLeft() / (double) imgContainer.getWidth();
            scrollVertical = (double) imgContainer.getScrollTop() / (double) imgContainer.getHeight();
            ClientUtils.fine(LOG, "stop: [%s, %s]", scrollHorizontal, scrollVertical);
        }

        private void scheduleForRender() {
            if (image.getWidth() == 0) {
                return ;
            } else if (imgContainer.isDirty() || !imgContainer.isDrawn()) {
                return ;
            }
            schedule(100);
        }

        public void render() {
            if (loadFailed) {
                return ;
            }

            double ratio = zoom.ratio(
                    imgContainer.getInnerWidth(), imgContainer.getInnerHeight(),
                    image.getWidth(), image.getHeight());

            double width = (double) image.getWidth() * ratio;
            double height = (double) image.getHeight() * ratio;
            log("render", width, height);

            // do not try to center horizontally as browsers crop large images in small containers
            Img img = new Img(image.getUrl(),
                    (int) width - imgContainer.getScrollbarSize() - 4,
                    (int) height - imgContainer.getScrollbarSize() - 4);
            img.setCanFocus(Boolean.TRUE);
            imgContainer.setMembers(img);
            imgContainer.adjustForContent(true);
            int scrollLeft = (int) (imgContainer.getWidth() * scrollHorizontal);
            int scrollTop = (int) (imgContainer.getHeight() * scrollVertical);
            imgContainer.scrollTo(scrollLeft, scrollTop);
            addContainerMoveListener(imgContainer, img);
            if (focus) {
                img.focus();
            }
        }

        public void resize(Zoom zoom) {
            this.zoom = zoom;
            if (loadFailed) {
                return ;
            }
            if (image.getWidth() != 0) {
                final Canvas img = imgContainer.getMember(0);
                scrollHorizontal = (double) imgContainer.getScrollLeft() / (double) imgContainer.getWidth();
                scrollVertical = (double) imgContainer.getScrollTop() / (double) imgContainer.getHeight();
                double ratio = zoom.ratio(
                        imgContainer.getInnerWidth(), imgContainer.getInnerHeight(),
                        image.getWidth(), image.getHeight());
                double width = (double) image.getWidth() * ratio;
                double height = (double) image.getHeight() * ratio;
                log("resize", width, height);
                img.animateResize((int) width - imgContainer.getScrollbarSize() - 4,
                        (int) height - imgContainer.getScrollbarSize() - 4,
                        new AnimationCallback() {

                    @Override
                    public void execute(boolean earlyFinish) {
                        if (focus) {
                            img.focus();
                        }
                        log("after resize.earlyFinish: " + earlyFinish, 0, 0);
                    }
                });
            }
        }

        private void log(String msg, double width, double height) {
            if (LOG.isLoggable(Level.FINE)) {
                ClientUtils.fine(LOG, "%s: %s,"
                        + "\nscrollbar: %s"
                        + "\nimage[%s, %s] => [%s, %s],"
                        + "\nimageContainer.visible: %s, drawn: %s, attached: %s, dirty: %s,"
                        + "\nsize[%s, %s], innerSize[%s, %s], innerContentSize[%s, %s], viewport[%s, %s], visible[%s, %s]"
                        + "\nscrollCurrent[%s, %s], scrollSize[%s, %s]",
                        msg,
                        image.getUrl(),
                        imgContainer.getScrollbarSize(),
                        image.getWidth(), image.getHeight(), (int) width, (int) height,
                        imgContainer.isVisible(), imgContainer.isDrawn(), imgContainer.isAttached(), imgContainer.isDirty(),
                        imgContainer.getWidth(), imgContainer.getHeight(),
                        imgContainer.getInnerWidth(), imgContainer.getInnerHeight(),
                        imgContainer.getInnerContentWidth(), imgContainer.getInnerContentHeight(),
                        imgContainer.getViewportWidth(), imgContainer.getViewportHeight(),
                        imgContainer.getVisibleWidth(), imgContainer.getVisibleHeight(),
                        imgContainer.getScrollLeft(), imgContainer.getScrollTop(),
                        imgContainer.getScrollWidth(), imgContainer.getScrollHeight()
                        );
            }
        }

        @Override
        public void onLoad(LoadEvent event) {
            ClientUtils.fine(LOG, "image onLoad: %s", image.getUrl());
            scheduleForRender();
        }

        @Override
        public void onError(ErrorEvent event) {
            loadFailed = true;
            ClientUtils.warning(LOG, "image onError: %s", image.getUrl());
            Img img = new Img("[SKIN]/Dialog/warn.png", 2 * 16, 2 * 16);
            img.setLayoutAlign(Alignment.CENTER);
            img.setAltText(i18n.ImportBatchDataSource_State_LOADING_FAILED());
            img.setPrompt(i18n.ImportBatchDataSource_State_LOADING_FAILED());
            imgContainer.setMembers(img);
        }

        @Override
        public void onDraw(DrawEvent event) {
            ClientUtils.fine(LOG, "image onDraw: %s", image.getUrl());
            scheduleForRender();
        }

        @Override
        public void onResized(ResizedEvent event) {
            ClientUtils.fine(LOG, "image onResized: %s", image.getUrl());
            scheduleForRender();
        }

        @Override
        public void run() {
            render();
        }

    }

}
