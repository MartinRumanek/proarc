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
package cz.incad.pas.editor.client.action;

import com.smartgwt.client.data.Record;
import cz.incad.pas.editor.client.ClientMessages;
import cz.incad.pas.editor.client.Editor;
import cz.incad.pas.editor.client.ds.MetaModelDataSource;
import cz.incad.pas.editor.client.ds.MetaModelDataSource.MetaModelRecord;
import cz.incad.pas.editor.client.presenter.DigitalObjectEditing.DigitalObjectEditorPlace;
import cz.incad.pas.editor.shared.rest.DigitalObjectResourceApi.DatastreamEditorType;

/**
 * Opens data stream editor.
 *
 * It expects the event source to implement {@link Selectable}.
 *
 * @author Jan Pokorsky
 */
public final class DigitalObjectEditAction extends AbstractAction {

    private DatastreamEditorType editorType;

    public DigitalObjectEditAction(String title, DatastreamEditorType editorType, ClientMessages i18n) {
        this(title, i18n.DigitalObjectEditAction_Hint(), editorType);
    }
    
    public DigitalObjectEditAction(String title, String tooltip, DatastreamEditorType editorType) {
        super(title, "[SKIN]/actions/edit.png", tooltip);
        this.editorType = editorType;
    }

    @Override
    public void performAction(ActionEvent event) {
        Record[] selection = Actions.getSelection(event);
        if (selection == null || selection.length != 1) {
            return ;
        }

        DigitalObjectEditorPlace place = new DigitalObjectEditorPlace(editorType, selection[0]);
        Editor.getInstance().getEditorWorkFlow().getPlaceController().goTo(place);
    }

    @Override
    public boolean accept(ActionEvent event) {
        Record[] selection = Actions.getSelection(event);
        if (selection == null || selection.length != 1) {
            return false;
        }
        // check object model
        MetaModelRecord model = (MetaModelRecord) selection[0].getAttributeAsObject(
                MetaModelDataSource.FIELD_MODELOBJECT);
        if (model != null) {
            return model.isSupportedDatastream(editorType.name());
        }
        return false;
    }

}
