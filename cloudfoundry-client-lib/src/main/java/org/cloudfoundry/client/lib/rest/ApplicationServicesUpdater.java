package org.cloudfoundry.client.lib.rest;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.ApplicationServicesUpdateCallback;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationServicesUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationServicesUpdater.class);

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

        List<String> rebindServices = calculateServicesToRebind(serviceNamesWithBindingParameters, application);

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

    protected List<String> calculateServicesToRebind(Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                                     CloudApplication application) {
        List<String> servicesToRebind = new ArrayList<>();
        for (String serviceName : serviceNamesWithBindingParameters.keySet()) {
            if (!application.getServices()
                            .contains(serviceName)) {
                continue;
            }

            CloudServiceInstance serviceInstance = client.getServiceInstance(serviceName);
            Map<String, Object> newServiceBindingParameters = getNewServiceBindingParameters(serviceNamesWithBindingParameters,
                                                                                             serviceInstance);
            if (hasServiceBindingsChanged(application, serviceInstance, newServiceBindingParameters)) {
                servicesToRebind.add(serviceInstance.getService()
                                                    .getName());
            }
        }
        return servicesToRebind;
    }

    private Map<String, Object> getNewServiceBindingParameters(Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                                               CloudServiceInstance serviceInstance) {
        return serviceNamesWithBindingParameters.get(serviceInstance.getService()
                                                                    .getName());
    }

    private boolean hasServiceBindingsChanged(CloudApplication application, CloudServiceInstance serviceInstance,
                                              Map<String, Object> newServiceBindingParameters) {
        CloudServiceBinding bindingForApplication = getServiceBindingForApplication(application, serviceInstance);
        return !Objects.equals(bindingForApplication.getBindingParameters(), newServiceBindingParameters);
    }

    private CloudServiceBinding getServiceBindingForApplication(CloudApplication application, CloudServiceInstance serviceInstance) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Looking for service binding between application \"{}\" and service instance \"{}\". Service bindings: {}",
                         getGuid(application), getGuid(serviceInstance), JsonUtil.convertToJson(serviceInstance.getBindings(), true));
        }
        return serviceInstance.getBindings()
                              .stream()
                              .filter(serviceBinding -> application.getMetadata()
                                                                   .getGuid()
                                                                   .equals(serviceBinding.getApplicationGuid()))
                              .findFirst()
                              .orElseThrow(() -> new IllegalStateException(MessageFormat.format("Application {0} was bound to service {1} which was unbound in parallel",
                                                                                                application.getName(),
                                                                                                serviceInstance.getService()
                                                                                                               .getName())));
    }

    private List<String> getUpdatedServiceNames(List<String> addServices, List<String> deleteServices, List<String> rebindServices) {
        List<String> result = new ArrayList<>();
        result.addAll(addServices);
        result.addAll(deleteServices);
        result.addAll(rebindServices);
        return result;
    }

    private UUID getGuid(CloudEntity entity) {
        return entity.getMetadata()
                     .getGuid();
    }

}
