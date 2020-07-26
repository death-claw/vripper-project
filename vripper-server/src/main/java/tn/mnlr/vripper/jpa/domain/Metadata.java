package tn.mnlr.vripper.jpa.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Metadata {

    @JsonIgnore
    private Long id;

    private List<String> resolvedNames = Collections.emptyList();

    private String postedBy;

    private Long postIdRef;
}
