package tn.mnlr.vripper.entities.mixin.persistance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tn.mnlr.vripper.services.AppStateService;

public abstract class PostPersistanceMixin {

    @JsonIgnore
    private String type;

    @JsonIgnore
    private AppStateService appStateService;
}
