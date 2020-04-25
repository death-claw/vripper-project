package tn.mnlr.vripper.web.restendpoints.domain.posts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AltPostName {
    private final String postId;
    private final String altName;

    public AltPostName(String postId, String altName) {
        this.postId = postId;
        this.altName = altName;
    }
}