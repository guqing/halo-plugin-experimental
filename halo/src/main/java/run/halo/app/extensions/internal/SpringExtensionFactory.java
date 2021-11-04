package run.halo.app.extensions.internal;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.pf4j.Extension;
import org.pf4j.ExtensionFactory;
import org.pf4j.Plugin;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import run.halo.app.extensions.SpringPlugin;
import run.halo.app.extensions.SpringPluginManager;

/**
 * Basic implementation of an extension factory.
 * <p><p>
 * Uses Springs {@link AutowireCapableBeanFactory} to instantiate a given extension class. All kinds
 * of {@link Autowired} are supported (see example below). If no {@link ApplicationContext} is
 * available (this is the case if either the related plugin is not a {@link SpringPlugin} or the
 * given plugin manager is not a {@link SpringPluginManager}), standard Java reflection will be used
 * to instantiate an extension.
 * <p><p>
 * Creates a new extension instance every time a request is done.
 * <p><p>
 * Example of supported autowire modes:
 * <pre>{@code
 *     @Extension
 *     public class Foo implements ExtensionPoint {
 *
 *         private final Bar bar;       // Constructor injection
 *         private Baz baz;             // Setter injection
 *         @Autowired
 *         private Qux qux;             // Field injection
 *
 *         @Autowired
 *         public Foo(final Bar bar) {
 *             this.bar = bar;
 *         }
 *
 *         @Autowired
 *         public void setBaz(final Baz baz) {
 *             this.baz = baz;
 *         }
 *     }
 * }</pre>
 *
 * @author guqing
 * @date 2021-11-02
 * @see <a href="https://github.com/pf4j/pf4j-spring">Referenced pf4j-spring</a>
 */
public class SpringExtensionFactory implements ExtensionFactory {

    public static final boolean AUTOWIRE_BY_DEFAULT = true;
    private static final Logger log = LoggerFactory.getLogger(SpringExtensionFactory.class);
    /**
     * The plugin manager is used for retrieving a plugin from a given extension class and as a
     * fallback supplier of an application context.
     */
    protected final PluginManager pluginManager;

    /**
     * Indicates if springs autowiring possibilities should be used.
     */
    protected final boolean autowire;

    public SpringExtensionFactory(PluginManager pluginManager) {
        this(pluginManager, AUTOWIRE_BY_DEFAULT);
    }

    public SpringExtensionFactory(final PluginManager pluginManager, final boolean autowire) {
        this.pluginManager = pluginManager;
        this.autowire = autowire;
        if (!autowire) {
            log.warn(
                "Autowiring is disabled although the only reason for existence of this special factory is"
                    +
                    " supporting spring and its application context.");
        }
    }

    @Override
    @Nullable
    public <T> T create(Class<T> extensionClass) {
        if (!this.autowire) {
            log.warn("Create instance of '" + nameOf(extensionClass)
                + "' without using springs possibilities as" +
                " autowiring is disabled.");
            return createWithoutSpring(extensionClass);
        }
        Optional<ApplicationContext> contextOptional = getApplicationContextBy(extensionClass);
        if (contextOptional.isPresent()) {
            createWithSpring(extensionClass, contextOptional.get());
            return null;
        }
        return createWithoutSpring(extensionClass);
    }

    public String getExtensionBeanName(Class<?> extensionClass) {
        return getApplicationContextBy(extensionClass)
            .map(pluginAppCtx -> pluginAppCtx.getBeanNamesForType(extensionClass))
            .filter(beanNames -> beanNames.length > 0)
            .map(beanNames -> beanNames[0])
            .orElse(null);
    }

    /**
     * Creates a BeanDefinition of the given {@code extensionClass} by using the {@link
     * AutowireCapableBeanFactory} of the given {@code applicationContext}. It should be noted that
     * this method cannot return an accurate instance, because there is a dependent injection class,
     * which can only be registered into the Factory and unable to complete. When all dependence is
     * registered then all kinds of autowiring are applied:
     * <ol>
     *     <li>Constructor injection</li>
     *     <li>Setter injection</li>
     *     <li>Field injection</li>
     * </ol>
     *
     * @param extensionClass     The class annotated with {@code @}{@link Extension}.
     * @param <T>                The type for that an instance should be created.
     * @param applicationContext The context to use for autowiring.
     */
    protected <T> void createWithSpring(final Class<T> extensionClass,
        final ApplicationContext applicationContext) {
        final DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        BeanDefinitionBuilder beanDefinitionBuilder =
            BeanDefinitionBuilder.genericBeanDefinition(extensionClass);
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
        beanDefinition.setScope(SCOPE_SINGLETON);
        beanFactory.registerBeanDefinition(extensionClass.getName(), beanDefinition);
    }

    /**
     * Creates an instance of the given class object by using standard Java reflection.
     *
     * @param extensionClass The class annotated with {@code @}{@link Extension}.
     * @param <T>            The type for that an instance should be created.
     * @return an instantiated extension.
     * @throws IllegalArgumentException if the given class object has no public constructor.
     * @throws RuntimeException         if the called constructor cannot be instantiated with {@code
     *                                  null}-parameters.
     */
    @SuppressWarnings("unchecked")
    protected <T> T createWithoutSpring(final Class<T> extensionClass)
        throws IllegalArgumentException {
        final Constructor<?> constructor =
            getPublicConstructorWithShortestParameterList(extensionClass)
                // An extension class is required to have at least one public constructor.
                .orElseThrow(
                    () -> new IllegalArgumentException("Extension class '" + nameOf(extensionClass)
                        + "' must have at least one public constructor."));
        try {
            log.debug("Instantiate '" + nameOf(extensionClass) + "' by calling '" + constructor
                + "'with standard Java reflection.");
            // Creating the instance by calling the constructor with null-parameters (if there are any).
            return (T) constructor.newInstance(nullParameters(constructor));
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            // If one of these exceptions is thrown it it most likely because of NPE inside the called constructor and
            // not the reflective call itself as we precisely searched for a fitting constructor.
            log.error(ex.getMessage(), ex);
            throw new RuntimeException(
                "Most likely this exception is thrown because the called constructor ("
                    + constructor + ")" +
                    " cannot handle 'null' parameters. Original message was: "
                    + ex.getMessage(), ex);
        }
    }

    private Optional<Constructor<?>> getPublicConstructorWithShortestParameterList(
        final Class<?> extensionClass) {
        return Stream.of(extensionClass.getConstructors())
            .min(Comparator.comparing(Constructor::getParameterCount));
    }

    private Object[] nullParameters(final Constructor<?> constructor) {
        return new Object[constructor.getParameterCount()];
    }

    protected <T> Optional<ApplicationContext> getApplicationContextBy(
        final Class<T> extensionClass) {
        final Plugin plugin = Optional.ofNullable(this.pluginManager.whichPlugin(extensionClass))
            .map(PluginWrapper::getPlugin)
            .orElse(null);

        final ApplicationContext applicationContext;

        if (plugin instanceof SpringPlugin) {
            log.debug(
                "  Extension class ' " + nameOf(extensionClass) + "' belongs to spring-plugin '"
                    + nameOf(plugin)
                    + "' and will be autowired by using its application context.");
            applicationContext = ((SpringPlugin) plugin).getApplicationContext();
        } else if (this.pluginManager instanceof SpringPluginManager) {
            log.debug("  Extension class ' " + nameOf(extensionClass)
                + "' belongs to a non spring-plugin (or main application)" +
                " '" + nameOf(plugin)
                + ", but the used PF4J plugin-manager is a spring-plugin-manager. Therefore" +
                " the extension class will be autowired by using the managers application contexts");
            applicationContext = ((SpringPluginManager) this.pluginManager).getApplicationContext();
        } else {
            log.warn("  No application contexts can be used for instantiating extension class '"
                + nameOf(extensionClass) + "'."
                + " This extension neither belongs to a PF4J spring-plugin (id: '" + nameOf(plugin)
                + "') nor is the used" +
                " plugin manager a spring-plugin-manager (used manager: '" + nameOf(
                this.pluginManager.getClass()) + "')." +
                " At perspective of PF4J this seems highly uncommon in combination with a factory which only reason for existence"
                +
                " is using spring (and its application context) and should at least be reviewed. In fact no autowiring can be"
                +
                " applied although autowire flag was set to 'true'. Instantiating will fallback to standard Java reflection.");
            applicationContext = null;
        }

        return Optional.ofNullable(applicationContext);
    }

    private String nameOf(final Plugin plugin) {
        return Objects.nonNull(plugin)
            ? plugin.getWrapper().getPluginId()
            : "system";
    }

    private <T> String nameOf(final Class<T> clazz) {
        return clazz.getName();
    }

}
