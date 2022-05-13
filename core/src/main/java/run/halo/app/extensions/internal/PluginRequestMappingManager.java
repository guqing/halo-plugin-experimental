package run.halo.app.extensions.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import run.halo.app.extensions.SpringPluginManager;
import run.halo.app.extensions.registry.ExtensionClassRegistry;
import run.halo.app.extensions.registry.ExtensionClassRegistry.ClassDescriptor;
import run.halo.app.extensions.registry.ExtensionContextRegistry;

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

    public void registerControllers(PluginWrapper pluginWrapper) {
        String pluginId = pluginWrapper.getPluginId();
        getControllerBeans((SpringPluginManager) pluginWrapper.getPluginManager(), pluginId)
            .forEach(this::registerController);
    }

    private void registerController(Object controller) {
        log.debug("Registering plugin request mapping for bean: [{}]", controller);
        Method detectHandlerMethods = ReflectionUtils.findMethod(RequestMappingHandlerMapping.class,
            "detectHandlerMethods", Object.class);
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
            log.warn("invocation target exception: [{}]", e.getMessage(), e);
        }
    }

    private void unregisterControllerMappingInternal(SpringPluginManager pluginManager, Object controller) {
        requestMappingHandlerMapping.getHandlerMethods()
            .forEach((mapping, handlerMethod) -> {
                if (controller == handlerMethod.getBean()) {
                    log.debug("Removed plugin request mapping [{}] from bean [{}]", mapping,
                        controller);
//                    DefaultListableBeanFactory factory = (DefaultListableBeanFactory)pluginManager.getApplicationContext().getAutowireCapableBeanFactory();
//                    factory.removeBeanDefinition(controller.getClass().getName());
                    requestMappingHandlerMapping.unregisterMapping(mapping);
                }
            });
    }

    public void removeControllerMapping(SpringPluginManager pluginManager, String pluginId) {
        getControllerBeans(pluginManager, pluginId)
            .forEach(bean -> unregisterControllerMappingInternal(pluginManager, bean));
    }

    public Collection<Object> getControllerBeans(SpringPluginManager pluginManager, String pluginId) {
        ApplicationContext applicationContext = pluginManager.getApplicationContext();
        GenericApplicationContext pluginContext =
            ExtensionContextRegistry.getInstance().getByPluginId(pluginId);
        return pluginContext.getBeansWithAnnotation(Controller.class).values();
    }
}
