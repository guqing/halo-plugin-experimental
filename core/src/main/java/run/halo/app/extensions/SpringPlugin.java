package run.halo.app.extensions;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationContext;

@Slf4j
public abstract class SpringPlugin extends Plugin {

    private final ApplicationContext applicationContext;

    public SpringPlugin(PluginWrapper wrapper) {
        super(wrapper);
        this.applicationContext = getPluginManager().getApplicationContext();
    }

    public final ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public SpringPluginManager getPluginManager() {
        return (SpringPluginManager) getWrapper().getPluginManager();
    }
}
