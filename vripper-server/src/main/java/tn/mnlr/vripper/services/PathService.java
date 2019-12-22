package tn.mnlr.vripper.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Post;

import java.io.File;

@Service
public class PathService {

    private static final Logger logger = LoggerFactory.getLogger(PathService.class);

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private AppSettingsService appSettingsService;

    public final File getDownloadDestinationFolder(String postId) {
        Post post = appStateService.getCurrentPosts().get(postId);
        String postTitle = post.getTitle();
        String threadTitle = post.getThreadTitle();
        File sourceFolder = appSettingsService.isSubLocation() ? new File(appSettingsService.getDownloadPath(), sanitize(post.getForum())) : new File(appSettingsService.getDownloadPath());
        sourceFolder = appSettingsService.isThreadSubLocation() ? new File(sourceFolder, threadTitle) : sourceFolder;
        return new File(sourceFolder, sanitize(postTitle + "_" + postId));
    }

    public final String sanitize(final String folderName) {
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
