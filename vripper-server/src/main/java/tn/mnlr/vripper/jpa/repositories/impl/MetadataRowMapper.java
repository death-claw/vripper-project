package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.jdbc.core.RowMapper;
import tn.mnlr.vripper.jpa.domain.Metadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MetadataRowMapper implements RowMapper<Metadata> {

    @Override
    public Metadata mapRow(ResultSet rs, int rowNum) throws SQLException {
        Metadata metadata = new Metadata();
        metadata.setPostIdRef(rs.getLong("POST_ID_REF"));
        metadata.setPostId(rs.getString("POST_ID"));
        metadata.setPostedBy(rs.getString("POSTED_BY"));
        String resolvedNames = rs.getString("RESOLVED_NAMES");
        if (resolvedNames != null && !resolvedNames.isBlank()) {
            metadata.setResolvedNames(List.of(resolvedNames.split("%sep%")));
        }
        return metadata;
    }
}
