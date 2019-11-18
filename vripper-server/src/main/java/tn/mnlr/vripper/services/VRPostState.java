package tn.mnlr.vripper.services;

import lombok.Getter;

@Getter
public abstract class VRPostState {
    private final String threadId;

    VRPostState(String threadId) {
        this.threadId = threadId;
    }
}
