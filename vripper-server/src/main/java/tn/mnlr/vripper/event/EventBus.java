package tn.mnlr.vripper.event;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import javax.annotation.PreDestroy;

@Service
public class EventBus {

  public static final Sinks.EmitFailureHandler RETRY = (signalType, emitResult) -> true;
  private final Sinks.Many<Event<?>> sink = Sinks.many().multicast().onBackpressureBuffer();

  public void publishEvent(Event<?> event) {
    sink.emitNext(event, RETRY);
  }

  public Flux<Event<?>> flux() {
    return sink.asFlux();
  }

  @PreDestroy
  private void destroy() {
    sink.emitComplete(RETRY);
  }
}
