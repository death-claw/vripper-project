package tn.mnlr.vripper.jpa.repositories;

import tn.mnlr.vripper.jpa.domain.Event;

import java.util.List;
import java.util.Optional;

public interface IEventRepository extends IRepository {

    Event save(Event event);

    Event update(Event event);

    List<Event> findAll();

    Optional<Event> findById(Long id);

    void delete(Long id);

    void deleteAll();
}
