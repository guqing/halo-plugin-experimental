package xyz.guqing.plugin.core.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import xyz.guqing.plugin.core.SpringBootPlugin;
import xyz.guqing.plugin.core.boot.SpringBootstrap;
import xyz.guqing.plugin.core.exception.RegisterRequestMappingException;

/**
 * 插件RequestMapping资源映射管理器
 *
 * @author guqing
 * @see RequestMappingHandlerMapping
 */
@Slf4j
// extends RequestMappingHandlerMapping
public class PluginRequestMappingManager {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public PluginRequestMappingManager(
        RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    //    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public void detectHandlerMethods(Object controller) {
//        super.detectHandlerMethods(controller);
//    }

    public void registerControllers(SpringBootPlugin springBootPlugin) {
        getControllerBeans(springBootPlugin).forEach(
            bean -> registerController(springBootPlugin, bean));
    }

    private void registerController(SpringBootPlugin springBootPlugin, Object controller) {
        String beanName = controller.getClass().getName();
        // unregister RequestMapping if already registered
        unregisterController(springBootPlugin.getMainApplicationContext(), controller);
        springBootPlugin.registerBeanToMainContext(beanName, controller);

        // requestMappingHandlerMapping.detectHandlerMethods(controller);
        Method detectHandlerMethods =
            ReflectionUtils.findMethod(RequestMappingHandlerMapping.class, "detectHandlerMethods",
                Object.class);
        if (detectHandlerMethods == null) {
            throw new RegisterRequestMappingException(
                "Unable to register Controller to the main container.");
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
            // throw new RegisterRequestMappingException(e);
        }
    }

    public void unregisterControllers(SpringBootPlugin springBootPlugin) {
        getControllerBeans(springBootPlugin).forEach(bean ->
            unregisterController(springBootPlugin.getMainApplicationContext(), bean));
    }

    public Set<Object> getControllerBeans(SpringBootPlugin springBootPlugin) {
        LinkedHashSet<Object> beans = new LinkedHashSet<>();
        ApplicationContext applicationContext = springBootPlugin.getApplicationContext();
        //noinspection unchecked
        Set<String> sharedBeanNames = (Set<String>) applicationContext.getBean(
            SpringBootstrap.BEAN_IMPORTED_BEAN_NAMES);
        beans.addAll(applicationContext.getBeansWithAnnotation(Controller.class)
            .entrySet().stream().filter(beanEntry -> !sharedBeanNames.contains(beanEntry.getKey()))
            .map(Map.Entry::getValue).collect(Collectors.toList()));
        beans.addAll(applicationContext.getBeansWithAnnotation(RestController.class)
            .entrySet().stream().filter(beanEntry -> !sharedBeanNames.contains(beanEntry.getKey()))
            .map(Map.Entry::getValue).collect(Collectors.toList()));
        return beans;
    }

    public void unregisterController(GenericApplicationContext mainCtx, Object controller) {

        new HashMap<>(requestMappingHandlerMapping.getHandlerMethods()).forEach(
            (mapping, handlerMethod) -> {
                if (controller == handlerMethod.getBean()) {
                    requestMappingHandlerMapping.unregisterMapping(mapping);
                }
            });
        SpringBootPlugin.unregisterBeanFromMainContext(mainCtx, controller);
    }

}
