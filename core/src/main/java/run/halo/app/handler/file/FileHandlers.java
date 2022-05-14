package run.halo.app.handler.file;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import run.halo.app.exception.FileOperationException;
import run.halo.app.extensions.extpoint.ExtensionComponent;
import run.halo.app.extensions.extpoint.ExtensionList;
import run.halo.app.extensions.extpoint.ExtensionPointFinder;
import run.halo.app.model.entity.Attachment;
import run.halo.app.model.enums.AttachmentType;
import run.halo.app.model.support.UploadResult;

/**
 * File handler manager.
 *
 * @author johnniang
 * @date 2019-03-27
 */
@Slf4j
@Component
public class FileHandlers {

    private final PluginManager pluginManager;

    public FileHandlers(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        // Add all file handler
        //addFileHandlers(applicationContext.getBeansOfType(FileHandler.class).values());
    }

    /**
     * Uploads files.
     *
     * @param file           multipart file must not be null
     * @param attachmentType attachment type must not be null
     * @return upload result
     * @throws FileOperationException throws when fail to delete attachment or no available file
     *                                handler to upload it
     */
    @NonNull
    public UploadResult upload(@NonNull MultipartFile file,
                               @NonNull AttachmentType attachmentType) {
        return getSupportedType(attachmentType).upload(file);
    }

    /**
     * Deletes attachment.
     *
     * @param attachment attachment detail must not be null
     * @throws FileOperationException throws when fail to delete attachment or no available file
     *                                handler to delete it
     */
    public void delete(@NonNull Attachment attachment) {
        Assert.notNull(attachment, "Attachment must not be null");
        getSupportedType(attachment.getType())
            .delete(attachment.getFileKey());
    }

    private FileHandler getSupportedType(AttachmentType type) {
        List<FileHandler> extensions = pluginManager.getExtensions(FileHandler.class);

        for (FileHandler fileHandler : extensions) {
            if (fileHandler.getAttachmentType() == type) {
                log.info("Used {} file handler(s)", fileHandler);
                return fileHandler;
            }
        }
        throw new FileOperationException("No available file handlers to operate the file")
            .setErrorData(type);
    }
}
