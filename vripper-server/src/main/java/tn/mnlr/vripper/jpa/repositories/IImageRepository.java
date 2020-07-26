package tn.mnlr.vripper.jpa.repositories;

import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.enums.Status;

import java.util.List;
import java.util.Optional;

public interface IImageRepository extends IRepository {

    Image save(Image image);

    void deleteAllByPostId(String postId);

    List<Image> findByPostId(String postId);

    Integer countRemaining();

    Integer countError();

    List<Image> findByPostIdAndIsNotCompleted(String postId);

    int stopByPostIdAndIsNotCompleted(String postId);

    List<Image> findByPostIdAndIsError(String postId);

    Optional<Image> findById(Long id);

    int updateStatus(Status status, Long id);

    int updateCurrent(long current, Long id);

    int updateTotal(long total, Long id);
}
