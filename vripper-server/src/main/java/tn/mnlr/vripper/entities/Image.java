package tn.mnlr.vripper.entities;

import io.reactivex.disposables.Disposable;
import io.reactivex.processors.BehaviorProcessor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.services.AppStateService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@NoArgsConstructor
public class Image {

    private static final Logger logger = LoggerFactory.getLogger(Image.class);

    private AppStateService appStateService;

    private Disposable subscription;

    private final String type = "img";

    private String postId;

    private String postName;

    private Host host;

    private String url;

    private int index;

    private AtomicLong current = new AtomicLong(0);
    private Status status;
    private BehaviorProcessor<Image> imageStateProcessor;

    @Setter
    private long total = 0;

    public Image(String url, String postId, String postName, Host host, int index) {
        this.url = url;
        this.postId = postId;
        this.postName = postName;
        this.host = host;
        this.index = index;
        status = Status.PENDING;
        imageStateProcessor = BehaviorProcessor.create();
    }

    public void setAppStateService(AppStateService appStateService) {
        this.appStateService = appStateService;
        appStateService.getCurrentImages().put(this.url, this);
        appStateService.getLiveImageUpdates().onNext(this);
    }

    public void setStatus(Status status) {
        this.status = status;
        update();
    }

    public boolean isCompleted() {
        return status.equals(Status.COMPLETE);
    }

    public void init() {
        if (appStateService == null) {
            logger.warn("Attempting to init the image whilst the App State is null, unexpected behaviour could occur from this");
            return;
        }
        cleanup();
        imageStateProcessor = BehaviorProcessor.create();
        subscription = imageStateProcessor
                .onBackpressureLatest()
                .doOnNext(appStateService::onImageUpdate)
                .subscribe();

        current.set(0);
        status = Status.PENDING;
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
        if(imageStateProcessor == null) {
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
