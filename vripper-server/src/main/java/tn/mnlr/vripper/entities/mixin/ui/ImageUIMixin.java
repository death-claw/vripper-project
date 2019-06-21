package tn.mnlr.vripper.entities.mixin.ui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.BehaviorProcessor;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.services.AppStateService;

public abstract class ImageUIMixin {

    @JsonIgnore
    private Host host;

    @JsonIgnore
    private BehaviorProcessor<Image> imageStateProcessor;

    @JsonIgnore
    private Disposable subscription;

    @JsonIgnore
    private AppStateService appStateService;
}
