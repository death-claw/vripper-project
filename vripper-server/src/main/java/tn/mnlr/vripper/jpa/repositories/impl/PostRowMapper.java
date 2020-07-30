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
        post.setStatus(Status.valueOf(rs.getString("STATUS")));
        post.setPostId(rs.getString("POST_ID"));
        post.setThreadTitle(rs.getString("THREAD_TITLE"));
        post.setThreadId(rs.getString("THREAD_ID"));
        post.setTitle(rs.getString("TITLE"));
        post.setUrl(rs.getString("URL"));
        post.setDone(rs.getInt("DONE"));
        post.setTotal(rs.getInt("TOTAL"));
        post.setHosts(Set.of(rs.getString("HOSTS").split(DELIMITER)));
        post.setForum(rs.getString("FORUM"));
        post.setSecurityToken(rs.getString("SECURITY_TOKEN"));
        post.setPostFolderName(rs.getString("POST_FOLDER_NAME"));
        post.setThanked(rs.getBoolean("THANKED"));
        String previews;
        if ((previews = rs.getString("PREVIEWS")) != null) {
            post.setPreviews(Set.of(previews.split(DELIMITER)));
        }

        Long metadataId = rs.getLong("metadata.ID");
        if (!rs.wasNull()) {
            Metadata metadata = new Metadata();
            metadata.setId(metadataId);
            metadata.setPostIdRef(rs.getLong("POST_ID_REF"));
            String resolvedNames = rs.getString("RESOLVED_NAMES");
            if (resolvedNames != null && !resolvedNames.isBlank()) {
                metadata.setResolvedNames(List.of(resolvedNames.split("%sep%")));
            }
            metadata.setPostedBy(rs.getString("POSTED_BY"));
            post.setMetadata(metadata);
        }

        return post;
    }
}
