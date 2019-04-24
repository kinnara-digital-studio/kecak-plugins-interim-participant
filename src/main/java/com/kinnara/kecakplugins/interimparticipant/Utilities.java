package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.service.FormService;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.model.PropertyEditable;
import org.joget.workflow.model.ParticipantPlugin;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utilities {
    public final static String APPLICATION_ID = "InterimParticipant";
    public final static String FORM_PARTICIPANT_MASTER = "participant_master";

    public final static String FIELD_ORIGINAL_PARTICIPANT = "employee";
    public final static String FIELD_INTERIM_PARTICIPANT = "interim_employee";

    private final static Map<String, Form> formCache = new WeakHashMap<>();

    public static Form generateParticipantMasterForm() {
        return generateForm(APPLICATION_ID, WorkflowManager.LATEST, FORM_PARTICIPANT_MASTER);
    }

    public static Form generateForm(String appId, String appVersion, String formDefId) {
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");
        return generateForm(
                appDefinitionDao.loadVersion(appId, appVersion.equals(WorkflowManager.LATEST)
                        ? appDefinitionDao.getPublishedVersion(appId) : Long.valueOf(appVersion)), formDefId);
    }

    public static Form generateForm(@Nullable AppDefinition appDef, String formDefId) {
        if(appDef == null)
            return null;

        // check in cache
        if(formCache.containsKey(formDefId))
            return formCache.get(formDefId);

        // proceed without cache
        ApplicationContext appContext = AppUtil.getApplicationContext();
        FormService formService = (FormService) appContext.getBean("formService");


        if (formDefId != null && !formDefId.isEmpty()) {
            FormDefinitionDao formDefinitionDao =
                    (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");

            FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
            if (formDef != null) {
                String json = formDef.getJson();
                Form form = (Form)formService.createElementFromJson(json);

                // put in cache if possible
                formCache.put(formDefId, form);

                return form;
            }
        }
        return null;
    }


    public static ParticipantPlugin getParticipantPlugin(Map<String, Object> elementSelect, PluginManager pluginManager, WorkflowActivity workflowActivity) {
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("pluginManager", pluginManager);
        additionalProperties.put("workflowActivity", workflowActivity);
        return getPluginObject(elementSelect, pluginManager, additionalProperties);
    }

    /**
     * Generate plugins
     * @param elementSelect
     * @param <T>
     * @return
     */
    public static <T extends PropertyEditable> T getPluginObject(Map<String, Object> elementSelect, PluginManager pluginManager, Map additionalProperties) {
        if(elementSelect == null)
            return null;

        String className = (String)elementSelect.get("className");
        Map<String, Object> properties = (Map<String, Object>)elementSelect.get("properties");

        LogUtil.info(Utilities.class.getName(), "properties ["+properties.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue()).collect(Collectors.joining(";"))+"]");

        T  plugin = (T) pluginManager.getPlugin(className);
        if(plugin == null) {
            LogUtil.warn(Utilities.class.getName(), "Error generating plugin [" + className + "]");
            return null;
        }

        properties.forEach(plugin::setProperty);
        plugin.getProperties().putAll(additionalProperties);

        LogUtil.info(Utilities.class.getName(), Optional.ofNullable(plugin.getProperties()).map(Map::entrySet).orElse(new HashSet<>()).stream().map(e -> e.getKey() + "->" + e.getValue()).collect(Collectors.joining("||")));

        return plugin;
    }
}
