package tn.mnlr.vripper.web.restendpoints.domain.posts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class RemoveAllResult {
    private List<String> postIds;
    private int removed;

    public RemoveAllResult(List<String> postIds) {
        this.removed = postIds.size();
        this.postIds = postIds;
    }
}