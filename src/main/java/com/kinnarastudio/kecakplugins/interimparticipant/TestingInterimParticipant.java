package com.kinnarastudio.kecakplugins.interimparticipant;

import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.DefaultParticipantPlugin;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;

public class TestingInterimParticipant extends DefaultParticipantPlugin {
    @Override
    public String getName() {
        return getLabel();
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
    public Collection<String> getActivityAssignments(Map props) {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        WorkflowManager wfManager = (WorkflowManager)appContext.getBean("workflowManager");
        WorkflowActivity activity = (WorkflowActivity)props.get("workflowActivity");
        String workflowVariable = getPropertyString("workflowVariable");
        String variableValue = getPropertyString("variableValue");
        wfManager.activityVariable(activity.getId(), workflowVariable, variableValue);
        return Collections.singleton("admin");
    }

    @Override
    public String getLabel() {
        return "Testing Interim Participant";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/TestingInterimParticipant.json", null, false, null);
    }
}
