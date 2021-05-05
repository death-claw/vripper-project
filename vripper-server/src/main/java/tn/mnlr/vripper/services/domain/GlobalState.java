package tn.mnlr.vripper.services.domain;

import lombok.Getter;

import java.util.Objects;

@Getter
public class GlobalState {

  private final long running;
  private final long remaining;
  private final long error;

  public GlobalState(long running, long remaining, long error) {
    this.running = running;
    this.remaining = remaining;
    this.error = error;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GlobalState that = (GlobalState) o;
    return running == that.running && remaining == that.remaining && error == that.error;
  }

  @Override
  public int hashCode() {
    return Objects.hash(running, remaining, error);
  }
}
