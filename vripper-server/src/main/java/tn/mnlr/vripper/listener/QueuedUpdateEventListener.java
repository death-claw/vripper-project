package tn.mnlr.vripper.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tn.mnlr.vripper.event.QueuedUpdateEvent;

import javax.annotation.PreDestroy;

@Component
@Slf4j
public class QueuedUpdateEventListener implements ApplicationListener<QueuedUpdateEvent>, DataEventListener<QueuedUpdateEvent> {

    private Sinks.Many<QueuedUpdateEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public void onApplicationEvent(QueuedUpdateEvent event) {
        sink.emitNext(event, (signalType, emitResult) -> true);
    }

    @Override
    public Flux<QueuedUpdateEvent> getDataFlux() {
        return sink.asFlux();
    }

    @PreDestroy
    private void destroy() {
        sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
