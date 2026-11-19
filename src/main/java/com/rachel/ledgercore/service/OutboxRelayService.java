package com.rachel.ledgercore.service;

import com.rachel.ledgercore.enums.OutboxStatus;
import com.rachel.ledgercore.model.OutboxEvent;
import com.rachel.ledgercore.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processOutboxEvents(){
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEventWithLock(10);

        if(pendingEvents.isEmpty()){
            return;
        }

        log.info("Outbox Relay found {} pending events to publish", pendingEvents.size());

        for(OutboxEvent event : pendingEvents){
            try{
                publishToMessageBroker(event);

                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                log.info("Successfully published outbox event; {} for transfer; {}", event.getId(), event.getAggregateId());

            } catch (Exception e){
                log.error("Failed to publish outbox event {}. Will retry. Error: {}",event.getId(),e.getMessage());

                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 3) {
                    event.setStatus(OutboxStatus.FAILED);
                }
                outboxEventRepository.save(event);
            }
        }
    }

    private void publishToMessageBroker(OutboxEvent event){
        log.info(" [>>] Dispatched to Message Broker: Topic=[{}], Payload={}", event.getEventType(), event.getPayload());
    }
}
