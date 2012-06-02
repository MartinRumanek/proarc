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

import com.google.gwt.event.shared.HandlerRegistration;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.data.events.DataArrivedEvent;
import com.smartgwt.client.data.events.DataArrivedHandler;
import com.smartgwt.client.data.events.ErrorEvent;
import com.smartgwt.client.data.events.HandleErrorHandler;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Progressbar;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.layout.VLayout;
import cz.incad.pas.editor.client.ClientUtils;
import cz.incad.pas.editor.client.PasEditorMessages;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shows progress in canvas or window.
 *
 * <p/>Use {@link #setInit() setInit}, {@link #setProgress setProgress}, {@link #setDone setDone}
 * to manually control progress.
 *
 * <p>Or use {@link #setInit() setInit}, {@link #setDataSource setDataSource} to bind data source that
 * supports paging.
 *
 * @author Jan Pokorsky
 */
public final class ProgressTracker {

    private static final Logger LOG = Logger.getLogger(ProgressTracker.class.getName());

    private final VLayout widget;
    private final Progressbar progressbar;
    private final Label label;
    private int lastDone;
    private int lastTotal;
    private DataSource datasource;
    private Criteria criteria;
    private ProgressHandler progressHandler;
    private Runnable exitCallback;
    private Window window;
    private final PasEditorMessages i18nPas;
    private String progressPrefix;

    public ProgressTracker(PasEditorMessages i18nPas) {
        this.i18nPas = i18nPas;
        widget = new VLayout(4);
        label = new Label();
        label.setWidth100();
        label.setAutoHeight();
        progressbar = new Progressbar();
        progressbar.setVertical(false);
        progressbar.setWidth100();
        progressbar.setHeight(24);
        progressbar.setBreadth(1);

        widget.setMembers(label, progressbar);
        widget.setWidth100();
        widget.setAutoHeight();
        progressPrefix = i18nPas.ProgressTracker_Progress_0();
    }

    public VLayout asPanel() {
        return widget;
    }

    public void showInWindow(Runnable exitCallback) {
        showInWindow(exitCallback, i18nPas.ProgressTracker_Window_Title());
    }

    public void showInWindow(Runnable exitCallback, String title) {
        this.exitCallback = exitCallback;
        window = new Window();
        window.setWidth(400);
        window.setAutoSize(true);
        window.setAutoCenter(true);
        window.setIsModal(true);
        widget.setMargin(10);
        window.setTitle(title);
        window.setShowMinimizeButton(false);
        window.setShowModalMask(true);
        window.addCloseClickHandler(new CloseClickHandler() {

            @Override
            public void onCloseClick(CloseClickEvent event) {
                stop();
            }
        });
        window.addItem(widget);
        window.addItem(createControls());
        window.show();
    }

    private Canvas createControls() {
        IButton continueBtn = new IButton(i18nPas.ProgressTracker_Continue_Title(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                stop();
            }
        });
        continueBtn.setTooltip(i18nPas.ProgressTracker_Continue_Hint());
        continueBtn.setAutoFit(true);

        HStack btnLayout = new HStack(5);
        btnLayout.setAutoHeight();
        btnLayout.setMargin(10);
        btnLayout.setLayoutAlign(Alignment.RIGHT);
        btnLayout.setMembers(continueBtn);
        return btnLayout;
    }

    public void setInit() {
        lastDone = lastTotal = 0;
        label.setContents(i18nPas.ProgressTracker_Initializing_Msg());
        progressbar.setPercentDone(0);
        if (datasource != null) {
            progressHandler = new ProgressHandler(datasource, criteria);
            ResultSet resultSet = progressHandler.getResultSet();
//            resultSet.getRange(0, 10);
            ClientUtils.getRangeWorkAround(resultSet, 0, 10);
        }
    }

    public void setProgress(int done, int total) {
        done = Math.max(0, done);
        total = Math.max(0, total);
        done = Math.min(done, total);
        lastDone = done;
        lastTotal = total;
        int progress = total == 0 ? 0 : done * 100 / total;
        String msg = i18nPas.ProgressTracker_Progress_Msg(
                progressPrefix, String.valueOf(done), String.valueOf(total));
        label.setContents(msg);
        progressbar.setPercentDone(progress);
    }

    public void setProgressPrefix(String prefix) {
        this.progressPrefix = prefix;
    }

    public void setDone(String msg) {
        setProgress(lastTotal, lastTotal);
        if (msg != null) {
            label.setContents(msg);
        }
    }

    public void stop() {
        if (progressHandler != null) {
            progressHandler.done();
            progressHandler = null;
        }
        if (window != null) {
            window.hide();
            window.destroy();
            window = null;
        }
        if (exitCallback != null) {
            exitCallback.run();
        }
    }

    public int getLastDone() {
        return lastDone;
    }

    public int getLastTotal() {
        return lastTotal;
    }

    public void setDataSource(DataSource ds, Criteria criteria) {
        this.datasource = ds;
        this.criteria = criteria;
    }

    private final class ProgressHandler implements Runnable, DataArrivedHandler, HandleErrorHandler {

        private final ArrayList<HandlerRegistration> registrations = new ArrayList<HandlerRegistration>();
        private final ResultSet resultSet;
        private ProgressTracker tracker = ProgressTracker.this;
        private boolean done;

        public ProgressHandler(DataSource ds, Criteria criteria) {
            resultSet = new ResultSet(ds);
            resultSet.setCriteria(criteria);
            resultSet.setFetchDelay(2000);
            registrations.add(ds.addHandleErrorHandler(this));
            registrations.add(resultSet.addDataArrivedHandler(this));
        }

        public ResultSet getResultSet() {
            return resultSet;
        }

        @Override
        public void run() {
            done();
        }

        public void done() {
            done = true;
            for (HandlerRegistration r : registrations) {
                r.removeHandler();
            }
            registrations.clear();
        }

        @Override
        public void onDataArrived(DataArrivedEvent event) {
            if (done) {
                return ;
            }
            int startRow = event.getStartRow();
            final int endRow = event.getEndRow();
            final int length = resultSet.getLength();
            Boolean lengthIsKnown = resultSet.lengthIsKnown();
            ClientUtils.log(LOG, Level.FINE, "onDataArrived: [%s,%s,%s], lengthIsKnown: %s", startRow, endRow, length, lengthIsKnown);
            tracker.setProgress(endRow, length);
            if (lengthIsKnown && endRow == resultSet.getLength()) {
                // done
                ProgressTracker.this.stop();
                ClientUtils.log(LOG, Level.FINE, "onDataArrived.done");
                done();
            } else {
                ClientUtils.log(LOG, Level.FINE, "onDataArrived.next: [%s,%s]", endRow, length);
//                resultSet.getRange(endRow, length);
                ClientUtils.getRangeWorkAround(resultSet, endRow, length);
            }
        }

        @Override
        public void onHandleError(ErrorEvent event) {
            ClientUtils.log(LOG, Level.FINE, "onHandleError");
            if (done) {
                return ;
            }
            event.cancel();
            Map errors = event.getResponse().getErrors();
            Iterator iterator = errors.values().iterator();
            StringBuilder sb = new StringBuilder();
            if (iterator.hasNext()) {
                sb.append(iterator.next());
            } else {
                sb.append(i18nPas.ProgressTracker_Failure_UnknownReason_Title());
            }
            String msg = i18nPas.ProgressTracker_Failure_Msg(sb.toString());
            tracker.setDone(msg);
            done();
        }

    }

}
