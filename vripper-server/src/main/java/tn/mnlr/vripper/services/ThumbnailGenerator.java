package tn.mnlr.vripper.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ThumbnailGenerator {

    @Getter
    private LoadingCache<CacheKey, byte[]> thumbnails;

    @Value("${base.dir}")
    private String baseDir;

    @Autowired
    private PathService pathService;

    @Getter
    private File cacheFolder;
    CacheLoader<CacheKey, byte[]> loader = new CacheLoader<>() {
        @Override
        public byte[] load(CacheKey key) throws Exception {
            File destinationFolder = pathService.getDownloadDestinationFolder(key.getPostId());
            if (!destinationFolder.exists() || !destinationFolder.isDirectory()) {
                return null;
            }
            return Files.readAllBytes(generateThumbnail(new File(destinationFolder, key.getImgName()), key.getPostId()).toPath());
        }
    };

    @PostConstruct
    private void init() throws Exception {
        cacheFolder = new File(baseDir + File.separator + ".vripper" + File.separator + "cache");
        cacheFolder.mkdirs();
        if (!cacheFolder.exists()) {
            throw new Exception(String.format("%s could not be created", cacheFolder.toString()));
        }
        thumbnails = CacheBuilder.newBuilder()
                .maximumSize(20000)
                .build(loader);
    }

    public void clearCache() {
        thumbnails.invalidateAll();
        for (File file : Optional.ofNullable(cacheFolder.listFiles()).orElse(new File[]{})) {
            FileSystemUtils.deleteRecursively(file);
        }
    }

    public long cacheSize() {

        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(cacheFolder.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    System.out.println("skipped: " + file + " (" + exc + ")");
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null)
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    else
                        size.addAndGet(dir.toFile().length());
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();
    }

    private File generateThumbnail(File inputFile, String postId) throws Exception {
        if (!inputFile.exists()) {
            throw new Exception(String.format("Input file %s does not exist", inputFile.toString()));
        }
        File postsCacheFolder = new File(cacheFolder, postId);
        postsCacheFolder.mkdirs();
        if (!postsCacheFolder.exists()) {
            throw new Exception(String.format("%s could not be created", postsCacheFolder.toString()));
        }

        File thumbFile = new File(postsCacheFolder, inputFile.getName());
        if (thumbFile.exists()) {
            return thumbFile;
        }

        BufferedImage image = ImageIO.read(inputFile);
        BufferedImage resize = Scalr.resize(image, 350, 350);
        ImageIO.write(resize, "jpg", thumbFile);
        image.flush();
        resize.flush();
        return thumbFile;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CacheKey {
        private String postId;
        private String imgName;

        public CacheKey(String postId, String imgName) {
            this.postId = postId;
            this.imgName = imgName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(postId, cacheKey.postId) &&
                    Objects.equals(imgName, cacheKey.imgName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, imgName);
        }
    }
}