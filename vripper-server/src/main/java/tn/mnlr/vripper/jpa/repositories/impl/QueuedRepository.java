package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.jpa.repositories.IQueuedRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class QueuedRepository implements IQueuedRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong counter = new AtomicLong(0);

    @Autowired
    public QueuedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void init() {
        Long maxId = jdbcTemplate.queryForObject(
                "SELECT MAX(ID) FROM QUEUED",
                Long.class
        );
        if (maxId == null) {
            maxId = 0L;
        }
        counter.set(maxId);
    }

    @Override
    public Queued save(Queued queued) {
        long id = counter.incrementAndGet();
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
        return jdbcTemplate.update(
                "DELETE FROM QUEUED AS queued WHERE THREAD_ID = ?",
                threadId
        );
    }
}
