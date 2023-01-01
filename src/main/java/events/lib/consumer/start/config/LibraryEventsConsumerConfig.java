package events.lib.consumer.start.config;


import events.lib.consumer.start.service.FailureRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

@Configuration
@EnableKafka
@Slf4j
public class LibraryEventsConsumerConfig {


    private final KafkaProperties properties;

    public LibraryEventsConsumerConfig(KafkaProperties properties) {
        this.properties = properties;
    }

    @Autowired
    private FailureRecordService failureRecordService;

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Value("${spring.kafka.topic.retry}")
    private String retryTopic;

    @Value("${spring.kafka.topic.dlt}")
    private String dltTopic;



    public DeadLetterPublishingRecoverer publishingRecoverer(){
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,(r,e)->{
            log.error("Exception in publishingRecoverer : {} ", e.getMessage(), e);
            if (e.getCause() instanceof RecoverableDataAccessException) {
                log.info("Publishing to retry topic : {} ", retryTopic);
                return new TopicPartition(retryTopic, r.partition());
            }
            else {
                log.info("Publishing to dead letter topic : {} ", dltTopic);
                return new TopicPartition(dltTopic, r.partition());
            }
        });
        return recoverer;
    }

    ConsumerRecordRecoverer consumerRecordRecoverer = ((consumerRecord, e) ->{
       var failureRecord = (ConsumerRecord<Integer,String>)consumerRecord;
       log.info("Failure Record {}",failureRecord);
       if(e.getCause() instanceof RecoverableDataAccessException){
           log.info("Failure Record : In Recovery");
           failureRecordService.saveFailureRecord(failureRecord,e,"RETRY");
       }else{
           log.info("Failure Record : In Non-Recovery");
           failureRecordService.saveFailureRecord(failureRecord,e,"DEAD");
       }
    });

    public DefaultErrorHandler defaultErrorHandler(){
        
        var fixedBackoff = new FixedBackOff(1000l,2);

        /*var exponentialBackoff = new ExponentialBackOffWithMaxRetries(2);
        exponentialBackoff.setInitialInterval(1000l);
        exponentialBackoff.setMultiplier(3);
        exponentialBackoff.setMaxInterval(3000l);*/

        var exceptionsListToIgnore = List.of(NullPointerException.class);
        var exceptionsListToRetry = List.of(RecoverableDataAccessException.class);
        //var defaultErrorHandler = new DefaultErrorHandler(fixedBackoff);
        //var defaultErrorHandler = new DefaultErrorHandler(exponentialBackoff);
        //var defaultErrorHandler = new DefaultErrorHandler(publishingRecoverer(),exponentialBackoff);
        var defaultErrorHandler = new DefaultErrorHandler(publishingRecoverer(),fixedBackoff);
        //var defaultErrorHandler = new DefaultErrorHandler(consumerRecordRecoverer,fixedBackoff);
        exceptionsListToIgnore.forEach(defaultErrorHandler::addNotRetryableExceptions);
        exceptionsListToRetry.forEach(defaultErrorHandler::addRetryableExceptions);
        defaultErrorHandler.setRetryListeners((record, ex, deliveryAttempt)->{
            log.info("In the Default Error Handler with - Exception {}, Delivery Attempt {}",ex.getMessage(),deliveryAttempt);
        });
        return defaultErrorHandler;
    }


    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConcurrentKafkaListenerContainerFactoryConfigurer configurer,ObjectProvider<ConsumerFactory<Object, Object>> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory.getIfAvailable(() -> new DefaultKafkaConsumerFactory<>(this.properties.buildConsumerProperties())));
        //factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(defaultErrorHandler());
        return factory;
    }



}
