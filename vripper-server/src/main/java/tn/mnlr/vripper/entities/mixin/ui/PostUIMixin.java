package tn.mnlr.vripper.entities.mixin.ui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.services.AppStateService;

import java.util.List;
import java.util.Map;

public abstract class PostUIMixin {

    @JsonIgnore
    private List<Image> images;

    @JsonIgnore
    private Map<String, String> metadata;

    @JsonIgnore
    private AppStateService appStateService;
}
