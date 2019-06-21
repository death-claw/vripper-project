package tn.mnlr.vripper.entities.mixin.persistance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.BehaviorProcessor;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.services.AppStateService;

public abstract class ImagePersistanceMixin {

    @JsonSerialize(converter = HostToString.class)
    @JsonDeserialize(converter = StringToHost.class)
    private Host host;

    @JsonIgnore
    private BehaviorProcessor<Image> imageStateProcessor;

    @JsonIgnore
    private Disposable subscription;

    @JsonIgnore
    private String type;

    @JsonIgnore
    public abstract boolean isCompleted();

    @JsonIgnore
    private AppStateService appStateService;
}
