package tn.mnlr.vripper.entities.mixin.ui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.services.AppStateService;

import java.util.List;

@Getter
@Setter
public abstract class PostUIMixin {

    @JsonIgnore
    private List<Image> images;

    @JsonIgnore
    private AppStateService appStateService;
}
