package tn.mnlr.vripper.services.domain;

import lombok.Getter;

import java.util.List;

@Getter
public class MultiPostScanResult {
  private final List<MultiPostItem> posts;
  private final String error;

  public MultiPostScanResult(List<MultiPostItem> posts, String error) {
    this.posts = posts;
    this.error = error;
  }
}
