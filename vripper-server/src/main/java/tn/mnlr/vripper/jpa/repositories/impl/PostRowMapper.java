package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.jdbc.core.RowMapper;
import tn.mnlr.vripper.jpa.domain.Metadata;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.enums.Status;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class PostRowMapper implements RowMapper<Post> {

    private static final String DELIMITER = ";";

    @Override
    public Post mapRow(ResultSet rs, int rowNum) throws SQLException {

        Post post = new Post();
        post.setId(rs.getLong("post.ID"));
        post.setStatus(Status.valueOf(rs.getString("post.STATUS")));
        post.setPostId(rs.getString("post.POST_ID"));
        post.setThreadTitle(rs.getString("post.THREAD_TITLE"));
        post.setThreadId(rs.getString("post.THREAD_ID"));
        post.setTitle(rs.getString("post.TITLE"));
        post.setUrl(rs.getString("post.URL"));
        post.setDone(rs.getInt("post.DONE"));
        post.setTotal(rs.getInt("post.TOTAL"));
        post.setHosts(Set.of(rs.getString("post.HOSTS").split(DELIMITER)));
        post.setForum(rs.getString("post.FORUM"));
        post.setSecurityToken(rs.getString("post.SECURITY_TOKEN"));
        post.setDownloadDirectory(rs.getString("post.POST_FOLDER_NAME"));
        post.setThanked(rs.getBoolean("post.THANKED"));
        String previews;
        if ((previews = rs.getString("post.PREVIEWS")) != null) {
            post.setPreviews(Set.of(previews.split(DELIMITER)));
        }

        Long metadataId = rs.getLong("metadata.POST_ID_REF");
        if (!rs.wasNull()) {
            Metadata metadata = new Metadata();
            metadata.setPostIdRef(metadataId);
            metadata.setPostId(rs.getString("metadata.POST_ID"));
            String resolvedNames = rs.getString("metadata.RESOLVED_NAMES");
            if (resolvedNames != null && !resolvedNames.isBlank()) {
                metadata.setResolvedNames(List.of(resolvedNames.split("%sep%")));
            }
            metadata.setPostedBy(rs.getString("metadata.POSTED_BY"));
            post.setMetadata(metadata);
        }

        return post;
    }
}
