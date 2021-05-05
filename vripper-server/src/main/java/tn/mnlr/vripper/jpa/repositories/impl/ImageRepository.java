package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.event.ImageUpdateEvent;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.jpa.repositories.IImageRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ImageRepository implements IImageRepository, ApplicationEventPublisherAware {

  private final JdbcTemplate jdbcTemplate;
  private ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public ImageRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private synchronized Long nextId() {
    return jdbcTemplate.queryForObject("CALL NEXT VALUE FOR SEQ_IMAGE", Long.class);
  }

  @Override
  public Image save(Image image) {
    long id = nextId();
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
        image.getPostIdRef());
    image.setId(id);
    applicationEventPublisher.publishEvent(new ImageUpdateEvent(ImageRepository.class, id));
    return image;
  }

  @Override
  public void deleteAllByPostId(String postId) {
    jdbcTemplate.update("DELETE FROM IMAGE WHERE POST_ID = ?", postId);
  }

  @Override
  public List<Image> findByPostId(String postId) {
    return jdbcTemplate.query(
        "SELECT * FROM IMAGE WHERE POST_ID = ?", new ImageRowMapper(), postId);
  }

  @Override
  public Integer countError() {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM IMAGE AS image WHERE image.STATUS = 'ERROR'", Integer.class);
  }

  @Override
  public List<Image> findByPostIdAndIsNotCompleted(String postId) {
    return jdbcTemplate.query(
        "SELECT * FROM IMAGE AS image WHERE image.POST_ID = ? AND image.STATUS <> 'COMPLETE'",
        new ImageRowMapper(),
        postId);
  }

  @Override
  public int stopByPostIdAndIsNotCompleted(String postId) {
    return jdbcTemplate.update(
        "UPDATE IMAGE AS image SET image.STATUS = 'STOPPED' WHERE image.POST_ID = ? AND image.STATUS <> 'COMPLETE'",
        postId);
  }

  @Override
  public List<Image> findByPostIdAndIsError(String postId) {
    return jdbcTemplate.query(
        "SELECT * FROM IMAGE AS image WHERE image.POST_ID = ? AND image.STATUS = 'ERROR'",
        new ImageRowMapper(),
        postId);
  }

  @Override
  public Optional<Image> findById(Long id) {
    List<Image> images =
        jdbcTemplate.query(
            "SELECT * FROM IMAGE AS image WHERE image.ID = ?", new ImageRowMapper(), id);
    if (images.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(images.get(0));
    }
  }

  @Override
  public int updateStatus(Status status, Long id) {
    int mutationCount =
        jdbcTemplate.update(
            "UPDATE IMAGE AS image SET image.STATUS = ? WHERE image.ID = ?", status.name(), id);
    applicationEventPublisher.publishEvent(new ImageUpdateEvent(ImageRepository.class, id));
    return mutationCount;
  }

  @Override
  public int updateCurrent(long current, Long id) {
    int mutationCount =
        jdbcTemplate.update(
            "UPDATE IMAGE AS image SET image.CURRENT = ? WHERE image.ID = ?", current, id);
    applicationEventPublisher.publishEvent(new ImageUpdateEvent(ImageRepository.class, id));
    return mutationCount;
  }

  @Override
  public int updateTotal(long total, Long id) {
    int mutationCount =
        jdbcTemplate.update(
            "UPDATE IMAGE AS image SET image.TOTAL = ? WHERE image.ID = ?", total, id);
    applicationEventPublisher.publishEvent(new ImageUpdateEvent(ImageRepository.class, id));
    return mutationCount;
  }

  @Override
  public void setApplicationEventPublisher(
      @NonNull ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }
}
