package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.app.model.DefaultSchedulerPlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
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

import static com.kinnara.kecakplugins.interimparticipant.Utilities.*;

public class InterimSchedulerParticipant extends DefaultSchedulerPlugin {
    private final static DateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    DateFormat dateFormat = new SimpleDateFormat("HH:mm");
    Date now = new Date();
    ApplicationContext applicationContext = AppUtil.getApplicationContext();
    FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");
    // generate master data form
    Form formParticipantMaster = Utilities.generateParticipantMasterForm();
    // generate history data form
//        Form formAssignmentHistory = Utilities.generateAssignmentHistoryForm();
    WorkflowUserManager workflowUserManager = (WorkflowUserManager) applicationContext.getBean("workflowUserManager");
    WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");

    @Override
    public boolean filter(@Nonnull Map<String, Object> map) {
//        return "00:00".equals(dateFormat.format(new Date()));
        return true;
    }

    @Override
    public void jobRun(@Nonnull Map<String, Object> map) {
        reassignToInterim();
        returnToUser();
    }
    public void reassignToInterim(){
        // cari orang2 yang hari ini cuti
        FormRowSet formRowsValidDate = formDataDao.find(formParticipantMaster, "WHERE e.customProperties.active = 'true' AND ? BETWEEN e.customProperties.date_from AND e.customProperties.date_to", new String[]{sDateFormat.format(now)}, null, null, null, null);
        if (formRowsValidDate == null || formRowsValidDate.isEmpty()) {
            // stop scheduler, jangan proses lebih lanjut
            return  ;
        }
        // cari berdasarkan list orang, dapatkan semua assignment yang masih aktif
        for (FormRow rowParticipant : formRowsValidDate) {
            // get current user
            String username = rowParticipant.getProperty(Utilities.FIELD_ORIGINAL_PARTICIPANT);
            if (username == null) {
                continue;
            }

            // set thread user
            workflowUserManager.setCurrentThreadUser(username);

            // ambil assignments
            Collection<WorkflowAssignment> assignments = workflowManager.getAssignmentListLite(null, null, null, null, null, null, null, null);
            if (assignments == null) {
                continue;
            }

            // dapatkan interim user
            String interimUsername = rowParticipant.getProperty(Utilities.FIELD_INTERIM_PARTICIPANT);
            if (interimUsername == null) {
                continue;
            }

            // untuk semua assignment
            for (WorkflowAssignment assignment : assignments) {

                // simpan semua assignment ke history
//                FormRowSet rowsetHistory = new FormRowSet();
//                FormRow rowHistory = new FormRow();
//                rowHistory.put(FIELD_PROCESS_ID,assignment.getProcessId());
//                rowHistory.put(FIELD_ACTIVITY_ID,assignment.getActivityId());
//                rowHistory.put(FIELD_ORIGIN,username);
//                rowHistory.put(FIELD_REASSIGN_TO,interimUsername);
//                rowsetHistory.add(rowHistory);
//                formAssignmentHistory.getStoreBinder().store(formAssignmentHistory, rowsetHistory , new FormData());


                // reassign semua assignment
//                LogUtil.info(InterimSchedulerParticipant.class.getName(), "------------" + assignment.getActivityId() + "--" + username + "---" + interimUsername);
//                workflowManager.assignmentReassign(assignment.getProcessDefId(), assignment.getProcessId(), assignment.getActivityId(), interimUsername, username);
                // reevaluate assignment
                workflowManager.reevaluateAssignmentsForActivity(assignment.getActivityId());
            }
        }
    }

    public void returnToUser(){
        // cari date yang tidak valid
        FormRowSet formRowsNotValid = formDataDao.find(formParticipantMaster, "WHERE e.customProperties.active = ? AND e.customProperties.date_to < ?", new String[]{"true", sDateFormat.format(now)}, null, null, null, null);
        if (formRowsNotValid == null || formRowsNotValid.isEmpty()) {
            // stop scheduler, jangan proses lebih lanjut
            return;
        }

        for (FormRow rowActive : formRowsNotValid) {
            rowActive.setProperty(CHECKBOX_ACTIVE, "false");
            String interimUser = rowActive.getProperty(FIELD_INTERIM_PARTICIPANT);
            String[] interimSplit =  interimUser.split(";");
                for (String username : interimSplit) {
                workflowManager.reevaluateAssignmentsForUser(username);
                }
        }
        // save rowActive
        formDataDao.saveOrUpdate(formParticipantMaster, formRowsNotValid);

        //reevaluate semua activity yang ada di interim master data
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
        return "";
    }
}