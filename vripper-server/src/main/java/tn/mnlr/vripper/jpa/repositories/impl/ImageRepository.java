package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.jpa.repositories.IImageRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ImageRepository implements IImageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong counter = new AtomicLong(0);

    @Autowired
    public ImageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void init() {
        Long maxId = jdbcTemplate.queryForObject(
                "SELECT MAX(ID) FROM IMAGE",
                Long.class
        );
        if (maxId == null) {
            maxId = 0L;
        }
        counter.set(maxId);
    }

    @Override
    public Image save(Image image) {
        long id = counter.incrementAndGet();
        jdbcTemplate.update(
                "INSERT INTO IMAGE (ID, CURRENT, HOST, INDEX, POST_ID, STATUS, TOTAL, URL, POST_ID_REF) VALUES (?,?,?,?,?,?,?,?,?)",
                id,
                image.getCurrent(),
                image.getHost().getHost(),
                image.getIndex(),
                image.getPostId(),
                image.getStatus().name(),
                image.getTotal(),
                image.getUrl(),
                image.getPostIdRef()
        );
        image.setId(id);
        return image;
    }

    @Override
    public void deleteAllByPostId(String postId) {
        jdbcTemplate.update("DELETE FROM IMAGE WHERE POST_ID = ?", postId);
    }

    @Override
    public List<Image> findByPostId(String postId) {
        return jdbcTemplate.query(
                "SELECT * FROM IMAGE WHERE POST_ID = ?",
                new Object[]{postId},
                new ImageRowMapper()
        );
    }

    @Override
    public Integer countRemaining() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM IMAGE AS image WHERE image.TOTAL = 0 OR image.TOTAL <> image.CURRENT",
                Integer.class
        );
    }

    @Override
    public Integer countError() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM IMAGE AS image WHERE image.STATUS = 'ERROR'",
                Integer.class
        );
    }

    @Override
    public List<Image> findByPostIdAndIsNotCompleted(String postId) {
        return jdbcTemplate.query(
                "SELECT * FROM IMAGE AS image WHERE image.POST_ID = ? AND image.STATUS <> 'COMPLETE'",
                new Object[]{postId},
                new ImageRowMapper()
        );
    }

    @Override
    public int stopByPostIdAndIsNotCompleted(String postId) {
        return jdbcTemplate.update(
                "UPDATE IMAGE AS image SET image.STATUS = 'STOPPED' WHERE image.POST_ID = ? AND image.STATUS <> 'COMPLETE'",
                postId
        );
    }

    @Override
    public List<Image> findByPostIdAndIsError(String postId) {
        return jdbcTemplate.query(
                "SELECT * FROM IMAGE AS image WHERE image.POST_ID = ? AND image.STATUS = 'ERROR'",
                new Object[]{postId},
                new ImageRowMapper()
        );
    }

    @Override
    public Optional<Image> findById(Long id) {
        List<Image> images = jdbcTemplate.query(
                "SELECT * FROM IMAGE AS image WHERE image.ID = ?",
                new Object[]{id},
                new ImageRowMapper()
        );
        if (images.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(images.get(0));
        }
    }

    @Override
    public int updateStatus(Status status, Long id) {
        return jdbcTemplate.update(
                "UPDATE IMAGE AS image SET image.STATUS = ? WHERE image.ID = ?",
                status.name(), id
        );
    }

    @Override
    public int updateCurrent(long current, Long id) {
        return jdbcTemplate.update(
                "UPDATE IMAGE AS image SET image.CURRENT = ? WHERE image.ID = ?",
                current, id
        );
    }

    @Override
    public int updateTotal(long total, Long id) {
        return jdbcTemplate.update(
                "UPDATE IMAGE AS image SET image.TOTAL = ? WHERE image.ID = ?",
                total, id
        );
    }
}
