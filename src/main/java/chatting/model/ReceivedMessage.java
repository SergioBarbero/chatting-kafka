package chatting.model;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@ToString
@Getter
public class ReceivedMessage {

    private String message;
    private String from;
    private Instant sentAt;

    public ReceivedMessage() {}; // Default constructor for Jackson

    public ReceivedMessage(String message, String from) {
        this.message = message;
        this.from = from;
        this.sentAt = Instant.now();
    }
}
