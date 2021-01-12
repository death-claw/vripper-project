package tn.mnlr.vripper.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tn.mnlr.vripper.event.ImageUpdateEvent;

import javax.annotation.PreDestroy;

@Component
@Slf4j
public class ImageUpdateEventListener implements ApplicationListener<ImageUpdateEvent>, DataEventListener<ImageUpdateEvent> {

    private final Sinks.Many<ImageUpdateEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public void onApplicationEvent(ImageUpdateEvent event) {
        sink.emitNext(event, EmitHandler.RETRY);
    }

    @Override
    public Flux<ImageUpdateEvent> getDataFlux() {
        return sink.asFlux();
    }

    @PreDestroy
    private void destroy() {
        sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
