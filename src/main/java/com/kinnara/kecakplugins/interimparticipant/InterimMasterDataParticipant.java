package com.kinnara.kecakplugins.interimparticipant;

import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.DefaultParticipantPlugin;
import org.joget.workflow.model.ParticipantPlugin;
import org.joget.workflow.model.WorkflowActivity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author aristo
 * <p>
 * Participant Plugins to implement Interim Participant
 */
public class InterimMasterDataParticipant extends DefaultParticipantPlugin {
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
        PluginManager pluginManager = (PluginManager) map.get("pluginManager");
        WorkflowActivity workflowActivity = (WorkflowActivity) map.get("workflowActivity");

        // generate participant mapping plugin
        ParticipantPlugin participantPlugin = Utilities.getParticipantPlugin((Map<String, Object>) map.get("participantPlugin"), pluginManager, workflowActivity);

        return Optional.ofNullable(participantPlugin)
                .map(p -> p.getActivityAssignments(p.getProperties()))
                .orElse(Collections.emptyList())
                .stream()

                // map actual user to interim user
                .map(Utilities::getInterimParticipantUsername)

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
        String[] args = new String[]{WorkflowVariablesApi.class.getName()};
        return AppUtil.readPluginResource(getClassName(), "/properties/InterimMasterDataParticipant.json", args, false, "/messages/InterimParticipant");
    }
}
