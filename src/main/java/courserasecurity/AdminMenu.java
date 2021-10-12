package courserasecurity;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes("logged_in_id")
public class AdminMenu {

	@Autowired
	private DataBase dataBase;

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * show admin menu
	 * (select "show users", "drop DB", "download DB"
	 */
	@RequestMapping(value = "/admin_menu", method = RequestMethod.GET)
	public String showAdminMenu(Map<String, Object> model) {
		System.out.println("called: AdminMenu/showAdminMenu : .GET");
		return "admin_menu";
	}

	/**
	 * Show users
	 */
	@RequestMapping(value = "/admin", method = RequestMethod.POST, params = "action=show")
	public String showUsers(Map<String, Object> model) {
		System.out.println("called: AdminMenu/showUsers : .Post");
		try (Connection connection = dataBase.getConnection()) {

			List<UserData> users = entityManager
					.createQuery("Select t from " + UserData.class.getSimpleName() + " t", UserData.class)
					.getResultList();
			ArrayList<UserData> output = new ArrayList<UserData>();

			for (UserData u : users) {
				output.add(u);
			}

			model.put("users", output);
			model.put("adminPage", new String("true"));

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return "show_users";
	}
	
	/**
	 * Restore DB
	@RequestMapping(value = "/admin", method = RequestMethod.POST, params = "action=restore")
	public String showRestore(Map<String, Object> model) {
		model.put("dbData", new DbRestoreData());
		return "dbrestore";
	}
	 */

	
	/**
	 * Drop users database
	@RequestMapping(value = "/admin", method = RequestMethod.POST, params = "action=drop")
	public String dropUsers(Map<String, Object> model) {
		System.out.println("called: AdminMenu/dropUsers : .Post");

		try (Connection connection = dataBase.getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("DELETE FROM users");
			stmt.executeUpdate("DELETE FROM messages");
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return "admin_menu";
	}
	 */

	/**
	 * Database Dump
	 */
	@RequestMapping(value = "/admin", method = RequestMethod.POST, params = "action=download")
	public String downloadDb(Map<String, Object> model) {
		System.out.println("called: AdminMenu/downloadUsers : .Post");

		String dbDump = dataBase.DumpDb();
		model.put("dbDump", dbDump);
		return "dbdump";
	}

	@RequestMapping(value = "/dbdump", method = RequestMethod.GET)
	public String dbDump(Map<String, Object> model) {
		return downloadDb(model);
	}

	/**
	 * Delete a user
	@RequestMapping(value = "/deleteUser", method = RequestMethod.GET)
	@Transactional
	public String deleteUser(@ModelAttribute UserData userData, Map<String, Object> model) {
		System.out.println("Delete: " + userData);

		TypedQuery<UserData> queryUser = entityManager.createNamedQuery(UserData.QUERY_FIND_USER_BY_USERNAME,
				UserData.class);
		queryUser.setParameter(1, userData.getUsername());

		List<UserData> res = queryUser.getResultList();
		if (res != null && res.size() == 1) {
			UserData u = res.get(0);
			entityManager.remove(u);
		}

		return showUsers(model);
	}
	 */

	/**
	 * import data into database
	@RequestMapping(value = "/restore_database", method = RequestMethod.POST)
	@Transactional
	public String restoreDB(@ModelAttribute(name = "dbData") DbRestoreData dbData,
							Map<String, Object> model) {
		System.out.println("Restore Data: " + dbData);
		System.out.println("Restore Data: " + model.get("dbData"));


		dataBase.RestoreDb(dbData.getUserData(), dbData.getMessageData());
		
		String dbDump = dataBase.DumpDb();
		model.put("dbDump", dbDump);
		return "dbdump";
	}
	 */

}
