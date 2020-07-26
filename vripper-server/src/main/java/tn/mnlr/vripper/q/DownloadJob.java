package tn.mnlr.vripper.q;

import lombok.Getter;
import net.jodah.failsafe.function.CheckedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;

import java.util.Objects;

public class DownloadJob implements CheckedRunnable {

    private static final Logger logger = LoggerFactory.getLogger(DownloadJob.class);

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
        if (Thread.interrupted()) {
            return;
        }
        logger.debug(String.format("Starting downloading %s", image.getUrl()));
        image.getHost().download(post, image, imageFileData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadJob that = (DownloadJob) o;
        return image.equals(that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(image);
    }
}
