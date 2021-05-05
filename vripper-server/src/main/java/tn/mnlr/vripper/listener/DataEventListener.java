package tn.mnlr.vripper.listener;

import reactor.core.publisher.Flux;

public interface DataEventListener<T> {
  Flux<T> getDataFlux();
}
