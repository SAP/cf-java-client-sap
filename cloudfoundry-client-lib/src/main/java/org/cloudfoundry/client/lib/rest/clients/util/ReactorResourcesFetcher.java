package org.cloudfoundry.client.lib.rest.clients.util;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.cloudfoundry.client.lib.domain.Derivable;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactorResourcesFetcher {

    public static <T, R, D extends Derivable<T>> List<T> fetchListWithAuxiliaryContent(Supplier<Flux<R>> resourceSupplier,
                                                                                       Function<R, Mono<D>> resourceMapper) {
        return fetchFluxWithAuxiliaryContent(resourceSupplier, resourceMapper).collectList()
                                                                              .block();
    }

    public static <T, R, D extends Derivable<T>> Flux<T> fetchFluxWithAuxiliaryContent(Supplier<Flux<R>> resourceSupplier,
                                                                                       Function<R, Mono<D>> resourceMapper) {
        return resourceSupplier.get()
                               .flatMap(resourceMapper)
                               .map(Derivable::derive);
    }

    public static <T, R, D extends Derivable<T>> List<T> fetchList(Supplier<Flux<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return fetchFlux(resourceSupplier, resourceMapper).collectList()
                                                          .block();
    }

    public static <T, R, D extends Derivable<T>> Flux<T> fetchFlux(Supplier<Flux<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return resourceSupplier.get()
                               .map(resourceMapper)
                               .map(Derivable::derive);
    }

    public static <T, R, D extends Derivable<T>> T fetchWithAuxiliaryContent(Supplier<Mono<R>> resourceSupplier,
                                                                             Function<R, Mono<D>> resourceMapper) {
        return fetchMonoWithAuxiliaryContent(resourceSupplier, resourceMapper).block();
    }

    public static <T, R, D extends Derivable<T>> T fetch(Supplier<Mono<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return fetchMono(resourceSupplier, resourceMapper).block();
    }

    public static <T, R, D extends Derivable<T>> Mono<T> fetchMonoWithAuxiliaryContent(Supplier<Mono<R>> resourceSupplier,
                                                                                       Function<R, Mono<D>> resourceMapper) {
        return resourceSupplier.get()
                               .flatMap(resourceMapper)
                               .map(Derivable::derive);
    }

    public static <T, R, D extends Derivable<T>> Mono<T> fetchMono(Supplier<Mono<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return resourceSupplier.get()
                               .map(resourceMapper)
                               .map(Derivable::derive);
    }
}
