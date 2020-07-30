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
    private final PostDataService postDataService;

    @Getter
    private GlobalState currentState;

    @Getter
    private final PublishProcessor<GlobalState> liveGlobalState = PublishProcessor.create();

    @Autowired
    public GlobalStateService(PendingQ pendingQ, ExecutionService executionService, PostDataService postDataService) {
        this.pendingQ = pendingQ;
        this.executionService = executionService;
        this.postDataService = postDataService;
    }

    @Scheduled(fixedDelay = 3000)
    private void interval() {
        GlobalState newGlobalState = new GlobalState(
                executionService.runningCount(),
                pendingQ.size(),
                postDataService.countRemainingImages(),
                postDataService.countErrorImages());
        if (!newGlobalState.equals(currentState)) {
            currentState = newGlobalState;
            liveGlobalState.onNext(currentState);
        }
    }
}
