package tn.mnlr.vripper.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tn.mnlr.vripper.event.MetadataUpdateEvent;

import javax.annotation.PreDestroy;

@Component
@Slf4j
public class MetadataUpdateEventListener
    implements ApplicationListener<MetadataUpdateEvent>, DataEventListener<MetadataUpdateEvent> {

  private final Sinks.Many<MetadataUpdateEvent> sink =
      Sinks.many().multicast().onBackpressureBuffer();

  @Override
  public void onApplicationEvent(MetadataUpdateEvent event) {
    sink.emitNext(event, (signalType, emitResult) -> true);
  }

  @Override
  public Flux<MetadataUpdateEvent> getDataFlux() {
    return sink.asFlux();
  }

  @PreDestroy
  private void destroy() {
    sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
  }
}
