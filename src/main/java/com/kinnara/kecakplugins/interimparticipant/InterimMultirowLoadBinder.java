package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.form.model.*;

import javax.annotation.Nonnull;

/**
 * @author aristo
 *
 * Load interim master data records based on original participant
 */
public class InterimMultirowLoadBinder extends FormBinder
        implements FormLoadBinder,
        FormLoadMultiRowElementBinder {

    @Override
    @Nonnull
    public FormRowSet load(Element element, String currentParticipantUsername, FormData formData) {
        return Utilities.getInterimParticipantRows(currentParticipantUsername);
    }

    @Override
    public String getName() {
        return "Interim Multi Row Load Binder";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    private FormRow putInterimParticipant(String username) {
        FormRow row = new FormRow();
        row.put(Utilities.FIELD_INTERIM_PARTICIPANT, username);
        return row;
    }
}
