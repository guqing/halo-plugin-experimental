package xyz.guqing.plugin.core.exception;

/**
 * 插件操作异常
 *
 * @author guqing
 * @since 2021-10-28
 */
public class PluginOperationException extends RuntimeException {

    public PluginOperationException() {
        super();
    }

    public PluginOperationException(String message) {
        super(message);
    }

    public PluginOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginOperationException(Throwable cause) {
        super(cause);
    }

    protected PluginOperationException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
