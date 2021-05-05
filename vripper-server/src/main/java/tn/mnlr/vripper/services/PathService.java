package tn.mnlr.vripper.services;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.exception.RenameException;
import tn.mnlr.vripper.jpa.domain.Post;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class PathService {

  private final SettingsService settingsService;
  private final DataService dataService;

  @Getter private final ReentrantLock directoryAccess = new ReentrantLock();

  public PathService(SettingsService settingsService, DataService dataService) {
    this.settingsService = settingsService;
    this.dataService = dataService;
  }

  public final File calcDownloadDirectory(Post post) {
    return new File(settingsService.getSettings().getDownloadPath(), post.getDownloadDirectory());
  }

  private File getRootFolder(@NonNull String forum, @NonNull String threadTitle) {
    File sourceFolder =
        settingsService.getSettings().getSubLocation()
            ? new File(settingsService.getSettings().getDownloadPath(), sanitize(forum))
            : new File(settingsService.getSettings().getDownloadPath());
    return settingsService.getSettings().getThreadSubLocation()
        ? new File(sourceFolder, threadTitle)
        : sourceFolder;
  }

  public final void createDefaultPostFolder(Post post) {
    File downloadDirectory = getRootFolder(post.getForum(), post.getThreadTitle());
    downloadDirectory =
        new File(
            downloadDirectory,
            settingsService.getSettings().getAppendPostId()
                ? sanitize(post.getTitle()) + "_" + post.getPostId()
                : sanitize(post.getTitle()));
    downloadDirectory = makeDir(downloadDirectory);
    post.setDownloadDirectory(
        downloadDirectory
            .getAbsolutePath()
            .replace(settingsService.getSettings().getDownloadPath(), ""));
    dataService.updateDownloadDirectory(post.getDownloadDirectory(), post.getId());
  }

  public final void rename(@NonNull String postId, @NonNull String altName) throws RenameException {
    Post post = dataService.findPostByPostId(postId).orElseThrow();
    if (altName.equals(post.getTitle())) {
      return;
    }

    // Download have not started yet
    if (post.getDownloadDirectory() == null) {
      post.setTitle(altName);
      dataService.updatePostTitle(post.getTitle(), post.getId());
      return;
    }

    File newDownloadDirectory = getRootFolder(post.getForum(), post.getThreadTitle());
    newDownloadDirectory = new File(newDownloadDirectory, sanitize(altName));
    File currentDownloadDirectory = calcDownloadDirectory(post);
    try {
      directoryAccess.lock();
      Files.move(
          currentDownloadDirectory.toPath(),
          newDownloadDirectory.toPath(),
          StandardCopyOption.ATOMIC_MOVE);
      post.setDownloadDirectory(
          newDownloadDirectory
              .getAbsolutePath()
              .replace(settingsService.getSettings().getDownloadPath(), ""));
      dataService.updateDownloadDirectory(post.getDownloadDirectory(), post.getId());

      post.setTitle(altName);
      dataService.updatePostTitle(post.getTitle(), post.getId());
    } catch (IOException e) {
      throw new RenameException(
          String.format(
              "Failed to move files from %s to %s", currentDownloadDirectory, newDownloadDirectory),
          e);
    } finally {
      directoryAccess.unlock();
    }
  }

  private File makeDir(@NonNull final File sourceFolder) {
    int counter = 1;
    File folder = sourceFolder;

    while (folder.exists()) {
      folder = new File(sourceFolder.toString() + '.' + counter++);
    }

    if (!folder.mkdirs()) {
      throw new RuntimeException(String.format("Failed to create the folder %s", sourceFolder));
    }

    return folder;
  }

  private String sanitize(final String folderName) {
    String sanitizedFolderName =
        folderName.replaceAll("\\.|\\\\|/|\\||:|\\?|\\*|\"|<|>|\\p{Cntrl}", "_");
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
