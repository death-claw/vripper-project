package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.event.QueuedRemoveEvent;
import tn.mnlr.vripper.event.QueuedUpdateEvent;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.jpa.repositories.IQueuedRepository;

import java.util.List;
import java.util.Optional;

@Service
public class QueuedRepository implements IQueuedRepository, ApplicationEventPublisherAware {

    private final JdbcTemplate jdbcTemplate;
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public QueuedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private synchronized Long nextId() {
        return jdbcTemplate.queryForObject(
                "CALL NEXT VALUE FOR SEQ_QUEUED",
                Long.class);
    }

    @Override
    public Queued save(Queued queued) {
        long id = nextId();
        jdbcTemplate.update(
                "INSERT INTO QUEUED (ID, TOTAL, LINK, LOADING, POST_ID, THREAD_ID) values (?,?,?,?,?,?)",
                id,
                queued.getTotal(),
                queued.getLink(),
                queued.isLoading(),
                queued.getPostId(),
                queued.getThreadId()
        );
        queued.setId(id);
        applicationEventPublisher.publishEvent(new QueuedUpdateEvent(QueuedRepository.class, id));
        return queued;
    }

    @Override
    public Optional<Queued> findByThreadId(String threadId) {
        List<Queued> queuedList = jdbcTemplate.query(
                "SELECT * FROM QUEUED AS queued WHERE queued.THREAD_ID = ?",
                new Object[]{threadId},
                new QueuedRowMapper()
        );
        if (queuedList.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(queuedList.get(0));
        }
    }

    @Override
    public List<Queued> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM QUEUED",
                new QueuedRowMapper()
        );
    }

    @Override
    public Optional<Queued> findById(Long id) {
        List<Queued> queuedList = jdbcTemplate.query(
                "SELECT * FROM QUEUED AS queued WHERE queued.ID = ?",
                new Object[]{id},
                new QueuedRowMapper()
        );
        if (queuedList.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(queuedList.get(0));
        }
    }

    @Override
    public int deleteByThreadId(String threadId) {
        int mutationCount = jdbcTemplate.update(
                "DELETE FROM QUEUED AS queued WHERE THREAD_ID = ?",
                threadId
        );
        applicationEventPublisher.publishEvent(new QueuedRemoveEvent(QueuedRepository.class, threadId));
        return mutationCount;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM QUEUED");
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
