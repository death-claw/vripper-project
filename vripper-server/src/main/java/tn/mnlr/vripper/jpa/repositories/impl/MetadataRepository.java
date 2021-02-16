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

@Service
public class MetadataRepository implements IMetadataRepository, ApplicationEventPublisherAware {

    private final JdbcTemplate jdbcTemplate;
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public MetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Metadata save(Metadata metadata) {
        jdbcTemplate.update(
                "INSERT INTO METADATA (POST_ID_REF, POST_ID, POSTED_BY, RESOLVED_NAMES) VALUES (?,?,?,?)",
                metadata.getPostIdRef(),
                metadata.getPostId(),
                metadata.getPostedBy(),
                String.join("%sep%", metadata.getResolvedNames())
        );
        applicationEventPublisher.publishEvent(new MetadataUpdateEvent(MetadataRepository.class, metadata.getPostIdRef()));
        return metadata;
    }

    @Override
    public Optional<Metadata> findByPostId(String postId) {
        List<Metadata> metadata = jdbcTemplate.query(
                "SELECT metadata.* FROM METADATA AS metadata WHERE metadata.POST_ID = ?",
                new MetadataRowMapper(),
                postId
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
                "DELETE FROM METADATA AS metadata WHERE metadata.POST_ID = ?",
                postId
        );
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
