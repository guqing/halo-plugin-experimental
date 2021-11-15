package run.halo.app;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.Test;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;
import run.halo.app.entity.Potato;
import run.halo.app.reposiory.CustomerRepository;
import run.halo.app.reposiory.PotatoRepository;

/**
 * @author guqing
 * @since 2021-11-10
 */
public class HelloTest {

    @Test
    public void test() throws ClassNotFoundException {

        //System.setProperty("pf4j.mode", RuntimeMode.DEPLOYMENT.toString());
        System.setProperty("pf4j.pluginsDir", "/home/guqing/halo-dev/plugins/");
        final PluginManager pluginManager = new DefaultPluginManager();
        // load the plugins
        pluginManager.loadPlugins();

        // start (active/resolved) the plugins
        pluginManager.startPlugin("potatoes");

        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
        driverManagerDataSource.setDriverClassName("org.h2.Driver");
        driverManagerDataSource.setUrl("jdbc:mysql://172.56.75.1:3307/business_flow_test");
        driverManagerDataSource.setUsername("root");
        driverManagerDataSource.setPassword("Idea@1234");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setPersistenceUnitName("potatoes");
        em.setDataSource(driverManagerDataSource);
        em.setPackagesToScan("run.halo.app.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.afterPropertiesSet();
        EntityManager entityManager = em.getObject().createEntityManager();
        System.out.println(entityManager.getMetamodel());
        try {
            Class<?> potatoes = pluginManager.getPluginClassLoader("potatoes")
                .loadClass("run.halo.app.reposiory.PotatoRepository");
            RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
            System.out.println(factory.getRepository(potatoes));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test2() throws ClassNotFoundException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://172.56.75.1:3307/business_flow_test");
        dataSource.setUsername("root");
        dataSource.setPassword("Idea@1234");
//
//        DataSourceTransactionManager transactionManager =
//            new DataSourceTransactionManager(dataSource);

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting(Environment.DATASOURCE, dataSource)
            .applySetting("hibernate.hbm2ddl.auto", "update")
            .applySetting("hibernate.show_sql", "true")
            .build();
        MetadataSources metadataSources = new MetadataSources(registry);
        metadataSources.addAnnotatedClass(Potato.class);
        Metadata metadata = metadataSources.buildMetadata();

        SessionFactory sessionFactory = metadata.buildSessionFactory();
        EntityManager entityManager = sessionFactory.createEntityManager();

        Class<PotatoRepository> repositoryClass = (Class<PotatoRepository>)getClass().getClassLoader()
            .loadClass("run.halo.app.reposiory.PotatoRepository");

        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        Potato potatoToUse = new Potato();
        potatoToUse.setId(1);
        session.update(potatoToUse);
        transaction.commit();
        session.close();
//        RepositoryFactorySupport factory = new JpaRepositoryFactory(entityManager);
//        factory.setRepositoryBaseClass(CustomerRepository.class);
//        try {
//            Potato potato = new Potato();
//            potato.setId(1);
//            PotatoRepository potatoRepository = factory.getRepository(repositoryClass);
//            Potato save = potatoRepository.saveAndFlush(potato);
//            System.out.println(save);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        sessionFactory.close();
        StandardServiceRegistryBuilder.destroy(registry);
    }
}
