package org.cloudfoundry.client.lib.rest.clients.tasks;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchList;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudTask;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.v3.tasks.CancelTaskRequest;
import org.cloudfoundry.client.v3.tasks.CreateTaskRequest;
import org.cloudfoundry.client.v3.tasks.GetTaskRequest;
import org.cloudfoundry.client.v3.tasks.ListTasksRequest;
import org.cloudfoundry.client.v3.tasks.Task;
import org.cloudfoundry.util.PaginationUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CloudControllerTasksClientImpl extends CloudControllerBaseClient implements CloudControllerTasksClient {

    private CloudControllerApplicationsClient applicationsClient;

    public CloudControllerTasksClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                          CloudControllerApplicationsClient applicationsClient) {
        super(target, v3Client);
        this.applicationsClient = applicationsClient;
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return fetch(() -> getTaskResource(taskGuid), ImmutableRawCloudTask::of);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        UUID applicationGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        return fetchList(() -> getTaskResourcesByApplicationGuid(applicationGuid), ImmutableRawCloudTask::of);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        UUID applicationGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        return createTask(applicationGuid, task);
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return fetch(() -> cancelTaskResource(taskGuid), ImmutableRawCloudTask::of);
    }

    private Mono<? extends Task> getTaskResource(UUID guid) {
        GetTaskRequest request = GetTaskRequest.builder()
                                               .taskId(guid.toString())
                                               .build();
        return v3Client.tasks()
                       .get(request);
    }

    private Flux<? extends Task> getTaskResourcesByApplicationGuid(UUID applicationGuid) {
        IntFunction<ListTasksRequest> pageRequestSupplier = page -> ListTasksRequest.builder()
                                                                                    .applicationId(applicationGuid.toString())
                                                                                    .page(page)
                                                                                    .build();
        return PaginationUtils.requestClientV3Resources(page -> v3Client.tasks()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private CloudTask createTask(UUID applicationGuid, CloudTask task) {
        return fetch(() -> createTaskResource(applicationGuid, task), ImmutableRawCloudTask::of);
    }

    private Mono<? extends Task> createTaskResource(UUID applicationGuid, CloudTask task) {
        CreateTaskRequest request = CreateTaskRequest.builder()
                                                     .applicationId(applicationGuid.toString())
                                                     .command(task.getCommand())
                                                     .name(task.getName())
                                                     .memoryInMb(task.getLimits()
                                                                     .getMemory())
                                                     .diskInMb(task.getLimits()
                                                                   .getDisk())
                                                     .build();
        return v3Client.tasks()
                       .create(request);
    }

    private Mono<? extends Task> cancelTaskResource(UUID taskGuid) {
        CancelTaskRequest request = CancelTaskRequest.builder()
                                                     .taskId(taskGuid.toString())
                                                     .build();
        return v3Client.tasks()
                       .cancel(request);
    }
}
