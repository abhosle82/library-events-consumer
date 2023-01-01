package events.lib.consumer.start.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import events.lib.consumer.start.entities.LibraryEvent;
import events.lib.consumer.start.entities.LibraryEventType;
import events.lib.consumer.start.repository.LibraryEventsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class LibraryEventsService {

    @Autowired
    LibraryEventsRepository libaryEventsRepository;

    @Autowired
    ObjectMapper objectMapper;



    public void processLibraryEvents(ConsumerRecord<Integer,String> consumerRecord) throws JsonProcessingException {
        LibraryEvent libraryEvent = objectMapper.readValue(consumerRecord.value(),LibraryEvent.class);

        log.info("Library Event {}",libraryEvent);

        switch(libraryEvent.getLibraryEventType()){
            case NEW:
                saveLibraryEvent(libraryEvent);
                break;
            case UPDATE:
                validateLibraryEvent(libraryEvent);
                saveLibraryEvent(libraryEvent);
                break;
            default:
                log.error("Not a valid Library Event Type");
        }

    }

    private void validateLibraryEvent(LibraryEvent libraryEvent) {
        if(libraryEvent.getLibraryEventId()==null){
            throw new RecoverableDataAccessException("Library Event Id cannot be null");
        }
        Optional<LibraryEvent> libraryEventOptional = libaryEventsRepository.findById(libraryEvent.getLibraryEventId());
        if(!libraryEventOptional.isPresent()){
            throw new RecoverableDataAccessException("Invalid Library Event Id");
        }
        log.info("successfully validated the library event id");
    }

    private void saveLibraryEvent(LibraryEvent libraryEvent) {
        libraryEvent.getBook().setLibraryEvent(libraryEvent);
        libaryEventsRepository.save(libraryEvent);
        log.info("LibraryEvent saved successfully :{} ",libraryEvent);
    }
}
