package tn.mnlr.vripper.web.restendpoints.domain.posts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PostId {
    public PostId(String postId) {
        this.postId = postId;
    }

    private String postId;
}