package xyz.guqing.plugin.core.internal;

import java.io.IOException;
import java.util.Arrays;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;
import xyz.guqing.plugin.core.utils.ApplicationContextProvider;

/**
 * @author <a href="https://github.com/hank-cp">Hank CP</a>
 */
public class PluginResourceResolver extends PathResourceResolver {

    @Autowired @Lazy
    private PluginManager pluginManager;

    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
        if (!(location instanceof ClassPathResource)) return null;
        ClassPathResource classPathLocation = (ClassPathResource) location;

        // pluginManager might not be autowired correctly because resolve bean
        // is instantiated before PluginManager.
        if (pluginManager == null) {
            pluginManager = ApplicationContextProvider.getBean(PluginManager.class);
        }

        for (PluginWrapper plugin : pluginManager.getPlugins(PluginState.STARTED)) {
            Resource pluginLocation = new ClassPathResource(classPathLocation.getPath(), plugin.getPluginClassLoader());
            Resource resource = pluginLocation.createRelative(resourcePath);
            if (resource.isReadable()) {
                if (checkResource(resource, pluginLocation)) {
                    return resource;
                }
                else if (logger.isWarnEnabled()) {
                    Resource[] allowedLocations = getAllowedLocations();
                    logger.warn("Resource path \"" + resourcePath + "\" was successfully resolved " +
                            "but resource \"" +	resource.getURL() + "\" is neither under the " +
                            "current location \"" + location.getURL() + "\" nor under any of the " +
                            "allowed locations " + (allowedLocations != null ? Arrays.asList(allowedLocations) : "[]"));
                }
            }
        }
        return super.getResource(resourcePath, location);
    }
}
