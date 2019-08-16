package tn.mnlr.vripper.entities.mixin.ui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.services.AppStateService;

import java.util.List;

public abstract class PostUIMixin {

    @JsonIgnore
    private List<Image> images;

    @JsonIgnore
    private AppStateService appStateService;
}
