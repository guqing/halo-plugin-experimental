package run.halo.app.extensions.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Environment;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * @author guqing
 * @since 2021-11-13
 */
public class JpaRepositoryFactoryRegistry {

    private static final JpaRepositoryFactoryRegistry INSTANCE = new JpaRepositoryFactoryRegistry();
    private static final Map<String, JpaRepositoryFactory> repositoryFactoryMap =
        new ConcurrentHashMap<>();
    private final ExtensionClassRegistry classRegistry = ExtensionClassRegistry.getInstance();

    private DataSource dataSource;

    public static JpaRepositoryFactoryRegistry getInstance() {
        return INSTANCE;
    }

    private JpaRepositoryFactoryRegistry() {
    }

    public void createJpaRepositoryFactory(String pluginId, ClassLoader classLoader) {
        if (repositoryFactoryMap.containsKey(pluginId)) {
            return;
        }

        List<Class<?>> entityClasses = findEntityClass(pluginId);
        ClassLoaderServiceImpl classLoaderService = new ClassLoaderServiceImpl(classLoader);
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySettings(resolveAdditionalProperties(pluginId))
            .addService(ClassLoaderService.class, classLoaderService)
            .build();
        MetadataSources metadataSources = new MetadataSources(registry);
        for (Class<?> entityClass : entityClasses) {
            metadataSources.addAnnotatedClass(entityClass);
        }

        Metadata metadata = metadataSources.buildMetadata();

        SessionFactory sessionFactory = metadata.buildSessionFactory();
        EntityManager entityManager = sessionFactory.createEntityManager();

        JpaRepositoryFactory repositoryFactory = new JpaRepositoryFactory(entityManager);
        repositoryFactory.setBeanClassLoader(classLoader);

        repositoryFactoryMap.put(pluginId, repositoryFactory);
    }

    public JpaRepositoryFactory getJpaRepositoryFactory(String pluginId) {
        return repositoryFactoryMap.get(pluginId);
    }

    public List<Class<?>> findEntityClass(String pluginId) {
        return classRegistry.findClassesWithAnnotation(pluginId, Entity.class);
    }

    public Map<String, Object> resolveAdditionalProperties(String pluginId) {
        if (dataSource == null) {
            throw new IllegalArgumentException("The plugin dataSource is not assigned.");
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(Environment.DATASOURCE, dataSource);
        properties.put(Environment.HBM2DDL_AUTO, "update");
        properties.put(Environment.SHOW_SQL, true);
        return properties;
    }

    public void setDataSource(@NonNull DataSource dataSource) {
        Assert.notNull(dataSource, "The dataSource must not be null.");
        this.dataSource = dataSource;
    }
}
