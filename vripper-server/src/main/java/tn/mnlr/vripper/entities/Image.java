package tn.mnlr.vripper.entities;

import io.reactivex.disposables.Disposable;
import io.reactivex.processors.BehaviorProcessor;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.services.AppStateService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class Image {

    private static final Logger logger = LoggerFactory.getLogger(Image.class);
    private final AppStateService appStateService;

    private final String type = "img";
    private String postId;
    private String postName;
    private Host host;
    private String url;
    private int index;
    private AtomicLong current = new AtomicLong(0);

    @Setter
    private long total = 0;
    private Status status;
    private BehaviorProcessor<Image> imageStateProcessor;
    private Disposable subscription;

    public Image() {
        this.appStateService = SpringContext.getBean(AppStateService.class);
    }

    public Image(String url, String postId, String postName, Host host, int index) throws PostParseException {
        this();
        this.url = url;
        this.postId = postId;
        this.postName = postName;
        this.host = host;
        this.index = index;
        status = Status.STOPPED;

        if (!appStateService.newImage(this)) {
            throw new PostParseException("Image already loaded");
        }
    }

    public void setStatus(Status status) {
        this.status = status;
        update();
    }

    public boolean isCompleted() {
        return status.equals(Status.COMPLETE);
    }

    public void init() {
        cleanup();
        imageStateProcessor = BehaviorProcessor.create();
        subscription = imageStateProcessor
                .onBackpressureBuffer()
                .doOnNext(appStateService::imageUpdated)
                .subscribe();

        current.set(0);
        status = Status.STOPPED;
        imageStateProcessor.onNext(this);
    }

    public void cleanup() {
        if (imageStateProcessor != null) {
            imageStateProcessor.onComplete();
        }
        if (subscription != null) {
            subscription.dispose();
        }
    }

    public void setCurrent(int current) {
        this.current.set(current);
        update();
    }

    public void increase(int read) {
        current.addAndGet(read);
        update();
    }

    private void update() {
        if (imageStateProcessor == null) {
            return;
        }
        imageStateProcessor.onNext(this);
        if (isCompleted()) {
            imageStateProcessor.onComplete();
            subscription.dispose();
            imageStateProcessor = null;
        }
    }

    public enum Status {
        PENDING, DOWNLOADING, COMPLETE, ERROR, STOPPED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return url.equals(image.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
