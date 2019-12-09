package tn.mnlr.vripper.services;

import lombok.Getter;

import java.util.List;

@Getter
public class VRPostState {
    private final String type = "postParse";
    private final String threadId;
    private String postId;
    private int number;
    private String title;
    private int imageCount;
    private String url;
    private List<String> previews;
    private String hosts;

    VRPostState(String threadId, String postId, int number, String title, int imageCount, String url, List<String> previews, String hosts) {
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
