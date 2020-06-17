import chatting.Application;
import chatting.model.ChattingMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

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
@Testcontainers
public class ChattingKafkaIT {

    //@Container
    //public KafkaContainer kafka = new KafkaContainer();

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
        //kafka.addExposedPort(9092);

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
