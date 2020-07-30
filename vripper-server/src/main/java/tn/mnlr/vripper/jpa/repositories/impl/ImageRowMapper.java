package tn.mnlr.vripper.jpa.repositories.impl;

import org.springframework.jdbc.core.RowMapper;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.enums.Status;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ImageRowMapper implements RowMapper<Image> {

    @Override
    public Image mapRow(ResultSet rs, int rowNum) throws SQLException {
        Image image = new Image();
        image.setId(rs.getLong("ID"));
        String host = rs.getString("HOST");
        image.setHost(SpringContext.getBeansOfType(Host.class).values().stream().filter(e -> e.getHost().equals(host)).findAny().orElse(null));
        image.setUrl(rs.getString("URL"));
        image.setIndex(rs.getInt("INDEX"));
        image.setCurrent(rs.getLong("CURRENT"));
        image.setTotal(rs.getLong("TOTAL"));
        image.setStatus(Status.valueOf(rs.getString("STATUS")));
        image.setPostId(rs.getString("POST_ID"));
        image.setPostIdRef(rs.getLong("POST_ID_REF"));
        return image;
    }
}
