package courserasecurity;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@Scope("session")
@SessionAttributes({"logged_in_id", "logged_in_password"})
public class MessageCenter {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	@Lazy
	LoginController loginController;
	
	/**
	 * Message main screen
	 */
	@RequestMapping(value = "/messages", method = RequestMethod.GET)
	String MessageMenu(Map<String, Object> model, @ModelAttribute("logged_in_id") String loggedInUserId) {
		String userId = (String) model.get("logged_in_id");
		if(userId == null) {
			model.put("userData", new UserData());
			return loginController.loginStart(new UserData(), model);
		}
		
		TypedQuery<UserData> queryMe = entityManager.createNamedQuery(UserData.QUERY_FIND_USER_BY_USERNAME, UserData.class);
		queryMe.setParameter(1, model.get("logged_in_id"));
		UserData me = queryMe.getResultList().get(0);

		TypedQuery<UserData> queryUser = entityManager.createNamedQuery(UserData.QUERY_FIND_ALL_USERS_EXCEPT, UserData.class);
		queryUser.setParameter(1, userId);

		TypedQuery<Message> rcvMsgQ = entityManager.createNamedQuery(Message.QUERY_FIND_MSG_FOR_USER, Message.class);
		rcvMsgQ.setParameter(1, userId);

		TypedQuery<Message> sendMsgQ = entityManager.createNamedQuery(Message.QUERY_FIND_MSG_FROM_USER, Message.class);
		sendMsgQ.setParameter(1, userId);

		//		TypedQuery<UserData> queryUser = entityManager.createNamedQuery(UserData.QUERY_FIND_ALL_VERIFIED_USERS,
//				UserData.class);
		List<UserData> res = queryUser.getResultList();

		List<Message> rcvMsg = rcvMsgQ.getResultList();
		
		List<Message> sentMsg = sendMsgQ.getResultList();
		
		/*
		System.out.println("possible recipients:");
		for (UserData r : res) {
			System.out.println(r.getUsername());
		}
		*/
		
		String userPassword = (String) model.get("logged_in_password");
		String myPrivKey = me.getPrivKey();
		String myPrivKeyIv = me.getPrivKeyIv();
		
		byte[] salt = PasswordHelper.getSaltFromPassString(me.getPassword());
		
		SecretKey skToGetPrivKey = Crypto.buildAESKeyFromPassword(userPassword, salt);
		System.out.println("logged in password: " + userPassword);
		String privKeyB64 = Crypto.decrypt(myPrivKey, skToGetPrivKey, Crypto.rebuildIv(myPrivKeyIv));
		
		System.out.println("sent messages:");
		for (Message m : sentMsg) {
			System.out.println(m.toString());
			IvParameterSpec msgIv = Crypto.rebuildIv(m.getCryptIv());
			String cypherTextB64 = m.getCryptText();
			String senderKeyB64 = m.getSendKey();
			
			String plainMessage = decodeMessage(privKeyB64, msgIv, cypherTextB64, senderKeyB64);
			m.setText(plainMessage);
		}
		
		System.out.println("rcv messages:");
		for (Message m : rcvMsg) {
			System.out.println(m.toString());
			IvParameterSpec msgIv = Crypto.rebuildIv(m.getCryptIv());
			String cypherTextB64 = m.getCryptText();
			String receiverKeyB64 = m.getRcvKey();
			
			String plainMessage = decodeMessage(privKeyB64, msgIv, cypherTextB64, receiverKeyB64);
			m.setText(plainMessage);
		}

		
		model.put("recipients", res);
		model.put("message", new Message());
		
		model.put("sent_messages", sentMsg);
		model.put("received_messages", rcvMsg);
		return "message_center";
	}

	private String decodeMessage(String privKeyB64, IvParameterSpec msgIv, String cypherTextB64, String senderKeyB64) {
		String secretKeyForMessage = Crypto.DecryptPk(senderKeyB64, privKeyB64);
		byte[] secretKeyBytes = Base64.getDecoder().decode(secretKeyForMessage);
		SecretKey key = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, "AES");
		String decodedMessage = Crypto.decrypt(cypherTextB64, key, msgIv);
		String txt = new String(Base64.getDecoder().decode(decodedMessage));
		System.out.println("decoded message: " + txt);
		return txt;
	}

	/**
	 * send a message
	 */
	@RequestMapping(value = "/send_message", method = RequestMethod.POST)
	@Transactional
	public String sendMesage(@ModelAttribute Message messageData, Map<String, Object> model,
			@ModelAttribute("logged_in_id") String loggedInUserId) {
		System.out.println("Sending Message - logged_in = " + loggedInUserId);

//		System.out.println(messageData.toString());
		messageData.setSender(loggedInUserId);

		TypedQuery<UserData> querySender = entityManager.createNamedQuery(UserData.QUERY_FIND_USER_BY_USERNAME, UserData.class);
		querySender.setParameter(1, loggedInUserId);
		List<UserData> sender = querySender.getResultList();

		TypedQuery<UserData> queryRcv = entityManager.createNamedQuery(UserData.QUERY_FIND_USER_BY_USERNAME, UserData.class);
		queryRcv.setParameter(1, messageData.getReceiver());
		List<UserData> rcv = queryRcv.getResultList();
		if(sender.size() == 1 && rcv.size() == 1)
		{
			// Sender's and Receiver's public key
			UserData theSender = sender.get(0);
			String senderPubKeyB64 = theSender.getPubKey();
			UserData theReceiver = rcv.get(0);
			String rcvPubKeyB64 = theReceiver.getPubKey();
			
			// Secret key to encrypt the message
			SecretKey msgSecretKey = Crypto.generateAESKey();
			String senderKey = Crypto.encryptRSA(msgSecretKey.getEncoded(), senderPubKeyB64);
			messageData.setSendKey(senderKey);
			String receiverKey = Crypto.encryptRSA(msgSecretKey.getEncoded(), rcvPubKeyB64);
			messageData.setRcvKey(receiverKey);

			IvParameterSpec iv = Crypto.generateAndStoreIv(messageData);
			String cipherText = Crypto.encryptAES(messageData.getText().getBytes(), msgSecretKey, iv);
			messageData.setCryptText(cipherText);
			entityManager.persist(messageData);
		}

		return MessageMenu(model, loggedInUserId);
	}

	@ModelAttribute("logged_in_id")
	public String loggedInUserId() {
		return null;
	}
	@ModelAttribute("logged_in_password")
	public String loggedInPassword() {
		return null;
	}

}
