package com.kinnara.kecakplugins.interimparticipant;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(InterimSchedulerParticipant.class.getName(), new InterimSchedulerParticipant(), null));
        registrationList.add(context.registerService(InterimMasterDataParticipant.class.getName(), new InterimMasterDataParticipant(), null));
        registrationList.add(context.registerService(InterimWorkflowVariableParticipant.class.getName(), new InterimWorkflowVariableParticipant(), null));
        registrationList.add(context.registerService(WorkflowVariablesApi.class.getName(), new WorkflowVariablesApi(), null));
        registrationList.add(context.registerService(InterimMultirowLoadBinder.class.getName(), new InterimMultirowLoadBinder(), null));

    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}