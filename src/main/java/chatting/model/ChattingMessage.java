package chatting.model;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class ChattingMessage {

    private String message;
    private String from;
    private String to;
    private String fileName;
    private byte[] rawData;

    public ChattingMessage(String from, String to, String message) {
        this.from = from;
        this.message = message;
        this.to = to;
    }

    public ChattingMessage() {}

    public ChattingMessage(String fileName, byte[] rawData, String from) {
        this.fileName = fileName;
        this.rawData = rawData;
        this.from = from;
    }
}
