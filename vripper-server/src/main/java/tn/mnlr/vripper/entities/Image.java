package tn.mnlr.vripper.entities;

import io.reactivex.disposables.Disposable;
import io.reactivex.processors.BehaviorProcessor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.services.AppStateService;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@NoArgsConstructor
public class Image {

    @Setter
    private AppStateService appStateService;
    private Disposable subscription;

    private final String type = "img";

    private String postId;

    private String postName;

    private Host host;

    private String url;

    private AtomicLong current = new AtomicLong(0);
    private Status status;
    private BehaviorProcessor<Image> imageStateProcessor;

    @Setter
    private long total = 0;

    public Image(String url, String postId, String postName, Host host, AppStateService appStateService) {
        this.url = url;
        this.postId = postId;
        this.postName = postName;
        this.host = host;
        this.status = Status.PENDING;
        this.appStateService = appStateService;
        this.imageStateProcessor = BehaviorProcessor.create();
        this.appStateService.getCurrentImages().put(this.url, this);
        this.appStateService.getAllImageState().onNext(this);
        this.init();
    }

    public void setStatus(Status status) {
        this.status = status;
        this.update();
    }

    public boolean isCompleted() {
        return this.status.equals(Status.COMPLETE);
    }

    public void init() {
        if (this.imageStateProcessor != null) {
            this.imageStateProcessor.onComplete();
        }
        if (this.subscription != null) {
            this.subscription.dispose();
        }
        this.imageStateProcessor = BehaviorProcessor.create();
        this.subscription = imageStateProcessor
                .onBackpressureBuffer()
                .doOnNext(appStateService::onImageUpdate)
                .subscribe();

        this.current.set(0);
        this.status = Status.PENDING;
        imageStateProcessor.onNext(this);
    }

    public void setCurrent(int current) {
        this.current.set(current);
        this.update();
    }

    public void increase(int read) {
        this.current.addAndGet(read);
        this.update();
    }

    private void update() {
        if(imageStateProcessor == null) {
            return;
        }
        imageStateProcessor.onNext(this);
        if (isCompleted()) {
            imageStateProcessor.onComplete();
            this.subscription.dispose();
            this.imageStateProcessor = null;
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
