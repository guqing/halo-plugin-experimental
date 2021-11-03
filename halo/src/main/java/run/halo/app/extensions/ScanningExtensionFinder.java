package run.halo.app.extensions;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.AbstractExtensionFinder;
import org.pf4j.Extension;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import run.halo.app.Application;

/**
 * Extension finder using classGraph to support Spring annotations.
 *
 * @author guqing
 * @since 2021-11-02
 */
@Slf4j
public class ScanningExtensionFinder extends AbstractExtensionFinder {

    public ScanningExtensionFinder(PluginManager pluginManager) {
        super(pluginManager);
    }

    @Override
    public Map<String, Set<String>> readPluginsStorages() {
        log.debug("Reading extensions storages from plugins");
        Map<String, Set<String>> result = new LinkedHashMap<>();

        List<PluginWrapper> plugins = pluginManager.getPlugins();
        for (PluginWrapper plugin : plugins) {
            String pluginId = plugin.getDescriptor().getPluginId();
            log.debug("Reading extensions storage from plugin '{}'", pluginId);
            Set<String> bucket = new HashSet<>();
            if(Objects.nonNull(plugin.getPlugin())) {

                try (ScanResult scanResult =
                    new ClassGraph()
                        .enableAllInfo()
                        .addClassLoader(plugin.getPluginClassLoader())
                        .whitelistPackages(plugin.getPlugin().getClass().getPackage().getName())
                        .scan()) {

                    for (ClassInfo classInfo : getExtensionClasses(scanResult)) {
                        if(log.isInfoEnabled()) {
                            log.info("Found extension {}", classInfo.getName());
                        }
                        bucket.add(classInfo.getName());
                    }
                }
            }

            debugExtensions(bucket);

            result.put(pluginId, bucket);
        }

        return result;
    }

    private ClassInfoList getExtensionClasses(ScanResult scanResult) {
        ClassInfoList classInfos = new ClassInfoList();
        List<String> extensionAnnotationNames = List.of(Extension.class.getName(),
            Service.class.getName(),
            Repository.class.getName(),
            Resource.class.getName(),
            Controller.class.getName(),
            RestController.class.getName(),
            Component.class.getName(),
            Configuration.class.getName());

        for (String extensionAnnotationName : extensionAnnotationNames) {
            ClassInfoList classesWithAnnotation =
                scanResult.getClassesWithAnnotation(extensionAnnotationName);
            for (ClassInfo classInfo : classesWithAnnotation) {
                if (!classInfos.contains(classInfo) && !classInfo.isAnnotation()) {
                    classInfos.add(classInfo);
                }
            }
        }
        return classInfos;
    }

    @Override
    public Map<String, Set<String>> readClasspathStorages() {
        log.debug("Reading extensions storages from classpath");
        Map<String, Set<String>> result = new LinkedHashMap<>();

        Set<String> bucket = new HashSet<>();

        try (ScanResult scanResult = new ClassGraph()
            .enableAllInfo()
            .addClassLoader(getClass().getClassLoader())
            .whitelistPackages(Application.class.getPackage().getName())
            .scan()) {
            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(Extension.class.getName())) {

                if(log.isInfoEnabled()) {
                    log.info("Found extension {}", classInfo.getName());
                }
                bucket.add(classInfo.getName());
            }
        }

        debugExtensions(bucket);

        result.put(null, bucket);

        return result;
    }
}
