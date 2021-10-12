package courserasecurity;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.EnglishSequenceData;
import org.passay.IllegalSequenceRule;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@Scope("session")
@SessionAttributes({"logged_in_id", "logged_in_password"})
public class LoginController {

	@PersistenceContext
	private EntityManager entityManager;


	@Autowired
	MessageCenter messageCenter;

	/**
	 * show user login page
	 */
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String loginStart(@ModelAttribute UserData userData, Map<String, Object> model) {
		System.out.println("called: LoginController/loginStart : .GET");
		List<UserData> users = entityManager.createNamedQuery(UserData.QUERY_FIND_ALL_USERS, UserData.class).getResultList();
		model.put("users", users);
		return "login";
	}
	
	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public String logout(HttpServletRequest request, @ModelAttribute UserData userData, Map<String, Object> model) {
		System.out.println("called: LoginController/logout : .GET");
		System.out.println("session: " + request);
		try {
		    HttpSession session = request.getSession(false);
		    session.invalidate();
			request.logout();
		} catch (ServletException e) {
			e.printStackTrace();
		}
		model.clear();
		model.put("userData", new UserData());
		return loginStart(userData, model);
	}


	/**
	 * receive user data when registering
	 */
	@RequestMapping(value = "/login", method = RequestMethod.POST, params = "action=Register")
	@Transactional
	public String registerNewUser(UserData userData, ModelMap model) {
		System.out.println("called: LoginController/registerNewUser : .POST");
		model.addAttribute("username: ", userData.getUsername());
		model.addAttribute("password: ", userData.getPassedPassword());

		TypedQuery<UserData> queryUser = entityManager.createNamedQuery(UserData.QUERY_FIND_USER_BY_USERNAME,
				UserData.class);
		queryUser.setParameter(1, userData.getUsername());
		List<UserData> res = queryUser.getResultList();
		if(res.size() > 0) {
			model.put("error_message", "Registration failed: Username \"" + userData.getUsername() + "\" already exists!");
			return loginStart(userData, model);
		}

		RuleResult validate = checkPasswordStrength(userData);
		if(!validate.isValid()) {
			
			String passwordPolicy = "Password does not match the password policy!";
			model.put("error_message", passwordPolicy);
			model.put("password_policy_error", true);
			return loginStart(userData, model);			
		}
		
		
		
		String pwd = userData.getPassedPassword();
		byte[] cryptPass = PasswordHelper.encryptPassword(pwd);
		byte[] salt = Arrays.copyOf(cryptPass, 16);
		String storePass = Base64.getEncoder().encodeToString(cryptPass);
		userData.setPassword(storePass);
		SecretKey sk = Crypto.buildAESKeyFromPassword(pwd, salt);
		Crypto.GenerateKeyPair(userData, userData.getPassedPassword(), sk);
		try {
			entityManager.persist(userData);
			return loginStart(userData, model);	
		} catch(Exception e)
		{
			model.put("error_message", "Registration failed!");
			return loginStart(userData, model);
		}
	}

	/**
	 * check if password conforms the criteria
	 */
	private RuleResult checkPasswordStrength(UserData userData) {
		PasswordValidator validator = new PasswordValidator(
				  // min. 8 characters
				  new LengthRule(8,255),

				  // at least one upper-case character
				  new CharacterRule(EnglishCharacterData.UpperCase, 1),

				  // at least one lower-case character
				  new CharacterRule(EnglishCharacterData.LowerCase, 1),

				  // at least one digit character
				  new CharacterRule(EnglishCharacterData.Digit, 1),

				  // at least one symbol (special character)
				  new CharacterRule(EnglishCharacterData.Special, 1),

				  // define some illegal sequences that will fail when >= 5 chars long
				  // alphabetical is of the form 'abcde', numerical is '34567', qwery is 'asdfg'
				  // the false parameter indicates that wrapped sequences are allowed; e.g. 'xyzabc'
				  new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 5, false),
				  new IllegalSequenceRule(EnglishSequenceData.Numerical, 5, false),
				  new IllegalSequenceRule(EnglishSequenceData.USQwerty, 5, false),

				  // no whitespace
				  new WhitespaceRule());

		PasswordValidator passwordValidator = new PasswordValidator(validator);
		PasswordData passwordData = new PasswordData(userData.getPassedPassword());
		RuleResult validate = passwordValidator.validate(passwordData);
		System.out.println(validate.getDetails());
		return validate;
	}
	
	/**
	 * user login
	 */
	@RequestMapping(value = "/login", method = RequestMethod.POST, params = "action=Login")
	@Transactional
	public String loginUser(@ModelAttribute UserData userData, @ModelAttribute("logged_in_id") String loggedInUserId,
			Map<String, Object> model) {
		System.out.println("called: LoginController/loginUser : .POST");
		System.out.println("user  : " + userData.getUsername());
//		System.out.println("passwd: " + userData.getPassedPassword());

		System.out.println("loggedInUserId: " + loggedInUserId);

		TypedQuery<UserData> queryUser = entityManager.createNamedQuery(UserData.QUERY_FIND_USER_BY_USERNAME,
				UserData.class);
		queryUser.setParameter(1, userData.getUsername());
		List<UserData> res = queryUser.getResultList();
		if (res != null && res.size() == 1) {
			UserData u = res.get(0);
			String provided = userData.getPassedPassword();
			String stored = u.getPassword();
			boolean match = PasswordHelper.checkPassword(stored, provided);

			if (match) {
				loggedInUserId = u.getUsername();
				model.put("logged_in_id", loggedInUserId);
				model.put("logged_in_password", userData.getPassedPassword());
				return messageCenter.MessageMenu(model, loggedInUserId);
			}
			model.put("error_message", "Login failed: Invalid Password!");
			return loginStart(new UserData(), model);

		}
		model.put("error_message", "Login failed: User \"" + userData.getUsername() + "\" not found!");
		return loginStart(new UserData(), model);
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
