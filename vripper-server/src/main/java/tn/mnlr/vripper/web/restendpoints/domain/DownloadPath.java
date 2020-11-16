package tn.mnlr.vripper.web.restendpoints.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DownloadPath {
    private String path;

    public DownloadPath(String path) {
        this.path = path;
    }
}
