package run.halo.app.extensions;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

@Slf4j
public abstract class SpringPlugin extends Plugin {

    private PluginApplicationContext applicationContext;

    public SpringPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * <p>Lazy initialization plugin application context,
     * avoid being unable to get context when system start scan plugin.</p>
     * <p>The plugin application context is not created until the plug-in is started.</p>
     *
     * @return Plugin application context.
     */
    public synchronized final PluginApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            applicationContext =
                getPluginManager().getPluginApplicationContext(this.wrapper.getPluginId());
        }
        return applicationContext;
    }

    public SpringPluginManager getPluginManager() {
        return (SpringPluginManager) getWrapper().getPluginManager();
    }
}
