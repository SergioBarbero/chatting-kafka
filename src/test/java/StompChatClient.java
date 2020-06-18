import chatting.model.ChattingMessage;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class StompChatClient implements AfterEachCallback {

    private final BlockingQueue<String> queue = new LinkedBlockingDeque<>();
    private final StompFrameHandler stompFrameHandler = new ChatFrameHandler();

    private class ChatFrameHandler implements StompFrameHandler {
        private final Logger logger = LoggerFactory.getLogger(ChatFrameHandler.class);

        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            return ChattingMessage.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            String message = o.toString();
            logger.info("Received : " + message);
            queue.offer(message);
        }
    }

    public StompFrameHandler getStompFrameHandler() {
        return stompFrameHandler;
    }

    public String poll(int seconds) throws InterruptedException {
        return queue.poll(seconds, TimeUnit.SECONDS);
    }

    public int getSizeOfReceivedElements() {
        return queue.size();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        queue.clear();
    }

}
