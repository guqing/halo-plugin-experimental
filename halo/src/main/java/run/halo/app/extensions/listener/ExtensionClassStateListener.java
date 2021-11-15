package run.halo.app.extensions.listener;

import java.util.List;
import javax.sql.DataSource;
import org.pf4j.PluginWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import run.halo.app.extensions.registry.ExtensionClassRegistry;
import run.halo.app.extensions.internal.ExtensionInjectedEvent;
import run.halo.app.extensions.registry.JpaRepositoryFactoryRegistry;

/**
 * @author guqing
 * @since 2021-11-15
 */
@Component
public class ExtensionClassStateListener implements ApplicationContextAware {

    private final ExtensionClassRegistry classRegistry = ExtensionClassRegistry.getInstance();
    private final JpaRepositoryFactoryRegistry jpaRepositoryFactoryRegistry;
    private ApplicationContext applicationContext;

    public ExtensionClassStateListener(DataSource dataSource) {
        this.jpaRepositoryFactoryRegistry = JpaRepositoryFactoryRegistry.getInstance();
        this.jpaRepositoryFactoryRegistry.setDataSource(dataSource);
    }

    @EventListener(ExtensionInjectedEvent.class)
    public void extensionInjectedListener(ExtensionInjectedEvent event) {
        // 注册repository bean
        PluginWrapper pluginWrapper = event.getPluginWrapper();
        String pluginId = pluginWrapper.getPluginId();

        jpaRepositoryFactoryRegistry.createJpaRepositoryFactory(pluginId,
            pluginWrapper.getPluginClassLoader());

        JpaRepositoryFactory jpaRepositoryFactory =
            jpaRepositoryFactoryRegistry.getJpaRepositoryFactory(pluginId);
        List<Class<?>> repositoryClasses =
            classRegistry.findClassesWithAnnotation(pluginId, Repository.class);
        DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        for (Class<?> repositoryClass : repositoryClasses) {
            Object repository = jpaRepositoryFactory.getRepository(repositoryClass);
            beanFactory.registerSingleton(repositoryClass.getName(), repository);
        }
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }
}
