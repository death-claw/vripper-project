package tn.mnlr.vripper.q;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;

import java.util.Objects;
import java.util.concurrent.Callable;

public class DownloadJob implements Callable<Image> {

    private static final Logger logger = LoggerFactory.getLogger(DownloadJob.class);

    @Getter
    private final Image image;
    private final Post post;

    @Getter
    private final ImageFileData imageFileData = new ImageFileData();

    DownloadJob(Post post, Image image) {
        this.image = image;
        this.post = post;
    }

    @Override
    public Image call() throws Exception {

        logger.debug(String.format("Starting downloading %s", image.getUrl()));
        image.setStatus(Image.Status.DOWNLOADING);
        image.setCurrent(0);
        image.getHost().download(post, image, imageFileData);
        image.setStatus(Image.Status.COMPLETE);
        return image;
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
