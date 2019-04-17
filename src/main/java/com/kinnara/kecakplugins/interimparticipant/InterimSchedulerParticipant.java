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
    private final static DateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final static String FIELD_ORIGINAL_PARTICIPANT = "employee";
    private final static String FIELD_INTERIM_PARTICIPANT = "interim_employee";

    @Override
    public boolean filter(@Nonnull Map<String, Object> map) {
        return true;
    }

    @Override
    public void jobRun(@Nonnull Map<String, Object> map) {
        Date now = new Date();
        LogUtil.info(getClassName(), "Looking for interim employee at date ["+sDateFormat.format(now)+"]");

        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");
        PluginManager pluginManager = (PluginManager) map.get("pluginManager");
        WorkflowActivity workflowActivity = (WorkflowActivity) map.get("workflowActivity");

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
