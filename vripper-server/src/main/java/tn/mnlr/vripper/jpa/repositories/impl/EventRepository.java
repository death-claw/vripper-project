package tn.mnlr.vripper.jpa.repositories.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.event.EventRemoveEvent;
import tn.mnlr.vripper.event.EventUpdateEvent;
import tn.mnlr.vripper.jpa.domain.Event;
import tn.mnlr.vripper.jpa.repositories.IEventRepository;
import tn.mnlr.vripper.services.SettingsService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EventRepository implements IEventRepository, ApplicationEventPublisherAware {

    private final JdbcTemplate jdbcTemplate;
    private final SettingsService settingsService;
    private ApplicationEventPublisher applicationEventPublisher;

    public EventRepository(JdbcTemplate jdbcTemplate, SettingsService settingsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.settingsService = settingsService;
    }

    private synchronized Long nextId() {
        return jdbcTemplate.queryForObject(
                "CALL NEXT VALUE FOR SEQ_EVENT",
                Long.class);
    }

    @Override
    public synchronized Event save(@NonNull Event event) {

        int maxRecords = settingsService.getSettings().getMaxEventLog() - 1;

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM EVENT", Long.class);
        if (count > maxRecords) {
            List<Long> idList = jdbcTemplate.queryForList("SELECT ID FROM EVENT ORDER BY TIME ASC LIMIT ?", Long.class, count - maxRecords);
            idList.forEach(this::delete);
        }

        long id = nextId();
        jdbcTemplate.update(
                "INSERT INTO EVENT (ID, TYPE, STATUS, TIME, MESSAGE) VALUES (?,?,?,?,?)",
                id,
                event.getType().name(),
                event.getStatus().name(),
                event.getTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                event.getMessage()
        );
        event.setId(id);
        applicationEventPublisher.publishEvent(new EventUpdateEvent(EventRepository.class, id));
        return event;
    }

    @Override
    public Event update(@NonNull Event event) {
        if (event.getId() == null) {
            log.warn("Cannot update entity with null id");
            return event;
        }

        jdbcTemplate.update(
                "UPDATE EVENT SET TYPE = ?, STATUS = ?, TIME = ?, MESSAGE = ? WHERE ID = ?",
                event.getType().name(),
                event.getStatus().name(),
                event.getTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                event.getMessage(),
                event.getId()
        );
        applicationEventPublisher.publishEvent(new EventUpdateEvent(EventRepository.class, event.getId()));
        return event;
    }

    @Override
    public Optional<Event> findById(Long id) {
        List<Event> events = jdbcTemplate.query(
                "SELECT * FROM EVENT WHERE ID = ?",
                new EventRowMapper(),
                id);
        if (events.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(events.get(0));
        }
    }

    @Override
    public List<Event> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM EVENT",
                new EventRowMapper());
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM EVENT WHERE ID = ?", id);
        applicationEventPublisher.publishEvent(new EventRemoveEvent(EventRepository.class, id));
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM EVENT");
    }

    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
