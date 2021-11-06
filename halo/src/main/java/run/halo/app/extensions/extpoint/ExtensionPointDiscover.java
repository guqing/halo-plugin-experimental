package run.halo.app.extensions.extpoint;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import run.halo.app.handler.file.FileHandler;

/**
 * @author guqing
 * @since 2021-11-06
 */
@Component
public class ExtensionPointDiscover implements InitializingBean {
    private final List<Class<?>> extpoints = new LinkedList<>();
    @Autowired
    private ApplicationContext applicationContext;

    public <T> void register(List<Class<T>> extComponents) {
        if (CollectionUtils.isEmpty(extComponents)) {
            return;
        }
        extpoints.addAll(extComponents);
    }

    public <T> List<T> getComponents(Class<T> extPoint) {
        return extpoints.stream()
            .filter(aClass -> aClass.isAssignableFrom(extPoint))
            .map(aClass -> applicationContext.getBeansOfType(aClass).values())
            .flatMap(Collection::stream)
            .map(bean -> (T) bean)
            .collect(Collectors.toList());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (FileHandler value : applicationContext.getBeansOfType(FileHandler.class).values()) {
            extpoints.add(value.getClass());
        }
    }
}
