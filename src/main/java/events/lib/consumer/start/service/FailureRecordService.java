package events.lib.consumer.start.service;

import events.lib.consumer.start.entities.FailureRecord;
import events.lib.consumer.start.repository.FailureRecordRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Service
public class FailureRecordService {

    private FailureRecordRepository failureRecordRepository;

    public FailureRecordService(FailureRecordRepository failureRecordRepository) {
        this.failureRecordRepository = failureRecordRepository;
    }

    public void saveFailureRecord(ConsumerRecord<Integer,String> consumerRecord,Exception e, String status){
        var failureRecord = FailureRecord.builder()
                .topic(consumerRecord.topic())
                .topicKey(consumerRecord.key())
                .errorRecord(consumerRecord.value())
                .exception(e.getCause().getMessage())
                .offset_value(consumerRecord.offset())
                .partition(consumerRecord.partition())
                .status(status).build();
        failureRecordRepository.save(failureRecord);
    }
}
