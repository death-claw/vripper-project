package tn.mnlr.vripper.services.post;

import lombok.Getter;

import java.util.List;

@Getter
public class CachedPost {
    private final String type = "postParse";
    private final String threadId;
    private final String postId;
    private final int number;
    private final String title;
    private final int imageCount;
    private final String url;
    private final List<String> previews;
    private final String hosts;

    public CachedPost(String threadId, String postId, int number, String title, int imageCount, String url, List<String> previews, String hosts) {
        this.threadId = threadId;
        this.postId = postId;
        this.number = number;
        this.title = title;
        this.imageCount = imageCount;
        this.previews = previews;
        this.url = url;
        this.hosts = hosts;
    }
}
