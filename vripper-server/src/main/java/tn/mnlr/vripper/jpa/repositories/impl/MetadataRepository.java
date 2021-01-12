package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.event.MetadataUpdateEvent;
import tn.mnlr.vripper.jpa.domain.Metadata;
import tn.mnlr.vripper.jpa.repositories.IMetadataRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetadataRepository implements IMetadataRepository, ApplicationEventPublisherAware {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong counter = new AtomicLong(0);
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public MetadataRepository(JdbcTemplate jdbcTemplate) {
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
    public Metadata save(Metadata metadata) {
        long id = counter.incrementAndGet();
        jdbcTemplate.update(
                "INSERT INTO METADATA (ID, POSTED_BY, RESOLVED_NAMES, POST_ID_REF) VALUES (?,?,?,?)",
                id,
                metadata.getPostedBy(),
                String.join("%sep%", metadata.getResolvedNames()),
                metadata.getPostIdRef()
        );
        metadata.setId(id);
        applicationEventPublisher.publishEvent(new MetadataUpdateEvent(MetadataRepository.class, id, metadata.getPostIdRef()));
        return metadata;
    }

    @Override
    public Optional<Metadata> findById(Long id) {
        List<Metadata> metadata = jdbcTemplate.query(
                "SELECT * FROM METADATA AS metadata WHERE metadata.ID = ?",
                new Object[]{id},
                new MetadataRowMapper()
        );
        if (metadata.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(metadata.get(0));
        }
    }

    @Override
    public Optional<Metadata> findByPostId(String postId) {
        List<Metadata> metadata = jdbcTemplate.query(
                "SELECT metadata.* FROM METADATA AS metadata INNER JOIN POST post ON post.ID = metadata.POST_ID_REF WHERE post.POST_ID = ?",
                new Object[]{postId},
                new MetadataRowMapper()
        );
        if (metadata.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(metadata.get(0));
        }
    }

    @Override
    public int deleteByPostId(String postId) {
        return jdbcTemplate.update(
                "DELETE FROM METADATA AS metadata WHERE metadata.ID = (SELECT inner_metadata.ID FROM POST AS post INNER JOIN METADATA inner_metadata ON post.ID = inner_metadata.POST_ID_REF WHERE post.POST_ID = ?)",
                postId
        );
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
