package tn.mnlr.vripper.entities.mixin.persistance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import tn.mnlr.vripper.services.AppStateService;

@Getter
@Setter
public abstract class PostPersistanceMixin {

    @JsonIgnore
    private String type;

    @JsonIgnore
    private AppStateService appStateService;


    @JsonIgnore
    private boolean removed;
}
