package org.cloudfoundry.client.lib.rest.clients.stacks;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchList;

import java.util.List;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudStack;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.stacks.ListStacksRequest;
import org.cloudfoundry.client.v2.stacks.StackEntity;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CloudControllerStacksClientImpl extends CloudControllerBaseClient implements CloudControllerStacksClient {

    public CloudControllerStacksClientImpl(CloudSpace target, CloudFoundryClient v3Client) {
        super(target, v3Client);
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        CloudStack stack = findStackResource(name);
        if (stack == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Stack " + name + " not found.");
        }
        return stack;
    }

    @Override
    public List<CloudStack> getStacks() {
        return fetchList(this::getStackResources, ImmutableRawCloudStack::of);
    }

    private CloudStack findStackResource(String name) {
        return fetch(() -> getStackResourceByName(name), ImmutableRawCloudStack::of);
    }

    private Flux<? extends Resource<StackEntity>> getStackResources() {
        IntFunction<ListStacksRequest> pageRequestSupplier = page -> ListStacksRequest.builder()
                                                                                      .page(page)
                                                                                      .build();
        return getStackResources(pageRequestSupplier);
    }

    private Mono<? extends Resource<StackEntity>> getStackResourceByName(String name) {
        IntFunction<ListStacksRequest> pageRequestSupplier = page -> ListStacksRequest.builder()
                                                                                      .name(name)
                                                                                      .page(page)
                                                                                      .build();
        return getStackResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Resource<StackEntity>> getStackResources(IntFunction<ListStacksRequest> requestForPage) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.stacks()
                                                                        .list(requestForPage.apply(page)));
    }
}
