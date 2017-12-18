package fr.estela.peerframe.device;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.estela.peerframe.device.util.CORSFilter;

@SpringBootApplication
//@EnableTransactionManagement
@EnableScheduling
@ComponentScan("fr.estela.peerframe.device")
@EnableJpaRepositories("fr.estela.peerframe.device.repository")
public class Application {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);     
    }
    
    public static String getTempFolder() {
        return System.getProperty("user.home").replace("\\", "/") + "/peerframe/tmp/";
    }
    
    public static String getDataFolder() {
        return System.getProperty("user.home").replace("\\", "/") + "/peerframe/data/";
    }
    
    @Autowired
    private Environment env;
    
    @Bean
    public FilterRegistrationBean getSwaggerCORSFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean(new CORSFilter());
        registration.setName("CORSFilter");
        registration.addUrlPatterns("/*");
        return registration;
    }
    
    @Bean
    public Jackson2ObjectMapperBuilder objectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder() {
            @Override
            public void configure(ObjectMapper objectMapper) {
                super.configure(objectMapper);
                objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
                objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
                objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            }
        };
    }
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        final LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan(new String[] { "fr.estela.peerframe.device.entity" });
        final JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", env.getProperty("hibernate.hbm2ddl.auto"));
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.show_sql", env.getProperty("hibernate.show_sql"));
        properties.setProperty("hibernate.format_sql", "true");
        em.setJpaProperties(properties);
        return em;
    }

    @Bean
    public DataSource dataSource(){
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();        
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/peerframe");
        dataSource.setUsername("peerframe");
        dataSource.setPassword("peerframe");
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory emf) {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
	
}