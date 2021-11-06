package run.halo.app.extensions.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author guqing
 * @since 2021-11-06
 */
public class PluginEventExpressionRootObject {
    private final ApplicationEvent event;

    private final Object[] args;

    public PluginEventExpressionRootObject(ApplicationEvent event, Object[] args) {
        this.event = event;
        this.args = args;
    }

    public ApplicationEvent getEvent() {
        return this.event;
    }

    public Object[] getArgs() {
        return this.args;
    }
}
