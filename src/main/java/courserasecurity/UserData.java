package courserasecurity;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

@NamedQuery(name = UserData.QUERY_FIND_USER_BY_USERNAME, query = "SELECT u FROM UserData u WHERE u.username = ?1 ORDER BY u.username")
@NamedQuery(name = UserData.QUERY_FIND_ALL_USERS_EXCEPT, query = "SELECT u FROM UserData u WHERE u.username <> ?1 ORDER BY u.username")
@NamedQuery(name = UserData.QUERY_FIND_ALL_USERS, query = "SELECT u FROM UserData u ORDER BY u.username")

@Entity
@Table(name = "users")
public class UserData implements Serializable, IVClass {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3288662878952523704L;

	static final String COL_USERNAME = "username";
	static final String COL_PASSWORD = "password";

	static final String QUERY_FIND_USER_BY_USERNAME = "FindUserByUsername";
	public static final String QUERY_FIND_ALL_USERS_EXCEPT = "FindUsersExcept";
	public static final String QUERY_FIND_ALL_USERS = "FindAllUsers";

	@Column(name = COL_USERNAME, unique = true)
	@Id
	private String username;

	@Column(name = COL_PASSWORD)
	private String password;

	@Lob
	@Type(type = "org.hibernate.type.TextType")
	String pubKey;

	@Lob
	@Type(type = "org.hibernate.type.TextType")
	String privKey;

	private String privKeyIv;
	
	
	@Transient
	private String passedPassword;
	
	public UserData() {
	}

	public UserData(ResultSet rs) {
		try {
			username = rs.getString(COL_USERNAME);
		} catch (SQLException e) {
		}
		try {
			password = rs.getString(COL_PASSWORD);
		} catch (SQLException e) {
		}
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}
	
	public String getPassedPassword() {
		return passedPassword;
	}

	public void setPassword(String cryptPass) {
		this.password = cryptPass;
	}
	
	public void setPassedPassword(String password) {
		this.passedPassword = password;
	}

	@Override
	public String toString() {
		return "UserData [username=" + username + ", password=" + password
				+ "]";
	}

	public void setPubKey(String pubKey) {
		this.pubKey = pubKey;
	}

	public String getPubKey() {
		return this.pubKey;
	}

	public String getPrivKeyIv() {
		return privKeyIv;
	}

	public void setPrivKeyIv(String privKeyIv) {
		this.privKeyIv = privKeyIv;
	}

	public String getPrivKey() {
		return privKey;
	}

	public void setPrivKey(String privKey) {
		this.privKey = privKey;
	}

	@Override
	public void setIv(String ivB64) {
		setPrivKeyIv(ivB64);
	}

	
	
}
