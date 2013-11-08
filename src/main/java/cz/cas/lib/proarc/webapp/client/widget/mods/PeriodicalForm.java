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

import com.smartgwt.client.types.CharacterCasing;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.RegExpValidator;
import cz.cas.lib.proarc.webapp.client.ClientMessages;
import cz.cas.lib.proarc.webapp.client.ds.LanguagesDataSource;
import cz.cas.lib.proarc.webapp.client.ds.ModsCustomDataSource;
import cz.cas.lib.proarc.webapp.client.ds.mods.IdentifierDataSource;
import cz.cas.lib.proarc.webapp.client.widget.StringTrimValidator;
import cz.cas.lib.proarc.webapp.client.widget.mods.RepeatableFormItem.CustomFormFactory;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Simple form to edit MODS of periodical object.
 *
 * @author Jan Pokorsky
 */
public final class PeriodicalForm extends AbstractModelForm {

    private static final Logger LOG = Logger.getLogger(PeriodicalForm.class.getName());

    public PeriodicalForm(final ClientMessages i18n) {
        setWidth100();
        setHeight100();
        setTitleOrientation(TitleOrientation.TOP);
        setNumCols(2);

        // identifiers
        final RepeatableFormItem identifiers = new RepeatableFormItem(ModsCustomDataSource.FIELD_IDENTIFIERS,
                i18n.PeriodicalForm_Identifiers_Title());
        identifiers.setDataSource(IdentifierDataSource.getInstance());
        identifiers.setValidators(
                new IdentifiersValidator(i18n, Arrays.asList(
                                IdentifierDataSource.TYPE_UUID,
                                IdentifierDataSource.TYPE_ISSN)));
        DynamicForm identifierForm = new DynamicForm();
        identifierForm.setUseAllDataSourceFields(true);
        identifierForm.setNumCols(4);
        identifiers.setFormPrototype(identifierForm);
        identifiers.setEndRow(true);
        identifiers.setColSpan("2");

        TextItem sigla = new TextItem(ModsCustomDataSource.FIELD_SIGLA, i18n.PeriodicalForm_Sigla_Title());
        sigla.setRequired(true);
        sigla.setValidators(new StringTrimValidator(), new RegExpValidator("^[A-Z]{3}[0-9]{3}$"));
        sigla.setCharacterCasing(CharacterCasing.UPPER);

        RepeatableFormItem shelfLocators = new RepeatableFormItem(
                ModsCustomDataSource.FIELD_SHELF_LOCATORS, i18n.PeriodicalForm_ShelfLocators_Title(),
                new StringFormFactory(ModsCustomDataSource.FIELD_STRING_VALUE, null, false)
                        .required(true)
                );
        shelfLocators.setRequired(true);
//        shelfLocators.setRowSpan(2);

        RepeatableFormItem periodicity = new RepeatableFormItem(
                ModsCustomDataSource.FIELD_PERIODICITIES, i18n.PeriodicalForm_Periodicities_Title(),
                new StringFormFactory(ModsCustomDataSource.FIELD_STRING_VALUE, null, false));
        
        RepeatableFormItem titles = new RepeatableFormItem(
                ModsCustomDataSource.FIELD_TITLES, i18n.PeriodicalForm_Titles_Title(),
                new StringFormFactory(ModsCustomDataSource.FIELD_STRING_VALUE, null, false, 600)
                        .required(true)
                );
        titles.setRequired(true);
        oneRow(titles);

        RepeatableFormItem subtitles = new RepeatableFormItem(
                ModsCustomDataSource.FIELD_SUBTITLES, i18n.PeriodicalForm_Subtitles_Title(),
                new StringFormFactory(ModsCustomDataSource.FIELD_STRING_VALUE, null, false, 600));
        oneRow(subtitles);

        RepeatableFormItem alternativeTitles = new RepeatableFormItem(
                ModsCustomDataSource.FIELD_ALTERNATIVE_TITLES, i18n.PeriodicalForm_AlternativeTitles_Title(),
                new StringFormFactory(ModsCustomDataSource.FIELD_STRING_VALUE, null, false, 600));
        oneRow(alternativeTitles);

        RepeatableFormItem keyTitles = new RepeatableFormItem(
                ModsCustomDataSource.FIELD_KEY_TITLES, i18n.PeriodicalForm_KeyTitles_Title(),
                new StringFormFactory(ModsCustomDataSource.FIELD_STRING_VALUE, null, false, 400));
        oneRow(keyTitles);

        // author
        RepeatableFormItem authors = new RepeatableFormItem(ModsCustomDataSource.FIELD_AUTHORS,
                i18n.PeriodicalForm_Authors_Title(), new PersonFormFactory(i18n));
        oneRow(authors);
        RepeatableFormItem contribs = new RepeatableFormItem(ModsCustomDataSource.FIELD_CONTRIBUTORS,
                i18n.PeriodicalForm_Contributors_Title(), new PersonFormFactory(i18n));
        oneRow(contribs);
        RepeatableFormItem printers = new RepeatableFormItem(ModsCustomDataSource.FIELD_PRINTERS,
                i18n.PeriodicalForm_Printers_Title(), new PrinterPublisherFormFactory(false, i18n));
        oneRow(printers);
        RepeatableFormItem publishers = new RepeatableFormItem(ModsCustomDataSource.FIELD_PUBLISHERS,
                i18n.PeriodicalForm_Publishers_Title(), new PrinterPublisherFormFactory(true, i18n));
        oneRow(publishers);

        RepeatableFormItem languages = new RepeatableFormItem(ModsCustomDataSource.FIELD_LANGUAGES,
                i18n.PeriodicalForm_Languages_Title(), new CustomFormFactory() {

            @Override
            public DynamicForm create() {
                DynamicForm form = new DynamicForm();
//                form.setNumCols(6);
                ComboBoxItem language = new ComboBoxItem(
                        ModsCustomDataSource.FIELD_LANGUAGE_CODE,
                        i18n.PeriodicalForm_LanguageCode_Title());
                language.setPrompt(i18n.PeriodicalForm_LanguageCode_Hint());
                language.setOptionDataSource(LanguagesDataSource.getInstance());
                language.setOptionCriteria(LanguagesDataSource.languageCriteria());
//                language.setPickListCriteria(LanguagesDataSource.activeLocaleAsCriteria());
                language.setValueField(LanguagesDataSource.FIELD_CODE);
                language.setDisplayField(LanguagesDataSource.FIELD_VALUE);
                form.setFields(language);
                return form;
            }
        });

        RepeatableFormItem subjects = new RepeatableFormItem(ModsCustomDataSource.FIELD_CLASSIFICATIONS,
                i18n.PeriodicalForm_Subjects_Title(), new CustomFormFactory() {

            @Override
            public DynamicForm create() {
                DynamicForm form = new DynamicForm();
                form.setNumCols(4);
                TextItem udc = new TextItem(ModsCustomDataSource.FIELD_CLASSIFICATION_UDC,
                        i18n.PeriodicalForm_SubjectsUdc_Title()); // MDT in czech
                udc.setValidators(new StringTrimValidator());
                TextItem ddc = new TextItem(ModsCustomDataSource.FIELD_CLASSIFICATION_DDC,
                        i18n.PeriodicalForm_SubjectsDdc_Title()); // DDT in czech
                ddc.setValidators(new StringTrimValidator());
                form.setFields(udc, ddc);
                return form;
            }
        });
        oneRow(subjects);

        RepeatableFormItem keywords = new RepeatableFormItem(ModsCustomDataSource.FIELD_KEYWORDS,
                i18n.PeriodicalForm_Keywords_Title(),
                new StringFormFactory(ModsCustomDataSource.FIELD_STRING_VALUE, null, false));

        RepeatableFormItem physicalDescriptions = new RepeatableFormItem(
                ModsCustomDataSource.FIELD_PHYSICAL_DESCRIPTIONS,
                i18n.PeriodicalForm_PhysicalDescriptions_Title(),
                new CustomFormFactory() {

            @Override
            public DynamicForm create() {
                DynamicForm form = new DynamicForm();
                form.setNumCols(4);
                TextItem extent = new TextItem(ModsCustomDataSource.FIELD_PHYSICAL_DESCRIPTIONS_EXTENT,
                        i18n.PeriodicalForm_PhysicalDescriptionsExtent_Title()); // rozsah
                extent.setValidators(new StringTrimValidator());
                TextItem size = new TextItem(ModsCustomDataSource.FIELD_PHYSICAL_DESCRIPTIONS_SIZE,
                        i18n.PeriodicalForm_PhysicalDescriptionsSize_Title()); // Rozmery
                size.setValidators(new StringTrimValidator());
                form.setFields(extent, size);
                return form;
            }
        });
        oneRow(physicalDescriptions);

        TextItem recordOrigin = new TextItem(ModsCustomDataSource.FIELD_RECORD_ORIGIN,
                i18n.PeriodicalForm_RecordOrigin_Title());
        recordOrigin.setWidth("*");
        recordOrigin.setValidators(new StringTrimValidator());
        oneRow(recordOrigin);

        TextAreaItem note = new TextAreaItem(ModsCustomDataSource.FIELD_NOTE,
                i18n.PeriodicalForm_Note_Title());
        note.setWidth("*");
        note.setHeight("*");
        note.setColSpan("*");
        oneRow(note);
        note.setMinHeight(50);

        setFields(identifiers, sigla, shelfLocators, periodicity, titles, subtitles,
                alternativeTitles, keyTitles, authors, contribs, printers, publishers,
                languages, keywords, subjects, physicalDescriptions, recordOrigin, note
                );
    }

}
