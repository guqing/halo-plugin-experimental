package xyz.guqing.plugin.core.boot;

import javax.sql.DataSource;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import xyz.guqing.plugin.core.SpringBootPlugin;

/**
 * Demonstrate how to share {@link DataSource} from main {@link ApplicationContext},
 * so plugin could use the same database as app and share database connection resource,
 * e.g. connection pool, transaction, etc.
 *
 * <b>Note that related AutoConfigurations have to be excluded explicitly to avoid
 * duplicated resource retaining.</b>
 *
 * @author <a href="https://github.com/hank-cp">Hank CP</a>
 */
public class SharedDataSourceSpringBootstrap extends SpringBootstrap {

    public SharedDataSourceSpringBootstrap(SpringBootPlugin plugin,
                                           Class<?>... primarySources) {
        super(plugin, primarySources);
    }

    @Override
    protected String[] getExcludeConfigurations() {
        return ArrayUtils.addAll(super.getExcludeConfigurations(),
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                "org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration",
                "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration");
    }

    @Override
    public ConfigurableApplicationContext createApplicationContext() {
        AnnotationConfigApplicationContext applicationContext =
                (AnnotationConfigApplicationContext) super.createApplicationContext();
        // share dataSource
        importBeanFromMainContext(applicationContext, DataSource.class);
        importBeanFromMainContext(applicationContext, "transactionManager");
        // share MongoDbFactory
        importBeanFromMainContext(applicationContext, "mongoDbFactory");

        return applicationContext;
    }

}
