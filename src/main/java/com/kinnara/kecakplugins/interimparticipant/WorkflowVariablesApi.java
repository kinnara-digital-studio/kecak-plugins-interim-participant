package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.app.model.AbstractVersionedObject;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorkflowVariablesApi extends DefaultApplicationPlugin implements PluginWebSupport {
    @Override
    public String getName() {
        return "Workflow Variables API";
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
    public Object execute(Map map) {
        return null;
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
        return null;
    }

    @Override
    public void webService(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        try {
            boolean isAdmin = WorkflowUtil.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN);
            if (!isAdmin) {
                throw new RestApiException(HttpServletResponse.SC_UNAUTHORIZED, "Current role is not admin");
            }

            List<Map<String, String>> variableList = getVariableList();

            // check for result
            if (variableList.isEmpty()) {
                throw new RestApiException(HttpServletResponse.SC_NO_CONTENT, "Empty data");
            }

            // send back response
            httpServletResponse.getWriter().write(new JSONArray(variableList).toString());
        } catch (RestApiException e) {
            LogUtil.warn(getClassName(), e.getMessage());
            httpServletResponse.setStatus(e.getResponseCode());
        }
    }

    @Nonnull
    private List<Map<String, String>> getVariableList() {
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");

        return Optional.ofNullable(appDefinition)
                .map(AppDefinition::getPackageDefinition)
                .map(AbstractVersionedObject::getId)
                .map(workflowManager::getProcessList)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .flatMap(p -> workflowManager.getProcessVariableDefinitionList(p.getId()).stream())
                .map(WorkflowVariable::getId)
                .distinct()
                .sorted()
                .map(var -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("value", var);
                    map.put("label", var);
                    return map;
                })
                .collect(Collectors.toList());
    }
}
