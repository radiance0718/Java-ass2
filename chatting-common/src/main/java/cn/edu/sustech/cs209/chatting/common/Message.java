package cn.edu.sustech.cs209.chatting.common;

public class Message {

  private Long timestamp;

  private String sentBy;

  private String sendTo;

  private String data;

  public Message(Long timestamp, String sentBy, String sendTo, String data) {
    this.timestamp = timestamp;
    this.sentBy = sentBy;
    this.sendTo = sendTo;
    this.data = data;
  }

  public static Message getMessage(String s) {
    String[] li = s.split("/", 4);
    Message ret = new Message(Long.parseLong(li[0]), li[1], li[2], li[3]);
    //System.out.println(new Message(timestamp, sentBy, sendTo, data));
    System.out.println(ret);
    return ret;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public String getSentBy() {
    return sentBy;
  }

  public String getSendTo() {
    return sendTo;
  }

  public String getData() {
    return data;
  }

  @Override
  public String toString() {
    return this.timestamp.toString() + "/" + this.sentBy + "/" + this.sendTo + "/" + this.data;
  }
}
