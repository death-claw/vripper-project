package tn.mnlr.vripper.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tn.mnlr.vripper.event.PostRemoveEvent;

import javax.annotation.PreDestroy;

@Component
@Slf4j
public class PostRemoveEventListener
    implements ApplicationListener<PostRemoveEvent>, DataEventListener<PostRemoveEvent> {

  private final Sinks.Many<PostRemoveEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

  @Override
  public void onApplicationEvent(PostRemoveEvent event) {
    sink.emitNext(event, EmitHandler.RETRY);
  }

  @Override
  public Flux<PostRemoveEvent> getDataFlux() {
    return sink.asFlux();
  }

  @PreDestroy
  private void destroy() {
    sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
  }
}
