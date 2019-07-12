package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormService;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.model.PropertyEditable;
import org.joget.workflow.model.ParticipantPlugin;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Utilities {
    public final static String APPLICATION_ID = "InterimParticipant";
    public final static String FORM_PARTICIPANT_MASTER = "participant_master";
//    public final static String FORM_ASSIGNMENT_HISTORY = "assignment_history";

    public final static String FIELD_ORIGINAL_PARTICIPANT = "employee";
    public final static String FIELD_INTERIM_PARTICIPANT = "interim_employee";
    public final static String CHECKBOX_ACTIVE = "active";
//    public final static String FIELD_PROCESS_ID = "process_id";
//    public final static String FIELD_ACTIVITY_ID = "activity_id";
//    public final static String FIELD_ORIGIN = "origin";
//    public final static String FIELD_REASSIGN_TO = "reassign_to"; //konstan


    private final static DateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final static Map<String, Form> formCache = new WeakHashMap<>();

    /**
     * Generate Participant Master Form
     *
     * @return
     */
    public static Form generateParticipantMasterForm() {
        return generateForm(APPLICATION_ID, WorkflowManager.LATEST, FORM_PARTICIPANT_MASTER);
    }

    /**
     * Generate Form
     *
     * @param appId
     * @param appVersion
     * @param formDefId
     * @return
     */
    public static Form generateForm(String appId, String appVersion, String formDefId) {
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");
        return generateForm(
                appDefinitionDao.loadVersion(appId, appVersion.equals(WorkflowManager.LATEST)
                        ? appDefinitionDao.getPublishedVersion(appId) : Long.valueOf(appVersion)), formDefId);
    }

    /**
     * Generate Form
     *
     * @param appDef
     * @param formDefId
     * @return
     */
    public static Form generateForm(@Nullable AppDefinition appDef, String formDefId) {
        if (appDef == null)
            return null;

        // check in cache
        if (formCache.containsKey(formDefId))
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
                Form form = (Form) formService.createElementFromJson(json);

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
     *
     * @param elementSelect
     * @param <T>
     * @return
     */
    public static <T extends PropertyEditable> T getPluginObject(Map<String, Object> elementSelect, PluginManager pluginManager, Map additionalProperties) {
        if (elementSelect == null)
            return null;

        String className = (String) elementSelect.get("className");
        Map<String, Object> properties = (Map<String, Object>) elementSelect.get("properties");

        T plugin = (T) pluginManager.getPlugin(className);
        if (plugin == null) {
            LogUtil.warn(Utilities.class.getName(), "Error generating plugin [" + className + "]");
            return null;
        }

        properties.forEach(plugin::setProperty);
        plugin.getProperties().putAll(additionalProperties);

        return plugin;
    }

    @Nonnull
    static FormRowSet getInterimParticipantRows(String originalParticipant) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();
        FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");

        Form formParticipantMaster = Utilities.generateParticipantMasterForm();

        // if Interim Participant app is not installed
        if (formParticipantMaster == null) {
            LogUtil.warn(Utilities.class.getName(), "Interim Participant application is not installed");

            // return empty Row Set
            return new FormRowSet();
        }

        final Date now = new Date();

        // retrieve interim participant from master data
        return Optional
                // get from master data
                .ofNullable(formDataDao.find(formParticipantMaster, "WHERE e.customProperties." + Utilities.FIELD_ORIGINAL_PARTICIPANT + " LIKE '%'||?||'%' AND e.customProperties.active = 'true' AND ? BETWEEN e.customProperties.date_from AND e.customProperties.date_to", new String[]{originalParticipant, sDateFormat.format(now)}, null, null, null, null))
                .orElse(new FormRowSet())
                .stream()

                // check if current actual user is in the mapping data
                .filter(formRow -> Arrays.asList(Optional.ofNullable(formRow.getProperty(Utilities.FIELD_ORIGINAL_PARTICIPANT)).orElse("").split(";")).contains(originalParticipant))
                .collect(Collectors.toCollection(FormRowSet::new));
    }

    /**
     * Get Interim Participant
     *
     * @param originalParticipant original participant username
     * @return interim participant username
     */
    @Nonnull
    static List<String> getInterimParticipantUsername(String originalParticipant) {
        FormRowSet interimParticipantRows = getInterimParticipantRows(originalParticipant);

        // retrieve interim participant from master data
        List<String> interimParticipant = interimParticipantRows
                .stream()

                // get the interim user(s)
                .map(formRow -> formRow.getProperty(Utilities.FIELD_INTERIM_PARTICIPANT))

                .filter(Objects::nonNull)
                .map(s -> s.split(";"))
                .flatMap(Arrays::stream)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (interimParticipant.isEmpty()) {
            // no interim user(s), return the current actual user
            return Collections.singletonList(originalParticipant);
        } else {
            // interim user(s) found
            LogUtil.info(Utilities.class.getName(), "Switching user [" + originalParticipant + "] with [" + interimParticipant + "]");
            return interimParticipant;
        }
    }
}
