package tn.mnlr.vripper.jpa.repositories;

import tn.mnlr.vripper.jpa.domain.Metadata;

import java.util.Optional;

public interface IMetadataRepository extends IRepository {

  Metadata save(Metadata metadata);

  Optional<Metadata> findByPostId(String postId);

  int deleteByPostId(String postId);
}
