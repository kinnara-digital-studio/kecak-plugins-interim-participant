package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.app.model.DefaultSchedulerPlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class InterimSchedulerParticipant extends DefaultSchedulerPlugin {
    private final static DateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat dateFormat = new SimpleDateFormat("HH:mm");

    @Override
    public boolean filter(@Nonnull Map<String, Object> map) {
        return "00:00".equals(dateFormat.format(new Date()));
    }

    @Override
    public void jobRun(@Nonnull Map<String, Object> map) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");
        PluginManager pluginManager = (PluginManager) map.get("pluginManager");
        WorkflowActivity workflowActivity = (WorkflowActivity) map.get("workflowActivity");

        // generate master data form
        Form formParticipantMaster = Utilities.generateParticipantMasterForm();

        // cari orang2 yang hari ini cuti
        Date now = new Date();
        FormRowSet formRows = formDataDao.find(formParticipantMaster, "WHERE e.customProperties.active = 'true' AND ? BETWEEN e.customProperties.date_from AND e.customProperties.date_to", new String[] {sDateFormat.format(now)}, null, null, null, null);
        if(formRows == null || formRows.isEmpty()) {
            // stop scheduler, jangan proses lebih lanjut
            return;
        }

        // cari berdasarkan list orang, dapatkan semua assignment yang masih aktif
        WorkflowUserManager workflowUserManager = (WorkflowUserManager) applicationContext.getBean("workflowUserManager");
        WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");
        for(FormRow row : formRows) {
            // get current user
            String username = row.getProperty(Utilities.FIELD_ORIGINAL_PARTICIPANT);
            if(username == null) {
                continue;
            }

            // set thread user
            workflowUserManager.setCurrentThreadUser(username);

            // ambil assignments
            Collection<WorkflowAssignment> assignments = workflowManager.getAssignmentListLite(null, null, null, null, null, null, null, null);
            if(assignments == null) {
                continue;
            }

            // dapatkan interim user
            String interimUsername = row.getProperty(Utilities.FIELD_INTERIM_PARTICIPANT);
            if(interimUsername == null) {
                continue;
            }

            // reassign semua assignment
            for(WorkflowAssignment assignment : assignments)
                workflowManager.assignmentReassign(null, null, assignment.getActivityId(), username, interimUsername);
        }

        // simpan semua assignment ke history

        // reassign semua assignment ke orang baru (pengganti)

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
