package tn.mnlr.vripper.q;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.function.CheckedRunnable;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.services.DataService;

import java.util.Objects;

@Slf4j
public class DownloadJob implements CheckedRunnable {

    private final DataService dataService;

    @Getter
    private Image image;

    @Getter
    private Post post;

    @Getter
    private final ImageFileData imageFileData = new ImageFileData();

    DownloadJob(Post post, Image image) {
        this.image = image;
        this.post = post;
        dataService = SpringContext.getBean(DataService.class);
    }

    @Override
    public void run() throws Exception {
        log.debug(String.format("Starting downloading %s", image.getUrl()));
        image.getHost().download(post, image, imageFileData);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadJob that = (DownloadJob) o;
        return Objects.equals(image, that.image) &&
                Objects.equals(post, that.post);
    }

    @Override
    public int hashCode() {
        return Objects.hash(image, post);
    }

    public void refresh() {
        post = dataService.findPostById(post.getId()).orElseThrow();
        image = dataService.findImageById(image.getId()).orElseThrow();
    }
}
