package courserasecurity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableJpaRepositories(basePackages = "courserasecurity", entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "jpaTransactionManager")
@PropertySource(value = { "classpath:application.properties" })
@EnableTransactionManagement
public class DataBase {

	private static final String[] ENTITYMANAGER_PACKAGES_TO_SCAN = { "courserasecurity" };

	@Autowired
	private Environment env;

	@Value("${spring.datasource.url}")
	private String dbUrl;

	@Bean
	public DataSource dataSource() {
		if (dbUrl == null || dbUrl.isEmpty()) {
			return new HikariDataSource();
		} else {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(dbUrl);
			return new HikariDataSource(config);
		}
	}

	@Bean
	public JpaTransactionManager jpaTransactionManager() {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
		return transactionManager;
	}

	// adding for future use
	private HibernateJpaVendorAdapter vendorAdaptor() {
		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		return vendorAdapter;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setJpaVendorAdapter(vendorAdaptor());
		entityManagerFactoryBean.setDataSource(dataSource());
		entityManagerFactoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
		entityManagerFactoryBean.setPackagesToScan(ENTITYMANAGER_PACKAGES_TO_SCAN);
		entityManagerFactoryBean.setJpaProperties(addProperties());

		return entityManagerFactoryBean;
	}

	private Properties addProperties() {
		Properties properties = new Properties();
//		properties.setProperty("hibernate.dialect", env.getProperty("spring.jpa.database-platform"));
//		properties.setProperty("hibernate.format_sql", env.getProperty("spring.jpa.properties.hibernate.format_sql"));
		properties.setProperty("hibernate.show_sql", env.getProperty("spring.jpa.show-sql"));
		properties.setProperty("spring.jpa.generate-ddl", "true");
		properties.setProperty("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto"));
		return properties;
	}

	@Autowired
	@Lazy
	private DataSource dataSource;

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	
	@Autowired
	@Bean(name = "hibernate5AnnotatedSessionFactory")
	LocalSessionFactoryBuilder getLocalSessionFactoryBean() {
		LocalSessionFactoryBuilder localSessionFactoryBean = new LocalSessionFactoryBuilder(dataSource());
		localSessionFactoryBean.scanPackages(ENTITYMANAGER_PACKAGES_TO_SCAN);
		localSessionFactoryBean.addProperties(addProperties());
		localSessionFactoryBean.buildSessionFactory();

		return localSessionFactoryBean;
	}

	/**
	 * print the DB dump
	 * @return 
	 */
	public String DumpDb() {
		try {
			Connection conn = DriverManager.getConnection(dbUrl);

			OutputStream os = new ByteArrayOutputStream();
			CopyManager copy = ((PGConnection) conn).getCopyAPI();
			
			String header = "Table Users:\n\n";
			os.write(header.getBytes(Charset.forName("UTF-8")));
			
			copy.copyOut("COPY users to STDOUT (FORMAT CSV, HEADER)", os);
			os.write("\\.\n".getBytes(Charset.forName("UTF-8")));			
			header = "\n\nTable Messages:\n\n";
			os.write(header.getBytes(Charset.forName("UTF-8")));

			copy.copyOut("COPY messages to STDOUT (FORMAT CSV, HEADER)", os);
			os.write("\\.\n".getBytes(Charset.forName("UTF-8")));
			os.write("\n".getBytes(Charset.forName("UTF-8")));
			return os.toString();
		} catch (SQLException e) {
			return e.getMessage();
		} catch (IOException e) {
			return e.getMessage();
		}
	}

	public void RestoreDb(String userData, String messageData) {
		if(userData == null || messageData == null) {
			System.out.println("cannot restore DB: userdata:" + userData);
			System.out.println("cannot restore DB: msgdata:" + messageData);
			return;
		}
		try {
			Connection conn = DriverManager.getConnection(dbUrl);
	        CopyManager cpManager = ((PGConnection) conn).getCopyAPI();
	        InputStream is = new ByteArrayInputStream(userData.getBytes());
	        cpManager.copyIn("COPY users FROM STDIN (FORMAT CSV, HEADER)", is);

	        is = new ByteArrayInputStream(messageData.getBytes());
	        cpManager.copyIn("COPY messages FROM STDIN (FORMAT CSV, HEADER)", is);
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	}
	
}
