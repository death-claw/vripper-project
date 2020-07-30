package tn.mnlr.vripper.jpa.repositories;

import tn.mnlr.vripper.jpa.domain.Queued;

import java.util.List;
import java.util.Optional;

public interface IQueuedRepository extends IRepository {
    Queued save(Queued queued);

    Optional<Queued> findByThreadId(String threadId);

    List<Queued> findAll();

    Optional<Queued> findById(Long id);

    int deleteByThreadId(String threadId);
}
