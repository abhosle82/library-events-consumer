package events.lib.consumer.start.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LibraryEventsRetryConsumer {

    @KafkaListener(topics ="${spring.kafka.topic.retry}",groupId = "retry-listener-group")
    public void onMessage(ConsumerRecord<Integer,String> consumerRecord){
        log.info("Topic Name {}",consumerRecord.topic());
        log.info("Partition Number {}",consumerRecord.partition());
        log.info("Consumer Record {}",consumerRecord.value());
        consumerRecord.headers().forEach(header->{
            log.info("Key: {} & Value: {} ",header.key(),header.value());
        });
    }
}
