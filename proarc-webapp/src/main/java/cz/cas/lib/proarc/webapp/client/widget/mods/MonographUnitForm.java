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
package cz.cas.lib.proarc.webapp.client.widget.mods;

import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.IsIntegerValidator;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ds.ModsCustomDataSource;
import cz.cas.lib.proarc.webapp.client.ds.mods.IdentifierDataSource;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Simple form to edit MODS of monograph unit object.
 *
 * @author Jan Pokorsky
 */
public final class MonographUnitForm extends AbstractModelForm {

    private static final Logger LOG = Logger.getLogger(MonographUnitForm.class.getName());

    public MonographUnitForm(final ClientMessages i18n) {
        setWidth100();
        setHeight100();
        setTitleOrientation(TitleOrientation.TOP);
        setNumCols(1);

        // identifiers
        final RepeatableFormItem identifiers = new RepeatableFormItem(ModsCustomDataSource.FIELD_IDENTIFIERS,
                i18n.MonographUnitForm_Identifiers_Title());
        identifiers.setDataSource(IdentifierDataSource.getInstance());
        identifiers.setValidators(
                new IdentifiersValidator(i18n, Arrays.asList(IdentifierDataSource.TYPE_UUID)));
        DynamicForm identifierForm = new DynamicForm();
        identifierForm.setUseAllDataSourceFields(true);
        identifierForm.setNumCols(4);
        identifiers.setFormPrototype(identifierForm);
        identifiers.setEndRow(true);
        identifiers.setColSpan("2");

        TextItem unitNumber = new TextItem(ModsCustomDataSource.FIELD_MONOGRAPHUNIT_NUMBER);
        unitNumber.setTitle(i18n.MonographUnitForm_UnitNumber_Title());
        unitNumber.setRequired(true);
        unitNumber.setValidators(new IsIntegerValidator());

        TextAreaItem note = new TextAreaItem(ModsCustomDataSource.FIELD_NOTE, i18n.MonographUnitForm_Note_Title());
        note.setWidth("*");
        note.setHeight("*");
        note.setColSpan("*");

        setFields(unitNumber, identifiers, note);
    }

}
