import chatting.Application;
import chatting.model.ChattingMessage;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * We use 3 actors in order to test the application, we will call them...
 * Steve
 * Bread
 * Pepito
 * These three guys message each other. Every guy has each own queue, where they receive the messages from the others.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = Application.class)
@EmbeddedKafka(
        partitions = 1,
        controlledShutdown = false,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"})
public class ChattingKafkaIT {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @RegisterExtension
    StompChatClient steveChatClient = new StompChatClient();

    @RegisterExtension
    StompChatClient breadChatClient = new StompChatClient();

    @RegisterExtension
    StompChatClient pepitoChatClient = new StompChatClient();

    private final WebSocketStompClient stompClient = getWebSocketStompClient();

    @LocalServerPort
    private int port;

    @Test
    public void shouldSendMessageToFriend() throws ExecutionException, InterruptedException {
        String url = "ws://localhost:" + port + "/chatting";
        StompSession steveSession = stompClient.connect(url, new StompSessionHandlerAdapter() {}).get();
        StompSession breadSession = stompClient.connect(url, new StompSessionHandlerAdapter() {}).get();

        steveSession.subscribe("/user/steve/queue/chatting", steveChatClient.getStompFrameHandler());
        breadSession.subscribe("/user/bread/queue/chatting", breadChatClient.getStompFrameHandler());

        steveSession.send("/app/message", new ChattingMessage("steve", "bread", "Hey Bread"));

        String actual = breadChatClient.poll(2);

        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo("ChattingMessage{message='Hey Bread', from='steve', to='bread', fileName='null', rawData=null}");

        assertThat(breadChatClient.getSizeOfReceivedElements()).isEqualTo(0);
        assertThat(breadChatClient.getSizeOfReceivedElements()).isEqualTo(0);
    }

    private static WebSocketStompClient getWebSocketStompClient() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
                asList(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        return stompClient;
    }
}
