package chatting.service;

import chatting.model.ChattingMessage;
import chatting.model.ReceivedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;

@Service
public class Receiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(Receiver.class);

    private final CountDownLatch latch = new CountDownLatch(1);
    private final SimpMessagingTemplate template;

    public Receiver(SimpMessagingTemplate template) {
        this.template = template;
    }

    @KafkaListener(topics = "${topic.boot}")
    public void receive(ConsumerRecord<?, ?> consumerRecord) throws Exception {
        String message = consumerRecord.value().toString();
        ChattingMessage chattingMessage = new ObjectMapper().readValue(message, ChattingMessage.class);
        LOGGER.info("received data='{}'", message);
        String destination = "/user/" + chattingMessage.getTo() + "/queue/chatting";
        LOGGER.info("sending message='{}' to destination={}", chattingMessage, destination);
        ReceivedMessage receivedMessage = new ReceivedMessage(chattingMessage.getMessage(), chattingMessage.getFrom());
        this.template.convertAndSend(destination, receivedMessage);
        latch.countDown();
    }
}