package tn.mnlr.vripper.jpa.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Event {

    private Long id;
    private Type type;
    private Status status;
    @JsonSerialize(using = DateTimeSerializer.class)
    private LocalDateTime time;
    private String message;

    public Event(Type type, Status status, LocalDateTime time, String message) {
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
        METADATA_CACHE_MISS,
        QUEUED_CACHE_MISS
    }

    public enum Status {
        PENDING, PROCESSING, DONE, ERROR
    }
}

class DateTimeSerializer extends StdSerializer<LocalDateTime> {

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");

    protected DateTimeSerializer() {
        super(LocalDateTime.class);
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.format(DATE_TIME_FORMATTER));
    }
}
