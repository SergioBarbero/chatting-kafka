package chatting.controller;

import chatting.model.ChattingMessage;
import chatting.service.Sender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;


@Controller
public class ChattingController {

    private final Sender sender;

    private final static String BOOT_TOPIC = "chatting";

    public ChattingController(Sender sender) {
        this.sender = sender;
    }

    @MessageMapping("/message")
    public void sendMessage(ChattingMessage message) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(message);
        sender.send(BOOT_TOPIC, data);
    }

    @MessageMapping("/file")
    @SendTo("/topic/chatting")
    public ChattingMessage sendFile(ChattingMessage message) throws Exception {
        return new ChattingMessage(message.getFileName(), message.getRawData(), message.getFrom());
    }
}