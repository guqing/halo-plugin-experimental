package xyz.guqing.plugin.core.exception;

/**
 * @author guqing
 * @since 2021-10-28
 */
public class RegisterRequestMappingException extends PluginOperationException {

    public RegisterRequestMappingException() {
        super();
    }

    public RegisterRequestMappingException(String message) {
        super(message);
    }

    public RegisterRequestMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegisterRequestMappingException(Throwable cause) {
        super(cause);
    }

    protected RegisterRequestMappingException(String message, Throwable cause,
        boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
