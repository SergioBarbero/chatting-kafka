package chatting.service;

import chatting.model.ChattingMessage;
import chatting.model.ReceivedMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
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
        LOGGER.info("received data='{}'", consumerRecord.toString());
        String[] message = consumerRecord.value().toString().split("\\|");
        String destination = "/user/" + message[2] + "/queue/chatting";
        LOGGER.info("sending message='{}' to destination={}", Arrays.toString(message), destination);
        ReceivedMessage receivedMessage = new ReceivedMessage(message[0], message[1]);
        this.template.convertAndSend(destination, receivedMessage);
        latch.countDown();
    }
}