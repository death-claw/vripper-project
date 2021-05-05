package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.event.PostRemoveEvent;
import tn.mnlr.vripper.event.PostUpdateEvent;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.jpa.repositories.IPostRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PostRepository implements IPostRepository, ApplicationEventPublisherAware {

  private final JdbcTemplate jdbcTemplate;
  private ApplicationEventPublisher applicationEventPublisher;

  @Autowired
  public PostRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private synchronized Long nextId() {
    return jdbcTemplate.queryForObject("CALL NEXT VALUE FOR SEQ_POST", Long.class);
  }

  @Override
  public Post save(Post post) {
    long id = nextId();
    jdbcTemplate.update(
        "INSERT INTO POST (ID, DONE, FORUM, HOSTS, POST_FOLDER_NAME, POST_ID, PREVIEWS, SECURITY_TOKEN, STATUS, THANKED, THREAD_ID, THREAD_TITLE, TITLE, TOTAL, URL) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        id,
        post.getDone(),
        post.getForum(),
        String.join(";", post.getHosts()),
        post.getDownloadDirectory(),
        post.getPostId(),
        String.join(";", post.getPreviews()),
        post.getSecurityToken(),
        post.getStatus().name(),
        post.isThanked(),
        post.getThreadId(),
        post.getThreadTitle(),
        post.getTitle(),
        post.getTotal(),
        post.getUrl());
    post.setId(id);
    applicationEventPublisher.publishEvent(new PostUpdateEvent(PostRepository.class, id));
    return post;
  }

  @Override
  public Optional<Post> findByPostId(String postId) {
    List<Post> posts =
        jdbcTemplate.query(
            "SELECT metadata.*,post.* FROM METADATA metadata FULL JOIN POST post ON metadata.POST_ID_REF = post.ID WHERE post.POST_ID = ?",
            new PostRowMapper(),
            postId);
    if (posts.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(posts.get(0));
    }
  }

  @Override
  public List<String> findCompleted() {
    return jdbcTemplate.query(
        "SELECT POST_ID FROM POST AS post WHERE status = 'COMPLETE' AND done >= total",
        ((rs, rowNum) -> rs.getString("POST_ID")));
  }

  @Override
  public Optional<Post> findById(Long id) {
    List<Post> posts =
        jdbcTemplate.query(
            "SELECT metadata.*,post.* FROM METADATA metadata FULL JOIN POST post ON metadata.POST_ID_REF = post.ID WHERE post.ID = ?",
            new PostRowMapper(),
            id);
    if (posts.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(posts.get(0));
    }
  }

  @Override
  public List<Post> findAll() {
    return jdbcTemplate.query(
        "SELECT metadata.*,post.* FROM METADATA metadata FULL JOIN POST post ON metadata.POST_ID_REF = post.ID",
        new PostRowMapper());
  }

  @Override
  public boolean existByPostId(String postId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM POST AS post WHERE post.POST_ID = ?", Integer.class, postId);
    if (count == null) {
      return false;
    } else {
      return count > 0;
    }
  }

  @Override
  public int setDownloadingToStopped() {
    return jdbcTemplate.update(
        "UPDATE POST AS post SET post.STATUS = 'STOPPED' WHERE post.STATUS = 'DOWNLOADING' OR post.STATUS = 'PARTIAL' OR post.STATUS = 'PENDING'");
  }

  @Override
  public int deleteByPostId(String postId) {
    int mutationCount =
        jdbcTemplate.update("DELETE FROM POST AS post WHERE post.POST_ID = ?", postId);
    applicationEventPublisher.publishEvent(new PostRemoveEvent(PostRepository.class, postId));
    return mutationCount;
  }

  @Override
  public int updateStatus(Status status, Long id) {
    int mutationCount =
        jdbcTemplate.update(
            "UPDATE POST AS post SET post.STATUS = ? WHERE post.ID = ?", status.name(), id);
    applicationEventPublisher.publishEvent(new PostUpdateEvent(PostRepository.class, id));
    return mutationCount;
  }

  @Override
  public int updateDone(int done, Long id) {
    int mutationCount =
        jdbcTemplate.update("UPDATE POST AS post SET post.DONE = ? WHERE post.ID = ?", done, id);
    applicationEventPublisher.publishEvent(new PostUpdateEvent(PostRepository.class, id));
    return mutationCount;
  }

  @Override
  public int updateFolderName(String postFolderName, Long id) {
    int mutationCount =
        jdbcTemplate.update(
            "UPDATE POST AS post SET post.POST_FOLDER_NAME = ? WHERE post.ID = ?",
            postFolderName,
            id);
    applicationEventPublisher.publishEvent(new PostUpdateEvent(PostRepository.class, id));
    return mutationCount;
  }

  @Override
  public int updateTitle(String title, Long id) {
    int mutationCount =
        jdbcTemplate.update("UPDATE POST AS post SET post.TITLE = ? WHERE post.ID = ?", title, id);
    applicationEventPublisher.publishEvent(new PostUpdateEvent(PostRepository.class, id));
    return mutationCount;
  }

  @Override
  public int updateThanked(boolean thanked, Long id) {
    int mutationCount =
        jdbcTemplate.update(
            "UPDATE POST AS post SET post.THANKED = ? WHERE post.ID = ?", thanked, id);
    applicationEventPublisher.publishEvent(new PostUpdateEvent(PostRepository.class, id));
    return mutationCount;
  }

  @Override
  public void setApplicationEventPublisher(
      @NonNull ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }
}
