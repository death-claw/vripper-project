package tn.mnlr.vripper.services;

import lombok.Getter;

import java.util.Objects;

@Getter
public class GlobalState {

    private final long running;
    private final long queued;
    private final long remaining;
    private final long error;

    GlobalState(long running, long queued, long remaining, long error) {
        this.running = running;
        this.queued = queued;
        this.remaining = remaining;
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GlobalState that = (GlobalState) o;
        return running == that.running && queued == that.queued && remaining == that.remaining && error == that.error;
    }

    @Override
    public int hashCode() {
        return Objects.hash(running, queued, remaining, error);
    }
}
