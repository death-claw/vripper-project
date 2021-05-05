package tn.mnlr.vripper.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tn.mnlr.vripper.event.EventUpdateEvent;

import javax.annotation.PreDestroy;

@Component
@Slf4j
public class EventUpdateEventListener
    implements ApplicationListener<EventUpdateEvent>, DataEventListener<EventUpdateEvent> {

  private final Sinks.Many<EventUpdateEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

  @Override
  public void onApplicationEvent(@NonNull EventUpdateEvent event) {
    sink.emitNext(event, EmitHandler.RETRY);
  }

  @Override
  public Flux<EventUpdateEvent> getDataFlux() {
    return sink.asFlux();
  }

  @PreDestroy
  private void destroy() {
    sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
  }
}
