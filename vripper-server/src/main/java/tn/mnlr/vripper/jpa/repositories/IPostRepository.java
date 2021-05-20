package tn.mnlr.vripper.jpa.repositories;

import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.enums.Status;

import java.util.List;
import java.util.Optional;

public interface IPostRepository extends IRepository {

  Post save(Post post);

  Optional<Post> findByPostId(String postId);

  Optional<Post> findById(Long id);

  List<String> findCompleted();

  List<Post> findAll();

  boolean existByPostId(String postId);

  int setDownloadingToStopped();

  int deleteByPostId(String postId);

  int updateStatus(Status status, Long id);

  int updateDone(int done, Long id);

  int updateFolderName(String postFolderName, Long id);

  int updateTitle(String title, Long id);

  int updateThanked(boolean thanked, Long id);

  int updateRank(int rank, Long id);
}
