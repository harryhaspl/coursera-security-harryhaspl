package courserasecurity;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import org.springframework.context.annotation.Lazy;

@Controller
@SpringBootApplication
@EnableAutoConfiguration
@SessionAttributes("logged_in_id")
public class MyMainClass {

	@Autowired
	@Lazy
	LoginController loginController;
	
	@RequestMapping("/")
	String index(@ModelAttribute UserData userData, Map<String, Object> model) {
		return indexHtml(userData, model);
	}
	@RequestMapping("/index.html")
	String indexHtml(@ModelAttribute UserData userData, Map<String, Object> model) {
		return loginController.loginStart(userData, model);
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(MyMainClass.class, args);
	}

}
