package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.app.model.DefaultSchedulerPlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowActivity;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class InterimSchedulerParticipant extends DefaultSchedulerPlugin {

    DateFormat dateFormat = new SimpleDateFormat("HH:mm");

    InterimMasterDataParticipant interimMasterDataParticipant = new InterimMasterDataParticipant();

    @Override
    public boolean filter(@Nonnull Map<String, Object> map) {
        return "00:00".equals(dateFormat.format(new Date()));
    }

    @Override
    public void jobRun(@Nonnull Map<String, Object> map) {
        // cari orang2 yang hari ini cuti

        // cari berdasarkan list orang semua assignment yang masih aktif

        // simpan semua assignment ke history

        // reassign semua assignment ke orang baru (pengganti)
        interimMasterDataParticipant.getActivityAssignments();
    }

    @Override
    public String getName() {
        return "Interim Scheduler Participant";
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
        String[] args = new String[] { WorkflowVariablesApi.class.getName() };
        return AppUtil.readPluginResource(getClassName(), "/properties/InterimSchedulerParticipant.json", args, false, "/messages/InterimSchedulerParticipant");
    }
}
