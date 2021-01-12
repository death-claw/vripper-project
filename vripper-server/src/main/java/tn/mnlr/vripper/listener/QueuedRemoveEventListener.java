package tn.mnlr.vripper.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tn.mnlr.vripper.event.QueuedRemoveEvent;

import javax.annotation.PreDestroy;

@Component
@Slf4j
public class QueuedRemoveEventListener implements ApplicationListener<QueuedRemoveEvent>, DataEventListener<QueuedRemoveEvent> {

    private final Sinks.Many<QueuedRemoveEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public void onApplicationEvent(QueuedRemoveEvent event) {
        sink.emitNext(event, (signalType, emitResult) -> true);
    }

    @Override
    public Flux<QueuedRemoveEvent> getDataFlux() {
        return sink.asFlux();
    }

    @PreDestroy
    private void destroy() {
        sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
