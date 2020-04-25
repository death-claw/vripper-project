package tn.mnlr.vripper.web.restendpoints.domain.posts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PostToAdd {
    private String threadId;
    private String postId;
}