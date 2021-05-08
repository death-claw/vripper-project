package tn.mnlr.vripper.jpa.repositories;

import tn.mnlr.vripper.jpa.domain.LogEvent;

import java.util.List;
import java.util.Optional;

public interface ILogEventRepository extends IRepository {

  LogEvent save(LogEvent logEvent);

  LogEvent update(LogEvent logEvent);

  List<LogEvent> findAll();

  Optional<LogEvent> findById(Long id);

  void delete(Long id);

  void deleteAll();
}
