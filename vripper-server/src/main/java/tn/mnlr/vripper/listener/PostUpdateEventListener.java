package tn.mnlr.vripper.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tn.mnlr.vripper.event.PostUpdateEvent;

import javax.annotation.PreDestroy;

@Component
@Slf4j
public class PostUpdateEventListener
    implements ApplicationListener<PostUpdateEvent>, DataEventListener<PostUpdateEvent> {

  private final Sinks.Many<PostUpdateEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

  @Override
  public void onApplicationEvent(PostUpdateEvent event) {
    sink.emitNext(event, EmitHandler.RETRY);
  }

  @Override
  public Flux<PostUpdateEvent> getDataFlux() {
    return sink.asFlux();
  }

  @PreDestroy
  private void destroy() {
    sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
  }
}
