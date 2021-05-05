package tn.mnlr.vripper.listener;

import reactor.core.publisher.Sinks.EmitFailureHandler;

public class EmitHandler {
  public static final EmitFailureHandler RETRY = (signalType, emitResult) -> true;
}
