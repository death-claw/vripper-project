package tn.mnlr.vripper.web.restendpoints;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.PathService;
import tn.mnlr.vripper.services.ThumbnailGenerator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(value = "*")
public class GalleryEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(GalleryEndpoint.class);

    @Autowired
    private PathService pathService;

    @Autowired
    private ThumbnailGenerator thumbnailGenerator;

    private static final String cacheControl = CacheControl.maxAge(365, TimeUnit.DAYS).getHeaderValue();
    @Autowired
    private AppSettingsService appSettingsService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleException(Exception e) {
        logger.error("Error when process request", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }

    @GetMapping(value = "/image/{postId}/{imgName}", produces = {MediaType.IMAGE_JPEG_VALUE})
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<byte[]> getImage(@PathVariable("postId") @NonNull String postId, @PathVariable("imgName") @NonNull String imgName, HttpServletResponse response) throws Exception {
        if (!appSettingsService.isViewPhotos()) {
            return new ResponseEntity("Gallery option is disabled", HttpStatus.BAD_REQUEST);
        }
        response.addHeader("Cache-Control", cacheControl);
        File destinationFolder = pathService.getDownloadDestinationFolder(postId);
        return ResponseEntity.ok(Files.readAllBytes(Paths.get(destinationFolder.toPath().toString(), imgName)));
    }

    @GetMapping(value = "/image/thumb/{postId}/{imgName}", produces = {MediaType.IMAGE_JPEG_VALUE})
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<byte[]> getImageThumb(@PathVariable("postId") @NonNull String postId, @PathVariable("imgName") @NonNull String imgName, HttpServletResponse response) throws Exception {
        if (!appSettingsService.isViewPhotos()) {
            return new ResponseEntity("Gallery option is disabled", HttpStatus.BAD_REQUEST);
        }
        response.addHeader("Cache-Control", cacheControl);
        File destinationFolder = pathService.getDownloadDestinationFolder(postId);
        if (destinationFolder.exists() && destinationFolder.isDirectory()) {
            return ResponseEntity.ok(thumbnailGenerator.getThumbnails().get(new ThumbnailGenerator.CacheKey(postId, imgName)));
        }
        return new ResponseEntity("Gallery does not exist in download location, you probably removed it", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/gallery/{postId}")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<List<GalleryImage>> getGallery(@PathVariable("postId") @NonNull String postId) {
        if (!appSettingsService.isViewPhotos()) {
            return new ResponseEntity("Gallery option is disabled", HttpStatus.BAD_REQUEST);
        }
        File destinationFolder = pathService.getDownloadDestinationFolder(postId);
        if (destinationFolder.exists() && destinationFolder.isDirectory()) {
            return ResponseEntity.ok(
                    Arrays.stream(Objects.requireNonNull(destinationFolder.listFiles()))
                            .filter(f -> !f.getName().endsWith("tmp"))
                            .sorted(Comparator.comparing(File::getName))
                            .map(GalleryImage::fromFile)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
        }
        return new ResponseEntity("Gallery does not exist in download location, you probably removed it", HttpStatus.BAD_REQUEST);
    }
}

@Getter
@Setter
@NoArgsConstructor
class GalleryImage {

    private static final Logger logger = LoggerFactory.getLogger(GalleryImage.class);
    private String src;
    private String msrc;
    private double w;
    private double h;

    public GalleryImage(String src, String msrc, double w, double h) {
        this.src = src;
        this.msrc = msrc;
        this.w = w;
        this.h = h;
    }

    public static GalleryImage fromFile(File file) {
        Dimension dimension;
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    dimension = new Dimension(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            } else {
                logger.error(String.format("No reader found for image %s", file.toString()));
                return null;
            }
        } catch (Exception e) {
            logger.error(String.format("Failed to create image object for %s", file.toString()), e);
            return null;
        }

        return new GalleryImage(file.getName(), file.getName(), dimension.getWidth(), dimension.getHeight());
    }
}
