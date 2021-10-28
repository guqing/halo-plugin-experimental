package xyz.guqing.plugin.core.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Get {@link ApplicationContext} in static way. Since {@link ApplicationContext}
 * is bound to static class in this way, Plugin shouldn't use it directly, but
 * need to inherit it as a new privite class and register it in its own configuration.
 *
 * To register main {@link ApplicationContext}:
 * ```
 * @Bean
 * public ApplicationContextAware multiApplicationContextProviderRegister() {
 *     return ApplicationContextProvider::registerApplicationContext;
 * }
 * ```
 * @author <a href="https://github.com/hank-cp">Hank CP</a>
 */
public class ApplicationContextProvider {

    private static Map<ClassLoader, ApplicationContext> ctxCache = Collections.synchronizedMap(new HashMap<>());

    public static void registerApplicationContext(ApplicationContext ctx) {
        ctxCache.put(ctx.getClassLoader(), ctx);
    }

    public static void unregisterApplicationContext(ApplicationContext ctx) {
        ctxCache.remove(ctx.getClassLoader());
    }

    public static ApplicationContext getApplicationContext(Object probe) {
        return getApplicationContext(probe.getClass().getClassLoader());
    }

    public static ApplicationContext getApplicationContext(Class<?> probeClazz) {
        return getApplicationContext(probeClazz.getClassLoader());
    }

    public static ApplicationContext getApplicationContext(ClassLoader classLoader) {
        return ctxCache.get(classLoader);
    }

    public static <T> T getBean(Class<T> clazz) {
        return getBean(clazz.getClassLoader(), clazz);
    }

    public static <T> T getBean(Class<?> probeClazz, Class<T> clazz) {
        return getBean(probeClazz.getClassLoader(), clazz);
    }

    public static <T> T getBean(ClassLoader classLoader, Class<T> clazz) {
        ApplicationContext ctx = getApplicationContext(classLoader);
        if (ctx == null) return null;
        try {
            return ctx.getBean(clazz);
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }

    public static <T> T getBean(Class<?> probeClazz, String beanName) {
        return getBean(probeClazz.getClassLoader(), beanName);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(ClassLoader classLoader, String beanName) {
        ApplicationContext ctx = getApplicationContext(classLoader);
        if (ctx == null) return null;
        try {
            return (T) ctx.getBean(beanName);
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }

    public static String getMessage(Class<?> probeClazz, String msgKey, Object...params) {
        return getMessage(probeClazz.getClassLoader(), msgKey, params);
    }

    public static String getMessage(ClassLoader classLoader, String msgKey, Object...params) {
        assert ctxCache.containsKey(classLoader);
        try {
            return Optional.ofNullable(getBean(classLoader, MessageSource.class)).map(bean -> bean.getMessage(
                msgKey, params, LocaleContextHolder.getLocale())).orElse(msgKey);
        } catch (NoSuchMessageException ignored) {
            return msgKey;
        }
    }

}