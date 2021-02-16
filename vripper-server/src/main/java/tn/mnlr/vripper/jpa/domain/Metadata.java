package tn.mnlr.vripper.jpa.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Metadata {

    private Long postIdRef;

    private String PostId;

    private String postedBy;

    private List<String> resolvedNames = Collections.emptyList();
}
