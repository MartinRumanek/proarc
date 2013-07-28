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
package cz.incad.pas.editor.client.widget.mods;

import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import cz.incad.pas.editor.client.ClientMessages;
import cz.incad.pas.editor.client.ds.ModsCustomDataSource;
import cz.incad.pas.editor.client.ds.mods.IdentifierDataSource;
import cz.incad.pas.editor.client.widget.StringTrimValidator;
import java.util.Arrays;

/**
 * Simple form to edit MODS of periodical issue object.
 *
 * @author Jan Pokorsky
 */
public final class PeriodicalIssueForm extends AbstractModelForm {

    public PeriodicalIssueForm(ClientMessages i18n) {
        setWidth100();
        setHeight100();
        setTitleOrientation(TitleOrientation.TOP);
        setNumCols(1);

        ComboBoxItem issueSequenceNumber = new ComboBoxItem(ModsCustomDataSource.FIELD_PER_ISSUE_NUMBER_SORTING,
                i18n.PeriodicalIssueForm_NumberSorting_Title());
        issueSequenceNumber.setRequired(true);
        issueSequenceNumber.setValidators(new StringTrimValidator(), new RegExpValidator(
                "^([1-9]\\d{0,4}(-[1-9]\\d{0,4})?|mimořádné|zvláštní|na ukázku)$"));
        issueSequenceNumber.setValueMap("mimořádné", "na ukázku", "zvláštní");
        issueSequenceNumber.setHideEmptyPickList(true);

        TextItem date = new TextItem(ModsCustomDataSource.FIELD_PER_ISSUE_DATE, i18n.PeriodicalIssueForm_Date_Title());
        date.setRequired(true);
        // issue 43, see https://docs.google.com/document/d/1zSriHPdnUY5d_tKv0M8a6nEym560DKh2H6XZ24tGAEw/edit?pli=1
        // YYYY|YYYY-YYYY|MM.YYYY|MM.-MM.YYYY|DD.MM.YYYY|DD.-DD.MM.YYYY
        // javascript regex ^([1-9]\d{3}(-[1-9]\d{3})?|(1\d|[1-9])\.(-(1\d|[1-9])\.)?[1-9]\d{3}|([123]\d|[1-9])\.(1\d|[1-9])\.(-([123]\d|[1-9])\.(1\d|[1-9])\.)?[1-9]\d{3})$
        date.setValidators(new StringTrimValidator(), new RegExpValidator(
                "^([1-9]\\d{3}(-[1-9]\\d{3})?|(1\\d|[1-9])\\.(-(1\\d|[1-9])\\.)?[1-9]\\d{3}|([123]\\d|[1-9])\\.(1\\d|[1-9])\\.(-([123]\\d|[1-9])\\.(1\\d|[1-9])\\.)?[1-9]\\d{3})$"));

        // identifiers
        final RepeatableFormItem identifiers = new RepeatableFormItem(ModsCustomDataSource.FIELD_IDENTIFIERS,
                i18n.PeriodicalIssueForm_Identifiers_Title());
        identifiers.setDataSource(IdentifierDataSource.getInstance());
        identifiers.setValidators(
                new IdentifiersValidator(i18n, Arrays.asList(IdentifierDataSource.TYPE_UUID)));
        DynamicForm identifierForm = new DynamicForm();
        identifierForm.setUseAllDataSourceFields(true);
        identifierForm.setNumCols(4);
        identifiers.setFormPrototype(identifierForm);
        identifiers.setEndRow(true);
        identifiers.setColSpan("2");

        TextAreaItem note = new TextAreaItem(ModsCustomDataSource.FIELD_NOTE, i18n.PeriodicalIssueForm_Note_Title());
        note.setWidth("*");
        note.setHeight("*");
        note.setColSpan("*");

        setFields(issueSequenceNumber, date, identifiers, note);
    }

}
