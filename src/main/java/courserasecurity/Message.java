package courserasecurity;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;


@NamedQuery(name = Message.QUERY_FIND_MSG_FOR_USER, query = "SELECT m FROM Message m WHERE m.receiver = ?1 order by m.sendTime DESC")
@NamedQuery(name = Message.QUERY_FIND_MSG_FROM_USER, query = "SELECT m FROM Message m WHERE m.sender = ?1 order by m.sendTime DESC")


@Entity
@Table(name = "messages")
public class Message implements IVClass {

	public static final String QUERY_FIND_MSG_FOR_USER = "FindMessagesForUser";
	public static final String QUERY_FIND_MSG_FROM_USER = "FindMessagesFromUser";

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	Long msgId;

	String sender;

	String receiver;

	@Transient
	String text;

	@Lob
	@Type(type = "org.hibernate.type.TextType")
	String cryptText;

	@Lob
	@Type(type = "org.hibernate.type.TextType")
	String sendKey;

	@Lob
	@Type(type = "org.hibernate.type.TextType")
	String rcvKey;

	@Lob
	@Type(type = "org.hibernate.type.TextType")
	String cryptIv;


	@CreationTimestamp
	private LocalDateTime sendTime;

	public Message() {

	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setSendTime(LocalDateTime ts) {
		this.sendTime = ts;
	}

	public String getSender() {
		return sender;
	}

	public String getReceiver() {
		return receiver;
	}

	public String getText() {
		return text;
	}
	
	public LocalDateTime getSendTime() {
		return sendTime;
	}

	@Override
	public String toString() {
		return "Message [msgId=" + msgId + ", sender=" + sender + ", receiver=" + receiver + ", text=" + text + "]";
	}


	public String getCryptText() {
		return cryptText;
	}

	public void setCryptText(String cryptText) {
		this.cryptText = cryptText;
	}

	public String getSendKey() {
		return sendKey;
	}

	public void setSendKey(String sendKey) {
		this.sendKey = sendKey;
	}

	public String getRcvKey() {
		return rcvKey;
	}

	public void setRcvKey(String rcvKey) {
		this.rcvKey = rcvKey;
	}

	public String getCryptIv() {
		return cryptIv;
	}

	public void setCryptIv(String cryptIv) {
		this.cryptIv = cryptIv;
	}

	@Override
	public void setIv(String ivB64) {
		setCryptIv(ivB64);
	}

}
