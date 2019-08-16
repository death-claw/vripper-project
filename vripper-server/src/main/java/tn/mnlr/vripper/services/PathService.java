package tn.mnlr.vripper.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class PathService {

    private static final Logger logger = LoggerFactory.getLogger(PathService.class);

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private AppSettingsService appSettingsService;

    public final File getDownloadDestinationFolder(String postId) {
        String postTitle = appStateService.getCurrentPosts().get(postId).getTitle();
        return new File(appSettingsService.getDownloadPath(), sanitize(postTitle + "_" + postId));
    }

    public final String sanitize(final String folderName) {
        String sanitizedFolderName = folderName.replaceAll("\\.|\\\\|/|\\||:|\\?|\\*|\"|<|>|\\p{Cntrl}", "_");
        logger.debug(String.format("%s sanitized to %s", folderName, sanitizedFolderName));
        return sanitizedFolderName;
    }

    /**
     * Will sanitize the image name and remove extension
     *
     * @param imageName
     * @return
     */
    public final String formatImageFileName(String imageName) {
        int extensionIndex = imageName.lastIndexOf('.');
        String fileName;
        if (extensionIndex != -1) {
            fileName = imageName.substring(0, extensionIndex);
        } else {
            fileName = imageName;
        }
        return sanitize(fileName);
    }
}
