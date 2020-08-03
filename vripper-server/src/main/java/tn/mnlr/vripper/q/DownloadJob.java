package tn.mnlr.vripper.q;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.function.CheckedRunnable;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;

import java.util.Objects;

@Slf4j
public class DownloadJob implements CheckedRunnable {

    @Getter
    private final Image image;

    @Getter
    private final Post post;

    @Getter
    private final ImageFileData imageFileData = new ImageFileData();

    DownloadJob(Post post, Image image) {
        this.image = image;
        this.post = post;
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
}
