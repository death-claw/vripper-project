package tn.mnlr.vripper.web.restendpoints.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RemoveResult {
    private String postId;

    public RemoveResult(String postId) {
        this.postId = postId;
    }
}
