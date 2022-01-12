package run.halo.app.extensions.listener;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import run.halo.app.config.properties.HaloProperties;
import run.halo.app.extensions.SpringPluginManager;
import run.halo.app.extensions.config.PluginProperties;
import run.halo.app.extensions.event.HaloPluginStartedEvent;
import run.halo.app.extensions.event.HaloPluginStoppedEvent;
import run.halo.app.extensions.event.HaloPluginWebStartedEvent;
import run.halo.app.extensions.extpoint.ExtensionPointFinder;
import run.halo.app.extensions.registry.ExtensionClassRegistry;
import run.halo.app.extensions.registry.ExtensionClassRegistry.ClassDescriptor;
import run.halo.app.extensions.registry.PluginListenerRegistry;

import javax.annotation.Resource;

import static run.halo.app.model.support.HaloConst.FILE_SEPARATOR;
import static run.halo.app.model.support.HaloConst.USER_HOME;
import static run.halo.app.utils.HaloUtils.ensureSuffix;

/**
 * Halo plugin state changed listener for Spring Boot.
 *
 * @author guqing
 * @see PluginProperties
 */
@Slf4j
@Configuration
@ConditionalOnClass({PluginManager.class, SpringPluginManager.class})
@ConditionalOnProperty(prefix = PluginProperties.PREFIX, value = "enabled", havingValue = "true")
public class PluginStateChangedListener {

    @Autowired
    private ExtensionPointFinder extensionPointFinder;

    @Autowired
    private PluginListenerRegistry listenerRegistry;

    @Resource
    private HaloProperties haloProperties;

    @EventListener(HaloPluginStartedEvent.class)
    public void onPluginStarted(HaloPluginStartedEvent event) {
        String pluginId = event.getPlugin().getPluginId();
        List<Class<?>> listenerClasses =
            ExtensionClassRegistry.getInstance().findClasses(pluginId, ClassDescriptor::isListener);
        for (Class<?> listenerClass : listenerClasses) {
            listenerRegistry.addPluginListener(event.getPlugin().getPluginId(), listenerClass);
        }

        this.extensionPointFinder.refreshExtensions();
        log.info("The plugin starts successfully.");
    }

    @EventListener(HaloPluginWebStartedEvent.class)
    public void onPluginWebStarted(HaloPluginWebStartedEvent event) {
        String pluginId = event.getPlugin().getPluginId();
        Path path = event.getPlugin().getPluginPath().toAbsolutePath();
        String targetPath = ensureSuffix(haloProperties.getWorkDir(), FILE_SEPARATOR) + "static";
        final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/plugin-"+pluginId+"-*.zip");
        try {
            final Path[] sourcePath = {null};
            Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (pathMatcher.matches(file)) {
                        sourcePath[0] = file;
                        return FileVisitResult.CONTINUE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            if (sourcePath[0] != null) {
                new ZipFile(sourcePath[0].toFile()).extractFile("classes/static/", targetPath, pluginId);
            }
            log.info("The plugin html deploy successfully.");
        } catch (Exception e) {
            log.error("The plugin html deploy fail.", e);
        }


    }

    @EventListener(HaloPluginStoppedEvent.class)
    public void onPluginStopped(HaloPluginStoppedEvent event) {
        listenerRegistry.removePluginListener(event.getPlugin().getPluginId());
        this.extensionPointFinder.refreshExtensions();
        log.info("Plugin {} is stopped", event.getPlugin().getPluginId());
    }
}