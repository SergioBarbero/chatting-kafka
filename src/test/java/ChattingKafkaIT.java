import chatting.Application;
import chatting.model.ChattingMessage;
import chatting.model.ReceivedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

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
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
public class ChattingKafkaIT {

    @LocalServerPort
    private int port;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @RegisterExtension
    StompChatClient steveChatClient = new StompChatClient();

    @RegisterExtension
    StompChatClient breadChatClient = new StompChatClient();

    @RegisterExtension
    StompChatClient pepitoChatClient = new StompChatClient();

    private final WebSocketStompClient stompClient = getWebSocketStompClient();

    @Test
    public void shouldSendMessageToFriend() throws ExecutionException, InterruptedException {
        String url = getUrl();
        StompSession steveSession = stompClient.connect(url, new StompSessionHandlerAdapter() {}).get();
        StompSession breadSession = stompClient.connect(url, new StompSessionHandlerAdapter() {}).get();

        steveSession.subscribe("/user/steve/queue/chatting", steveChatClient.getStompFrameHandler());
        breadSession.subscribe("/user/bread/queue/chatting", breadChatClient.getStompFrameHandler());

        steveSession.send("/app/message", new ChattingMessage("steve", "bread", "Hey Bread"));

        Instant sentMessage1At = Instant.now();
        assertMessage(breadChatClient.poll(), "Hey Bread", "steve", sentMessage1At);

        assertThat(breadChatClient.getSizeOfReceivedElements()).isEqualTo(0);
        assertThat(pepitoChatClient.getSizeOfReceivedElements()).isEqualTo(0);
    }

    @Test
    public void shouldSendMessagesToFriend() throws ExecutionException, InterruptedException {
        // given
        String url = getUrl();
        StompSession steveSession = stompClient.connect(url, new StompSessionHandlerAdapter() {}).get();
        StompSession breadSession = stompClient.connect(url, new StompSessionHandlerAdapter() {}).get();

        // Each person subscribes to each own inbox
        // when
        steveSession.subscribe("/user/steve/queue/chatting", steveChatClient.getStompFrameHandler());
        breadSession.subscribe("/user/bread/queue/chatting", breadChatClient.getStompFrameHandler());

        Instant sentMessage1At = Instant.now();
        steveSession.send("/app/message", new ChattingMessage("steve", "bread", "Hey Bread"));
        Thread.sleep(20); // Adding minimum delay between messages

        Instant sentMessage2At = Instant.now();
        steveSession.send("/app/message", new ChattingMessage("steve", "bread", "How are you doing?"));
        Thread.sleep(20);

        Instant sentMessage3At = Instant.now();
        breadSession.send("/app/message", new ChattingMessage("bread", "steve", "My girlfriend has left me"));
        Thread.sleep(20);

        Instant sentMessage4At = Instant.now();
        breadSession.send("/app/message", new ChattingMessage("bread", "steve", "I'm broken"));
        Thread.sleep(20);

        Instant sentMessage5At = Instant.now();
        steveSession.send("/app/message", new ChattingMessage("steve", "bread", "I'm so sorry mate"));

        // then
        assertMessage(steveChatClient.poll(), "My girlfriend has left me", "bread", sentMessage1At);
        assertMessage(steveChatClient.poll(), "I'm broken", "bread", sentMessage2At);
        assertMessage(breadChatClient.poll(), "Hey Bread", "steve", sentMessage3At);
        assertMessage(breadChatClient.poll(), "How are you doing?", "steve", sentMessage4At);
        assertMessage(breadChatClient.poll(), "I'm so sorry mate", "steve", sentMessage5At);
        assertThat(pepitoChatClient.getSizeOfReceivedElements()).isEqualTo(0);
    }

    private void assertMessage(ReceivedMessage actual, String expectedMessage, String expectedFrom, Instant sentAt) {
        assertThat(actual).isNotNull();
        assertThat(actual.getFrom()).isEqualTo(expectedFrom);
        assertThat(actual.getMessage()).isEqualTo(expectedMessage);

        // Message should be have a timestamp in between these thresholds
        assertThat(actual.getSentAt()).isAfter(sentAt.minus(500, ChronoUnit.MILLIS));
        assertThat(actual.getSentAt()).isBefore(sentAt.plus(500, ChronoUnit.MILLIS));
    }

    private String getUrl() {
        return "ws://localhost:" + port + "/chatting";
    }

    private static WebSocketStompClient getWebSocketStompClient() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule timeModule = new JavaTimeModule();
        // For the deserializer to deserialize to Timestamp when needed
        timeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
        timeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        objectMapper.registerModule(timeModule);

        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(
                asList(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(converter);
        converter.setObjectMapper(objectMapper);
        return stompClient;
    }
}
