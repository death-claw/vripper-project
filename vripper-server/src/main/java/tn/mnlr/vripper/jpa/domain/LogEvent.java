package tn.mnlr.vripper.jpa.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class LogEvent {

  private Long id;
  private Type type;
  private Status status;

  @JsonSerialize(using = DateTimeSerializer.class)
  private LocalDateTime time;

  private String message;

  public LogEvent(Type type, Status status, LocalDateTime time, String message) {
    this.type = type;
    this.status = status;
    this.time = time;
    this.message = message;
  }

  public enum Type {
    POST,
    QUEUED,
    THANKS,
    METADATA,
    SCAN,
    DOWNLOAD,

    // Below are deprecated events
    METADATA_CACHE_MISS,
    QUEUED_CACHE_MISS
  }

  public enum Status {
    PENDING,
    PROCESSING,
    DONE,
    ERROR
  }
}
