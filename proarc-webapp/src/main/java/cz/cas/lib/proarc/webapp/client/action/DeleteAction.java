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
package cz.cas.lib.proarc.webapp.client.action;

import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ds.RestConfig;
import cz.cas.lib.proarc.webapp.client.widget.Dialog;
import cz.cas.lib.proarc.webapp.client.widget.StatusView;
import java.util.logging.Logger;

/**
 * The delete action invokes {@link Deletable} with selection received from
 * the source object using {@link Selectable}.
 *
 * @author Jan Pokorsky
 */
public final class DeleteAction extends AbstractAction {

    private static final Logger LOG = Logger.getLogger(DeleteAction.class.getName());

    private final Deletable deletable;
    private final ClientMessages i18n;
    private final DynamicForm optionsForm;

    public DeleteAction(Deletable deletable, ClientMessages i18n) {
        this(deletable, null, i18n);
    }

    public DeleteAction(Deletable deletable, DynamicForm options, ClientMessages i18n) {
        super(i18n.DeleteAction_Title(), "[SKIN]/actions/remove.png", i18n.DeleteAction_Hint());
        this.deletable = deletable;
        this.optionsForm = options;
        this.i18n = i18n;
    }

    @Override
    public boolean accept(ActionEvent event) {
        Object[] selection = Actions.getSelection(event);
        return selection != null && selection.length > 0;
    }

    @Override
    public void performAction(ActionEvent event) {
        Object[] selection = Actions.getSelection(event);
        if (selection != null && selection.length > 0) {
            askAndDelete(selection);
        }
    }

    public void askAndDelete(final Object[] selection) {
        if (selection == null || selection.length == 0) {
            return ;
        }
        if (optionsForm != null) {
            optionsForm.clearValues();
            final Dialog d = new Dialog(i18n.DeleteAction_Window_Title());
            d.getDialogLabelContainer().setContents(i18n.DeleteAction_Window_Msg(String.valueOf(selection.length)));
            d.getDialogContentContainer().setMembers(optionsForm);
            d.addYesButton((ClickEvent event) -> {
                Record options = optionsForm.getValuesAsRecord();
                d.destroy();
                delete(selection, options);
            });
            d.addNoButton(new Dialog.DialogCloseHandler() {
                @Override
                public void onClose() {
                    d.destroy();
                }
            });
            d.setWidth(400);
            d.show();
        } else {
            SC.ask(i18n.DeleteAction_Window_Title(),
                    i18n.DeleteAction_Window_Msg(String.valueOf(selection.length)),
                    new BooleanCallback() {

                @Override
                public void execute(Boolean value) {
                    if (value != null && value) {
                        delete(selection);
                    }
                }
            });
        }
    }

    private void initOptionForms() {

    }

    public void delete(Object[] selection) {
        delete(selection, null);
    }

    public void delete(Object[] selection, Record options) {
        if (selection != null && selection.length > 0) {
            deletable.delete(selection, options);
        }
    }

    /**
     * Implement to provide deletion of items.
     */
    public interface Deletable<T> {

        void delete(Object[] items);

        /**
         * Deletes items using options.
         * @param items items to deletete
         * @param options options to customize the delete
         */
        default void delete(Object[] items, T options) {
            delete(items);
        }

    }

    /**
     * Helper to delete records of {@link DataSource}.
     */
    public static final class RecordDeletable implements Deletable {

        private final DataSource ds;
        private final ClientMessages i18n;

        public RecordDeletable(DataSource ds, ClientMessages i18n) {
            if (ds == null) {
                throw new NullPointerException();
            }
            this.ds = ds;
            this.i18n = i18n;
        }

        @Override
        public void delete(Object[] items) {
            DeleteTask task = new DeleteTask(items, ds, i18n);
            task.delete();
        }

        private static class DeleteTask {

            private final Object[] items;
            private int itemIndex = 0;
            private final DataSource ds;
            private final ClientMessages i18n;

            public DeleteTask(Object[] items, DataSource ds, ClientMessages i18n) {
                this.items = items;
                this.ds = ds;
                this.i18n = i18n;
            }

            public void delete() {
                deleteItem();
            }

            private void deleteItem() {
                Record item = (Record) items[itemIndex];
                // TileGrid.removeSelectedData uses queuing support in case of multi-selection.
                // It will require extra support on server. For now remove data in separate requests.
                //thumbGrid.removeSelectedData();
                ds.removeData(item, new DSCallback() {

                    @Override
                    public void execute(DSResponse response, Object rawData, DSRequest request) {
                        if (RestConfig.isStatusOk(response)) {
                            itemIndex++;
                            if (itemIndex < items.length) {
                                deleteItem();
                            } else {
                                StatusView.getInstance().show(i18n.DeleteAction_Done_Msg());
                            }
                        }
                    }
                });
            }
        }

    }
}
