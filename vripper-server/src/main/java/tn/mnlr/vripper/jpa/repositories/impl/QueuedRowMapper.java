package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.jdbc.core.RowMapper;
import tn.mnlr.vripper.jpa.domain.Queued;

import java.sql.ResultSet;
import java.sql.SQLException;

public class QueuedRowMapper implements RowMapper<Queued> {

    @Override
    public Queued mapRow(ResultSet rs, int rowNum) throws SQLException {
        Queued queued = new Queued();
        queued.setId(rs.getLong("ID"));
        queued.setLink(rs.getString("LINK"));
        queued.setThreadId(rs.getString("THREAD_ID"));
        queued.setPostId(rs.getString("POST_ID"));
        queued.setTotal(rs.getInt("TOTAL"));
        queued.setLoading(rs.getBoolean("LOADING"));
        return queued;
    }
}
