package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.jpa.repositories.IPostRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PostRepository implements IPostRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong counter = new AtomicLong(0);

    @Autowired
    public PostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

    }

    @Override
    public void init() {
        Long maxId = jdbcTemplate.queryForObject(
                "SELECT MAX(ID) FROM POST",
                Long.class
        );
        if (maxId == null) {
            maxId = 0L;
        }
        counter.set(maxId);
    }

    @Override
    public Post save(Post post) {
        long id = counter.incrementAndGet();
        jdbcTemplate.update(
                "INSERT INTO POST (ID, DONE, FORUM, HOSTS, POST_FOLDER_NAME, POST_ID, PREVIEWS, SECURITY_TOKEN, STATUS, THANKED, THREAD_ID, THREAD_TITLE, TITLE, TOTAL, URL) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id,
                post.getDone(),
                post.getForum(),
                String.join(";", post.getHosts()),
                post.getPostFolderName(),
                post.getPostId(),
                String.join(";", post.getPreviews()),
                post.getSecurityToken(),
                post.getStatus().name(),
                post.isThanked(),
                post.getThreadId(),
                post.getThreadTitle(),
                post.getTitle(),
                post.getTotal(),
                post.getUrl()
        );
        post.setId(id);
        return null;
    }

    @Override
    public int delete(Long id) {
        return jdbcTemplate.update(
                "DELETE FROM POST AS post WHERE post.ID = ?",
                id
        );
    }

    @Override
    public Optional<Post> findByPostId(String postId) {
        List<Post> posts = jdbcTemplate.query(
                "SELECT metadata.*,post.* FROM METADATA metadata FULL JOIN POST post ON metadata.POST_ID_REF = post.ID WHERE POST_ID = ?",
                new Object[]{postId},
                new PostRowMapper()
        );
        if (posts.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(posts.get(0));
        }
    }

    @Override
    public List<String> findCompleted() {
        return jdbcTemplate.query(
                "SELECT POST_ID FROM POST WHERE status = 'COMPLETE' AND done >= total",
                ((rs, rowNum) -> rs.getString("POST_ID"))
        );
    }

    @Override
    public Optional<Post> findById(Long id) {
        List<Post> posts = jdbcTemplate.query(
                "SELECT metadata.*,post.* FROM METADATA metadata FULL JOIN POST post ON metadata.POST_ID_REF = post.ID WHERE post.ID = ?",
                new Object[]{id},
                new PostRowMapper()
        );
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
                new PostRowMapper()
        );
    }

    @Override
    public boolean existByPostId(String postId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM POST AS post WHERE post.POST_ID = ?",
                new Object[]{postId},
                Integer.class
        );
        if (count == null) {
            return false;
        } else {
            return count > 0;
        }
    }

    @Override
    public int setDownloadingToStopped() {
        return jdbcTemplate.update(
                "UPDATE POST AS post SET post.STATUS = 'STOPPED' WHERE post.STATUS = 'DOWNLOADING' OR post.STATUS = 'PARTIAL' OR post.STATUS = 'PENDING'"
        );
    }

    @Override
    public int deleteByPostId(String postId) {
        return jdbcTemplate.update(
                "DELETE FROM POST AS post WHERE post.POST_ID = ?",
                postId
        );
    }

    @Override
    public int updateStatus(Status status, Long id) {
        return jdbcTemplate.update(
                "UPDATE POST AS post SET post.STATUS = ? WHERE post.ID = ?",
                status.name(), id
        );
    }

    @Override
    public int updateDone(int done, Long id) {
        return jdbcTemplate.update(
                "UPDATE POST AS post SET post.DONE = ? WHERE post.ID = ?",
                done, id
        );
    }

    @Override
    public int updateFolderName(String postFolderName, Long id) {
        return jdbcTemplate.update(
                "UPDATE POST AS post SET post.POST_FOLDER_NAME = ? WHERE post.ID = ?",
                postFolderName, id
        );
    }

    @Override
    public int updateTitle(String title, Long id) {
        return jdbcTemplate.update(
                "UPDATE POST AS post SET post.TITLE = ? WHERE post.ID = ?",
                title, id
        );
    }

    @Override
    public int updateThanked(boolean thanked, Long id) {
        return jdbcTemplate.update(
                "UPDATE POST AS post SET post.THANKED = ? WHERE post.ID = ?",
                thanked, id
        );
    }
}
