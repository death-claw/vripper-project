package tn.mnlr.vripper.services;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Post;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PathService {

    private static final Logger logger = LoggerFactory.getLogger(PathService.class);

    private final AppSettingsService appSettingsService;

    private final CommonExecutor commonExecutor;

    private final int MAX_DELETE_ATTEMPTS = 180;

    @Autowired
    public PathService(AppSettingsService appSettingsService, CommonExecutor commonExecutor) {
        this.appSettingsService = appSettingsService;
        this.commonExecutor = commonExecutor;
    }

    public final File getDownloadDestinationFolder(Post post) {
        return _getDownloadDestinationFolder(post.getForum(), post.getThreadTitle(), post.getPostFolderName());
    }

    private File _getDownloadDestinationFolder(@NonNull String forum, @NonNull String threadTitle, @NonNull String title) {
        File sourceFolder = appSettingsService.getSettings().getSubLocation() ? new File(appSettingsService.getSettings().getDownloadPath(), sanitize(forum)) : new File(appSettingsService.getSettings().getDownloadPath());
        sourceFolder = appSettingsService.getSettings().getThreadSubLocation() ? new File(sourceFolder, threadTitle) : sourceFolder;
        return new File(sourceFolder, sanitize(title));
    }

    public synchronized final void createDefaultPostFolder(Post post) {
        File sourceFolder = _getDownloadDestinationFolder(post.getForum(), post.getThreadTitle(), post.getTitle());
        File destFolder = makeDirs(sourceFolder);
        post.setPostFolderName(destFolder.getName());
    }

    public synchronized final void rename(Post post, String altName) {
        post.setTitle(altName);
        if (post.getPostFolderName() == null) {
            return;
        }
        File newDestFolder = makeDirs(_getDownloadDestinationFolder(post.getForum(), post.getThreadTitle(), altName));
        File currentDesFolder = getDownloadDestinationFolder(post);
        post.setPostFolderName(newDestFolder.getName());

        List<File> files = Optional.ofNullable(currentDesFolder.listFiles()).stream().flatMap(Arrays::stream).filter(e -> !e.getName().endsWith(".tmp")).collect(Collectors.toList());
        for (File f : files) {
            try {
                Files.move(f.toPath(), Paths.get(newDestFolder.toString(), f.toPath().getFileName().toString()), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error(String.format("Failed to move files from %s to %s", currentDesFolder.toString(), newDestFolder.toString()), e);
                return;
            }
        }

        commonExecutor.getGeneralExecutor().submit(() -> {
            int attemptCount = 0;
            while (Objects.requireNonNull(currentDesFolder.listFiles()).length != 0 && attemptCount < MAX_DELETE_ATTEMPTS) {
                try {
                    attemptCount++;
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (!currentDesFolder.delete()) {
                logger.warn(String.format("Failed to remove %s", currentDesFolder.toString()));
            }
        });
    }

    private synchronized File makeDirs(@NonNull final File sourceFolder) {
        int counter = 1;
        File folder = sourceFolder;

        while (folder.exists()) {
            folder = new File(sourceFolder.toString() + '.' + counter++);
        }

        if (!folder.mkdirs()) {
            throw new RuntimeException(String.format("Failed to create the folder %s", sourceFolder.toString()));
        }

        return folder;
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
