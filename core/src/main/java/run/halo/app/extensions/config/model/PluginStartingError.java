package run.halo.app.extensions.config.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author guqing
 * @since 2021-11-02
 */
@Data
@AllArgsConstructor(staticName = "of")
public class PluginStartingError implements Serializable {

    private String pluginId;
    private String message;
    private String devMessage;
}
