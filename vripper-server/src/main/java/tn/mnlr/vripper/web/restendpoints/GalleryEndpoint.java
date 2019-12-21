package tn.mnlr.vripper.web.restendpoints;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.services.PathService;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(value = "*")
public class GalleryEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(GalleryEndpoint.class);

    @Getter
    private LoadingCache<CacheKey, byte[]> cache;

    @PostConstruct
    private void init() {

        CacheLoader<CacheKey, byte[]> loader = new CacheLoader<>() {
            @Override
            public byte[] load(CacheKey key) throws Exception {
                File destinationFolder = pathService.getDownloadDestinationFolder(key.getPostId());
                if (!destinationFolder.exists() || !destinationFolder.isDirectory()) {
                    return null;
                }
                BufferedImage image = ImageIO.read(Paths.get(destinationFolder.toPath().toString(), key.getImgName()).toFile());
                BufferedImage resize = Scalr.resize(image, 350);
                image.flush();
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(resize, "jpg", baos);
                    baos.flush();
                    return baos.toByteArray();
                }
            }
        };

        cache = CacheBuilder.newBuilder()
                .maximumSize(20000)
                .build(loader);
    }

    @Autowired
    private PathService pathService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception e) {
        logger.error("Error when process request", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }

    @GetMapping(value = "/image/{postId}/{imgName}", produces = {MediaType.IMAGE_JPEG_VALUE})
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity<byte[]> getImage(@PathVariable("postId") @NonNull String postId, @PathVariable("imgName") @NonNull String imgName) throws Exception {
        File destinationFolder = pathService.getDownloadDestinationFolder(postId);
        return ResponseEntity.ok(Files.readAllBytes(Paths.get(destinationFolder.toPath().toString(), imgName)));
    }

    @GetMapping(value = "/image/thumb/{postId}/{imgName}", produces = {MediaType.IMAGE_JPEG_VALUE})
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity<byte[]> getImageThumb(@PathVariable("postId") @NonNull String postId, @PathVariable("imgName") @NonNull String imgName) throws Exception {
        return ResponseEntity.ok(cache.get(new CacheKey(postId, imgName)));
    }

    @GetMapping("/gallery/{postId}")
    @ResponseStatus(value = HttpStatus.OK)
    public synchronized ResponseEntity<List<GalleryImage>> getGallery(@PathVariable("postId") @NonNull String postId) throws Exception {
        File destinationFolder = pathService.getDownloadDestinationFolder(postId);
        if (destinationFolder.exists() && destinationFolder.isDirectory()) {
            return ResponseEntity.ok(Arrays.stream(Objects.requireNonNull(destinationFolder.listFiles())).filter(f -> !f.getName().endsWith("tmp")).map(f -> new GalleryImage(f.getName(), null, f.getName(), f.getName())).collect(Collectors.toList()));
        }
        return new ResponseEntity("Gallery does not exist in download location, you probably removed it", HttpStatus.BAD_REQUEST);
    }
}

@Getter
@Setter
@NoArgsConstructor
class CacheKey {
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

@Getter
@Setter
@NoArgsConstructor
class GalleryImage {
    private String img;
    private String description;
    private String title;
    private String alt;

    public GalleryImage(String img, String description, String title, String alt) {
        this.img = img;
        this.description = description;
        this.title = title;
        this.alt = alt;
    }
}
