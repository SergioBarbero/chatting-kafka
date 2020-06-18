import chatting.model.ChattingMessage;
import chatting.model.ReceivedMessage;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class StompChatClient implements AfterEachCallback {

    private final BlockingQueue<ReceivedMessage> queue = new LinkedBlockingDeque<>();
    private final StompFrameHandler stompFrameHandler = new ChatFrameHandler();

    private class ChatFrameHandler implements StompFrameHandler {
        private final Logger logger = LoggerFactory.getLogger(ChatFrameHandler.class);

        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            return ReceivedMessage.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            ReceivedMessage receivedMessage = (ReceivedMessage) o;
            logger.info("Received : " + receivedMessage.toString());
            queue.offer(receivedMessage);
        }
    }

    public StompFrameHandler getStompFrameHandler() {
        return stompFrameHandler;
    }

    public ReceivedMessage poll() throws InterruptedException {
        return queue.poll(3, TimeUnit.SECONDS);
    }

    public int getSizeOfReceivedElements() {
        return queue.size();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        queue.clear();
    }

}
