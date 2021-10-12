package courserasecurity;

public class DbRestoreData {
	String userData;
	String messageData;
	
	public String getUserData() {
		return userData;
	}
	public void setUserData(String userData) {
		this.userData = userData;
	}
	public String getMessageData() {
		return messageData;
	}
	public void setMessageData(String messageData) {
		this.messageData = messageData;
	}
	@Override
	public String toString() {
		return "DbRestoreData [userData=" + userData + ", messageData=" + messageData + "]";
	}
	
}
