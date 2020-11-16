package tn.mnlr.vripper.web.restendpoints.domain;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class RemoveAllResult {
    private List<String> postIds;
    private int removed;

    public RemoveAllResult(List<String> postIds) {
        this.removed = postIds.size();
        this.postIds = postIds;
    }
}
