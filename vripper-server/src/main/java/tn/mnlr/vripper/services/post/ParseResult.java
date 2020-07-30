package tn.mnlr.vripper.services.post;

import lombok.Getter;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;

import java.util.Optional;
import java.util.Set;

public class ParseResult {

    private final Post post;

    @Getter
    private final Set<Image> images;

    ParseResult(Post post, Set<Image> images) {
        this.post = post;
        this.images = images;
    }

    public Optional<Post> getPost() {
        return Optional.ofNullable(post);
    }
}