package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.plugin.base.PluginManager;

import javax.annotation.Nonnull;
import java.util.ResourceBundle;

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
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
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
