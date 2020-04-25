package tn.mnlr.vripper.web.restendpoints.domain.posts;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RemoveResult {
    private String postId;

    public RemoveResult(String postId) {
        this.postId = postId;
    }
}
