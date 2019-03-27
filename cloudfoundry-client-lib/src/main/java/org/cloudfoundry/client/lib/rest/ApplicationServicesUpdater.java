package org.cloudfoundry.client.lib.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.ApplicationServicesUpdateCallback;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;

public class ApplicationServicesUpdater {
    private CloudControllerClient client;

    public ApplicationServicesUpdater(CloudControllerClient client) {
        this.client = client;
    }

    public List<String> updateApplicationServices(String applicationName,
        Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
        ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        CloudApplication application = client.getApplication(applicationName);

        List<String> addServices = calculateServicesToAdd(serviceNamesWithBindingParameters.keySet(), application);

        List<String> deleteServices = calculateServicesToDelete(serviceNamesWithBindingParameters.keySet(), application);

        List<String> rebindServices = calculateServicesToRebind(serviceNamesWithBindingParameters.keySet(),
            serviceNamesWithBindingParameters, application);

        for (String serviceName : addServices) {
            client.bindService(applicationName, serviceName, serviceNamesWithBindingParameters.get(serviceName),
                applicationServicesUpdateCallback);
        }
        for (String serviceName : deleteServices) {
            client.unbindService(applicationName, serviceName, applicationServicesUpdateCallback);
        }
        for (String serviceName : rebindServices) {
            client.unbindService(applicationName, serviceName, applicationServicesUpdateCallback);
            client.bindService(applicationName, serviceName, serviceNamesWithBindingParameters.get(serviceName),
                applicationServicesUpdateCallback);
        }

        return getUpdatedServiceNames(addServices, deleteServices, rebindServices);
    }

    private List<String> calculateServicesToAdd(Set<String> services, CloudApplication application) {
        return services.stream()
            .filter(serviceName -> !application.getServices()
                .contains(serviceName))
            .collect(Collectors.toList());
    }

    private List<String> calculateServicesToDelete(Set<String> services, CloudApplication application) {
        return application.getServices()
            .stream()
            .filter(serviceName -> !services.contains(serviceName))
            .collect(Collectors.toList());
    }

    protected List<String> calculateServicesToRebind(Set<String> services,
        Map<String, Map<String, Object>> serviceNamesWithBindingParameters, CloudApplication application) {
        List<String> servicesToRebind = new ArrayList<>();
        for (String serviceName : services) {
            if (!application.getServices()
                .contains(serviceName)) {
                continue;
            }

            CloudServiceInstance cloudServiceInstance = client.getServiceInstance(serviceName);
            Map<String, Object> newServiceBindings = getNewServiceBindings(serviceNamesWithBindingParameters, cloudServiceInstance);
            if (hasServiceBindingsChanged(application, cloudServiceInstance.getBindings(), newServiceBindings)) {
                servicesToRebind.add(cloudServiceInstance.getService()
                    .getName());
            }
        }
        return servicesToRebind;
    }

    private Map<String, Object> getNewServiceBindings(Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
        CloudServiceInstance serviceInstance) {
        return serviceNamesWithBindingParameters.get(serviceInstance.getService()
            .getName());
    }

    private boolean hasServiceBindingsChanged(CloudApplication application, List<CloudServiceBinding> existingServiceBindings,
        Map<String, Object> newServiceBindings) {
        CloudServiceBinding bindingForApplication = getServiceBindingsForApplication(application, existingServiceBindings);
        return !Objects.equals(bindingForApplication.getBindingParameters(), newServiceBindings);
    }

    private CloudServiceBinding getServiceBindingsForApplication(CloudApplication application, List<CloudServiceBinding> serviceBindings) {
        return serviceBindings.stream()
            .filter(serviceBinding -> application.getMeta()
                .getGuid()
                .equals(serviceBinding.getApplicationGuid()))
            .findFirst()
            .orElse(null);
    }

    private List<String> getUpdatedServiceNames(List<String> addServices, List<String> deleteServices, List<String> rebindServices) {
        List<String> result = new ArrayList<>();
        result.addAll(addServices);
        result.addAll(deleteServices);
        result.addAll(rebindServices);
        return result;
    }

}
