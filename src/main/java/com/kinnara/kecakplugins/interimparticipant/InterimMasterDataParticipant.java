package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.DefaultParticipantPlugin;
import org.joget.workflow.model.ParticipantPlugin;
import org.joget.workflow.model.WorkflowActivity;
import org.springframework.context.ApplicationContext;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class InterimMasterDataParticipant extends DefaultParticipantPlugin {
    private final static DateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private final static String FIELD_ORIGINAL_PARTICIPANT = "employee";
    private final static String FIELD_INTERIM_PARTICIPANT = "interim_employee";

    @Override
    public String getName() {
        return "Interim Master Data Participant";
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
    public Collection<String> getActivityAssignments(Map map) {
        Date now = new Date();
        LogUtil.info(getClassName(), "Looking for interim employee at date ["+sDateFormat.format(now)+"]");

        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");
        PluginManager pluginManager = (PluginManager) map.get("pluginManager");
        WorkflowActivity workflowActivity = (WorkflowActivity) map.get("workflowActivity");

        // generate master data form
        Form formParticipantMaster = Utilities.generateParticipantMasterForm();

        // generate participant mapping plugin
        ParticipantPlugin participantPlugin = Utilities.getParticipantPlugin((Map<String, Object>) map.get("participantPlugin"), pluginManager, workflowActivity);

        return Optional.ofNullable(participantPlugin)
                .map(p -> p.getActivityAssignments(p.getProperties()))
                .orElse(Collections.emptyList())
                .stream()

                .peek(s -> LogUtil.info(getClassName(), "original participant ["+s+"]"))
                // map actual user to interim user
                .map(originalParticipant -> {
                    List<String> interimParticipant = Optional
                            // get from master data
                            .ofNullable(formDataDao.find(formParticipantMaster, "WHERE e.customProperties."+FIELD_ORIGINAL_PARTICIPANT+" LIKE '%'||?||'%' AND e.customProperties.active = 'true' AND ? BETWEEN e.customProperties.date_from AND e.customProperties.date_to", new String[] {originalParticipant, sDateFormat.format(now)}, null, null, null, null))
                            .orElse(new FormRowSet())
                            .stream()

                            // check if current actual user is in the mapping data
                            .filter(formRow -> Arrays.asList(Optional.ofNullable(formRow.getProperty(FIELD_ORIGINAL_PARTICIPANT)).orElse("").split(";")).contains(originalParticipant))

                            // get the interim user(s)
                            .map(formRow -> formRow.getProperty(FIELD_INTERIM_PARTICIPANT))

                            .filter(Objects::nonNull)
                            .map(s -> s.split(";"))
                            .flatMap(Arrays::stream)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    if(interimParticipant.isEmpty()) {
                        // no interim user(s), return the current actual user
                        return Collections.singletonList(originalParticipant);
                    } else {
                        // interim user(s) found
                        LogUtil.info(getClassName(), "Switching user [" + originalParticipant + "] with [" + interimParticipant + "]");
                        return interimParticipant;
                    }
                })
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
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
        return AppUtil.readPluginResource(getClassName(), "/properties/InterimMasterDataParticipant.json", args, false, "/messages/InterimParticipant");
    }
}
