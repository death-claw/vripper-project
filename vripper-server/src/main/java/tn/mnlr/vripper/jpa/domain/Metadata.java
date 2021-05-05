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

  private String postId;

  private String postedBy;

  private List<String> resolvedNames = Collections.emptyList();

  public static Metadata from(Metadata metadata) {
    Metadata copy = new Metadata();
    copy.postIdRef = metadata.postIdRef;
    copy.postId = metadata.postId;
    copy.postedBy = metadata.postedBy;
    copy.resolvedNames = List.copyOf(metadata.resolvedNames);
    return copy;
  }
}
