package chatting.controller;

import chatting.model.ChattingMessage;
import chatting.service.Receiver;
import chatting.service.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Arrays;

@Controller
public class ChattingController {

    private final Sender sender;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChattingController.class);

    private final static String BOOT_TOPIC = "chatting";

    @Autowired
    private SimpMessagingTemplate template;


    public ChattingController(Sender sender) {
        this.sender = sender;
    }

//    @MessageMapping("/message")
//    public void sendMessage(ChattingMessage message) throws Exception {
//        Thread.sleep(1000); // simulated delay
//        // TODO: Serialize data in JSON
//        String data = message.getMessage() + "|" + message.getFrom() + "|" + message.getTo();
//        sender.send(BOOT_TOPIC, data);
//    }

//    Sending message without kafka
    @MessageMapping("/message")
    public void sendMessage(ChattingMessage message) throws Exception {
        Thread.sleep(1000); // simulated delay
        // TODO: Serialize data in JSON
        String data = message.getMessage() + "|" + message.getFrom() + "|" + message.getTo();
        String[] m = data.split("\\|");
        String destination = "/user/" + m[2] + "/queue/chatting";
        LOGGER.info("sending message='{}' to destination={}", Arrays.toString(m), destination);
        this.template.convertAndSend(destination, new ChattingMessage(m[1], m[2], m[0]));
        //sender.send(BOOT_TOPIC, data);
    }

    @MessageMapping("/file")
    @SendTo("/topic/chatting")
    public ChattingMessage sendFile(ChattingMessage message) throws Exception {
        return new ChattingMessage(message.getFileName(), message.getRawData(), message.getFrom());
    }
}