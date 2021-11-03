package run.halo.app.extensions.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import run.halo.app.extensions.ExtensionsInjector;
import run.halo.app.extensions.ExtController;
import run.halo.app.extensions.ExtRestController;
import run.halo.app.extensions.SpringPlugin;

/**
 * Plugin mapping manager
 *
 * @author guqing
 * @see RequestMappingHandlerMapping
 */
@Slf4j
public class PluginRequestMappingManager {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public PluginRequestMappingManager(
        RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    public void registerControllers(SpringPlugin springBootPlugin) {
        getControllerBeans(springBootPlugin).forEach(this::registerController);
    }

    private void registerController(Object controller) {
        Method detectHandlerMethods =
            ReflectionUtils.findMethod(RequestMappingHandlerMapping.class, "detectHandlerMethods",
                Object.class);
        if (detectHandlerMethods == null) {
            return;
        }
        try {
            detectHandlerMethods.setAccessible(true);
            detectHandlerMethods.invoke(requestMappingHandlerMapping, controller);
        } catch (IllegalStateException ise) {
            // ignore this
            log.warn(ise.getMessage());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            log.warn("invocation target exception: [{}]", e.getMessage());
        }
    }

    public void unregisterControllers(SpringPlugin springBootPlugin) {
        getControllerBeans(springBootPlugin).forEach(bean ->
            unregisterController(springBootPlugin.getApplicationContext(), bean));
    }

    public Set<Object> getControllerBeans(SpringPlugin springBootPlugin) {
        ApplicationContext applicationContext = springBootPlugin.getApplicationContext();
        Set<Object> controllerBeans = new LinkedHashSet<>();
        try {
            controllerBeans.addAll(
                applicationContext.getBeansWithAnnotation(ExtController.class).values());
            controllerBeans.addAll(
                applicationContext.getBeansWithAnnotation(ExtRestController.class).values());
        } catch (BeansException e) {
            // ignore this exception
            log.warn(e.getMessage());
        }
        return controllerBeans;
    }

    public void unregisterController(ApplicationContext ctx, Object controller) {
        new HashMap<>(requestMappingHandlerMapping.getHandlerMethods()).forEach(
            (mapping, handlerMethod) -> {
                if (controller == handlerMethod.getBean()) {
                    requestMappingHandlerMapping.unregisterMapping(mapping);
                }
            });
        ExtensionsInjector.unregisterBeanFromContext(ctx, controller);
    }

}
