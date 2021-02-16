package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.jdbc.core.RowMapper;
import tn.mnlr.vripper.jpa.domain.Event;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventRowMapper implements RowMapper<Event> {

    @Override
    public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
        Event event = new Event();
        event.setId(rs.getLong("ID"));
        event.setType(Event.Type.valueOf(rs.getString("TYPE")));
        event.setStatus(Event.Status.valueOf(rs.getString("STATUS")));
        event.setTime(LocalDateTime.parse(rs.getString("TIME"), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        event.setMessage(rs.getString("MESSAGE"));
        return event;
    }
}
