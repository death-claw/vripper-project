package tn.mnlr.vripper.q;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.mnlr.vripper.entities.Image;

import java.util.Objects;
import java.util.concurrent.Callable;

public class DownloadJob implements Callable<Image> {

    private Logger logger = LoggerFactory.getLogger(DownloadJob.class);

    @Getter
    final Image image;

    @Getter
    final ImageFileData imageFileData = new ImageFileData();

    public DownloadJob(Image image) {
        this.image = image;
    }

    @Override
    public Image call() throws Exception {

        logger.debug(String.format("Starting downloading %s", image.getUrl()));
        this.image.setStatus(Image.Status.DOWNLOADING);
        this.image.setCurrent(0);
        this.image.getHost().download(this.image, this.imageFileData);
        this.image.setStatus(Image.Status.COMPLETE);
        return this.image;
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
