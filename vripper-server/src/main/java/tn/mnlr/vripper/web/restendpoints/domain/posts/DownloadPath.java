package tn.mnlr.vripper.web.restendpoints.domain.posts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DownloadPath {
    private String path;

    public DownloadPath(String path) {
        this.path = path;
    }
}