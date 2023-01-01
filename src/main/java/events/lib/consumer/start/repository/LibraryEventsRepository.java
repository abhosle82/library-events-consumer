package events.lib.consumer.start.repository;

import events.lib.consumer.start.entities.LibraryEvent;
import org.springframework.data.repository.CrudRepository;

public interface LibraryEventsRepository extends CrudRepository<LibraryEvent,Integer> {
}
