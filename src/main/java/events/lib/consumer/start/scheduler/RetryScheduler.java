package events.lib.consumer.start.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import events.lib.consumer.start.entities.FailureRecord;
import events.lib.consumer.start.repository.FailureRecordRepository;
import events.lib.consumer.start.repository.LibraryEventsRepository;
import events.lib.consumer.start.service.LibraryEventsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
public class RetryScheduler {

    @Autowired
    private LibraryEventsService libraryEventsService;
    private FailureRecordRepository failureRecordRepository;

    public RetryScheduler(LibraryEventsService libraryEventsService, FailureRecordRepository failureRecordRepository) {
        this.libraryEventsService = libraryEventsService;
        this.failureRecordRepository = failureRecordRepository;
    }

    //@Scheduled(fixedRate = 10000)
    public void retryFailedRecords(){
        log.info("Retrying Failed Records Started.....");
        failureRecordRepository.findAllByStatus("RETRY").
                forEach(failureRecord->{
                    ConsumerRecord<Integer, String> consumerRecord = buildConsumerRecord(failureRecord);
                    try {
                        log.info("Retrying Failed Records {}:",consumerRecord);
                        libraryEventsService.processLibraryEvents(consumerRecord);
                        failureRecordRepository.save(failureRecord);
                    } catch (JsonProcessingException e) {
                        log.info("Error Saving Failed Records {}:",e.getCause().getMessage());
                        throw new RuntimeException(e);
                    }
                });
        log.info("Retrying Failed Records Completed.....");
    }

    public ConsumerRecord<Integer,String> buildConsumerRecord(FailureRecord failureRecord){
        return new ConsumerRecord<>(failureRecord.getTopic(),failureRecord.getPartition(),failureRecord.getOffset_value(),failureRecord.getTopicKey(),failureRecord.getErrorRecord());
    }
}
