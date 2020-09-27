package tn.mnlr.vripper.services;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.jpa.domain.Post;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PathService {

    public static final int MAX_ATTEMPTS = 24;
    private final AppSettingsService appSettingsService;
    private final DataService dataService;

    private final CommonExecutor commonExecutor;

    @Autowired
    public PathService(AppSettingsService appSettingsService, DataService dataService, CommonExecutor commonExecutor) {
        this.appSettingsService = appSettingsService;
        this.dataService = dataService;
        this.commonExecutor = commonExecutor;
    }

    public final File getDownloadDestinationFolder(Post post) {
        return _getDownloadDestinationFolder(post.getForum(), post.getThreadTitle(), post.getPostFolderName(), post.getPostId());
    }

    private File _getRootFolder(@NonNull String forum, @NonNull String threadTitle) {
        File sourceFolder = appSettingsService.getSettings().getSubLocation() ? new File(appSettingsService.getSettings().getDownloadPath(), sanitize(forum)) : new File(appSettingsService.getSettings().getDownloadPath());
        return appSettingsService.getSettings().getThreadSubLocation() ? new File(sourceFolder, threadTitle) : sourceFolder;
    }

    private File _getDownloadDestinationFolder(@NonNull String forum, @NonNull String threadTitle, @NonNull String title, @NonNull String postId) {
        File sourceFolder = _getRootFolder(forum, threadTitle);
        return new File(sourceFolder, title);
    }

    private File _createDownloadDestinationFolder(@NonNull String forum, @NonNull String threadTitle, @NonNull String title, @NonNull String postId) {
        File sourceFolder = _getRootFolder(forum, threadTitle);
        return new File(sourceFolder, appSettingsService.getSettings().getAppendPostId() ? title + "_" + postId : title);
    }

    public final void createDefaultPostFolder(Post post) {
        File sourceFolder = _createDownloadDestinationFolder(post.getForum(), post.getThreadTitle(), sanitize(post.getTitle()), post.getPostId());
        File destFolder = makeDirs(sourceFolder);
        post.setPostFolderName(destFolder.getName());
        dataService.updatePostFolderName(post.getPostFolderName(), post.getId());
    }

    public final void rename(@NonNull String postId, @NonNull String altName) {
        Post post = dataService.findPostByPostId(postId).orElseThrow();
        commonExecutor.getGeneralExecutor().submit(() -> {
            if (altName.equals(post.getTitle())) {
                return;
            }
            post.setTitle(altName);
            dataService.updatePostTitle(post.getTitle(), post.getId());
            if (post.getPostFolderName() == null) {
                return;
            }
            File newDestFolder = makeDirs(_getDownloadDestinationFolder(post.getForum(), post.getThreadTitle(), sanitize(altName), postId));
            File currentDesFolder = getDownloadDestinationFolder(post);
            post.setPostFolderName(newDestFolder.getName());
            dataService.updatePostFolderName(post.getPostFolderName(), post.getId());

            List<File> files = Arrays.stream(Objects.requireNonNull(currentDesFolder.listFiles())).filter(e -> !e.getName().endsWith(".tmp")).collect(Collectors.toList());
            for (File f : files) {
                try {
                    Files.move(f.toPath(), Paths.get(newDestFolder.toString(), f.toPath().getFileName().toString()), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error(String.format("Failed to move files from %s to %s", currentDesFolder.toString(), newDestFolder.toString()), e);
                    return;
                }
            }

            int attempts = 0;
            while (currentDesFolder.exists() && attempts <= MAX_ATTEMPTS) {
                attempts++;
                if (!currentDesFolder.delete()) {
                    log.warn(String.format("Failed to remove %s", currentDesFolder.toString()));
                }
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException ignored) {
                }
            }
            if (attempts > MAX_ATTEMPTS) {
                log.error(String.format("Failed to rename post %s", postId));
            }
        });
    }

    private File makeDirs(@NonNull final File sourceFolder) {
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
        log.debug(String.format("%s sanitized to %s", folderName, sanitizedFolderName));
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
