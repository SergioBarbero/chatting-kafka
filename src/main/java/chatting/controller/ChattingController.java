package chatting.controller;

import chatting.model.ChattingMessage;
import chatting.service.Sender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;


@Controller
public class ChattingController {

    private final Sender sender;

    private final static String BOOT_TOPIC = "chatting";

    public ChattingController(Sender sender, SimpMessagingTemplate template) {
        this.sender = sender;
    }

    @MessageMapping("/message")
    public void sendMessage(ChattingMessage message) throws Exception {
        String data = message.getMessage() + "|" + message.getFrom() + "|" + message.getTo();
        sender.send(BOOT_TOPIC, data);
    }

    @MessageMapping("/file")
    @SendTo("/topic/chatting")
    public ChattingMessage sendFile(ChattingMessage message) throws Exception {
        return new ChattingMessage(message.getFileName(), message.getRawData(), message.getFrom());
    }
}