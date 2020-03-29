package tn.mnlr.vripper.services;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Post;

import java.io.File;
import java.util.Map;

@Service
public class PathService {

    private static final Logger logger = LoggerFactory.getLogger(PathService.class);

    private final AppSettingsService appSettingsService;

    @Autowired
    public PathService(AppSettingsService appSettingsService) {
        this.appSettingsService = appSettingsService;
    }

    public final File getDownloadDestinationFolder(final String postTitle, final String forum, final String threadTitle, final Map<String, Object> metadata, final String destinationFolderName) {
        String title = metadata.get(Post.METADATA.RESOLVED_NAME.name()) != null ? String.valueOf(metadata.get(Post.METADATA.RESOLVED_NAME.name())) : postTitle;
        File sourceFolder = appSettingsService.getSettings().getSubLocation() ? new File(appSettingsService.getSettings().getDownloadPath(), sanitize(forum)) : new File(appSettingsService.getSettings().getDownloadPath());
        sourceFolder = appSettingsService.getSettings().getThreadSubLocation() ? new File(sourceFolder, threadTitle) : sourceFolder;
        if (destinationFolderName == null) {
            sourceFolder = new File(sourceFolder, sanitize(title));
            sourceFolder = makeDirs(sourceFolder);
        } else {
            sourceFolder = new File(sourceFolder, destinationFolderName);
        }
        return sourceFolder;
    }

    private synchronized File makeDirs(@NonNull File sourceFolder) {
        int counter = 1;
        do {
            if (sourceFolder.exists()) {
                sourceFolder = new File(sourceFolder.toString() + '.' + counter++);
            } else {
                if (!sourceFolder.mkdirs()) {
                    throw new RuntimeException(String.format("Failed to create the folder %s", sourceFolder.toString()));
                }
            }
        } while (!sourceFolder.exists());

        return sourceFolder;
    }

    private String sanitize(final String folderName) {
        String sanitizedFolderName = folderName.replaceAll("\\.|\\\\|/|\\||:|\\?|\\*|\"|<|>|\\p{Cntrl}", "_");
        logger.debug(String.format("%s sanitized to %s", folderName, sanitizedFolderName));
        return sanitizedFolderName;
    }

    /**
     * Will sanitize the image name and remove extension
     *
     * @param imageName path string
     * @return Sanitized local path string
     */
    public final String formatImageFileName(String imageName) {
        return sanitize(imageName);
    }
}
