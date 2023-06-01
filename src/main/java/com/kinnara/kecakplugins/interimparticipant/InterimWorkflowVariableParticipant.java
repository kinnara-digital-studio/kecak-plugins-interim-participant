package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryManager;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.DefaultParticipantPlugin;
import org.joget.workflow.model.ParticipantPlugin;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InterimWorkflowVariableParticipant extends DefaultParticipantPlugin {
    @Override
    public String getName() {
        return "Interim Workflow Variable Participant";
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
    public Collection<String> getActivityAssignments(Map map) {
        String variableId = getPropertyString("variableId");

        LogUtil.info(getClassName(), "Looking for interim employee using variable ["+variableId+"]");

        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        WorkflowManager workflowManager = (WorkflowManager) applicationContext.getBean("workflowManager");
        DirectoryManager directoryManager = (DirectoryManager) applicationContext.getBean("directoryManager");
        PluginManager pluginManager = (PluginManager) map.get("pluginManager");
        WorkflowActivity workflowActivity = (WorkflowActivity) map.get("workflowActivity");

        List<String> interimParticipant = Optional
                // get variable value
                .ofNullable(workflowManager.getProcessVariable(workflowActivity.getProcessId(), variableId))

                // handle multiple interim users
                .map(s -> s.split(";"))
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .filter(s -> !s.isEmpty())

                // get user from directory manager to check if the username exists
                .map(directoryManager::getUserByUsername)
                .filter(Objects::nonNull)

                // get the username
                .map(User::getUsername)
                .collect(Collectors.toList());

        if(interimParticipant.isEmpty()) {
            // no interim user(s) return actual participant
            ParticipantPlugin participantPlugin = Utilities.getParticipantPlugin((Map<String, Object>) getProperty("participantPlugin"), pluginManager, workflowActivity);
            return participantPlugin.getActivityAssignments(participantPlugin.getProperties());
        } else {
            // assign task to interim user(s)
            return interimParticipant;
        }
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
        return AppUtil.readPluginResource(getClassName(), "/properties/WorkflowVariableInterimParticipant.json", args, false, "/messages/InterimParticipant");
    }
}
