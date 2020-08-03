package tn.mnlr.vripper.services;

import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.q.ExecutionService;
import tn.mnlr.vripper.q.PendingQ;

@Service
@EnableScheduling
public class GlobalStateService {

    private final PendingQ pendingQ;
    private final ExecutionService executionService;
    private final DataService dataService;

    @Getter
    private GlobalState currentState;

    @Getter
    private final PublishProcessor<GlobalState> liveGlobalState = PublishProcessor.create();

    @Autowired
    public GlobalStateService(PendingQ pendingQ, ExecutionService executionService, DataService dataService) {
        this.pendingQ = pendingQ;
        this.executionService = executionService;
        this.dataService = dataService;
    }

    @Scheduled(fixedDelay = 3000)
    private void interval() {
        GlobalState newGlobalState = new GlobalState(
                executionService.runningCount(),
                pendingQ.size(),
                dataService.countErrorImages());
        if (!newGlobalState.equals(currentState)) {
            currentState = newGlobalState;
            liveGlobalState.onNext(currentState);
        }
    }
}
