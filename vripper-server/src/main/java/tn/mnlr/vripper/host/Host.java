package tn.mnlr.vripper.host;

import lombok.Getter;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.HtmlProcessorException;
import tn.mnlr.vripper.q.ImageFileData;
import tn.mnlr.vripper.services.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Objects;

@Service
abstract public class Host {

    private static final Logger logger = LoggerFactory.getLogger(Host.class);

    private static final int READ_BUFFER_SIZE = 8192;

    @Autowired
    protected HtmlProcessorService htmlProcessorService;

    @Autowired
    protected XpathService xpathService;

    @Autowired
    private AppSettingsService appSettingsService;

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private ConnectionManager cm;

    @Autowired
    private DownloadSpeedService downloadSpeedService;

    @Autowired
    private PathService pathService;

    abstract public String getHost();

    abstract public String getLookup();

    @Override
    public int hashCode() {
        return Objects.hash(pathService);
    }

    public boolean isSupported(String url) {
        return url.contains(getLookup());
    }

    public void download(Image image, ImageFileData imageFileData) throws DownloadException, InterruptedException {

        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());
        try {

            appStateService.postDownloadingUpdate(image.getPostId());

            imageFileData.setPageUrl(image.getUrl());

            /*
             * HOST SPECIFIC
             */
            logger.debug(String.format("Getting image url and name from %s using %s", image.getUrl(), this.getHost()));
            setNameAndUrl(image.getUrl(), imageFileData, context);
            logger.debug(String.format("Resolved name for %s: %s", image.getUrl(), imageFileData.getImageName()));
            logger.debug(String.format("Resolved image url for %s: %s", image.getUrl(), imageFileData.getImageUrl()));

            logger.debug(String.format("Building image request for %s", image.getUrl()));
            setImageRequest(imageFileData);
            /*
             * END HOST SPECIFIC
             */

            String formatImageFileName = pathService.formatImageFileName(imageFileData.getImageName());
            logger.debug(String.format("Sanitizing image name from %s to %s", imageFileData.getImageName(), formatImageFileName));
            imageFileData.setImageName(formatImageFileName);
            File destinationFolder = pathService.getDownloadDestinationFolder(image.getPostId());
            logger.debug(String.format("Saving to %s", destinationFolder.getPath()));
            logger.debug(String.format("Creating %s", destinationFolder.getPath()));
            destinationFolder.mkdirs();
            if (!destinationFolder.exists()) {
                logger.debug(String.format("Folder %s is created", destinationFolder.toString()));
            }

            HttpClient client = cm.getClient().build();

            logger.debug(String.format("Downloading %s", imageFileData.getImageUrl()));
            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(imageFileData.getImageRequest(), context)) {

                if (response.getStatusLine().getStatusCode() / 100 != 2) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    throw new DownloadException(String.format("Server returned code %d", response.getStatusLine().getStatusCode()));
                }

                File outputFile = new File(destinationFolder.getPath() + File.separator + imageFileData.getImageName() + ".tmp");
                try (InputStream downloadStream = response.getEntity().getContent(); FileOutputStream fos = new FileOutputStream(outputFile)) {
                    image.setTotal(response.getEntity().getContentLength());
                    logger.debug(String.format("%s length is %d", imageFileData.getImageUrl(), image.getTotal()));
                    logger.debug(String.format("Starting data transfer for %s", imageFileData.getImageUrl()));

                    byte[] buffer = new byte[READ_BUFFER_SIZE];
                    int read;
                    while ((read = downloadStream.read(buffer, 0, READ_BUFFER_SIZE)) != -1) {
                        fos.write(buffer, 0, read);
                        image.increase(read);
                        downloadSpeedService.increase(read);
                    }
                    fos.flush();
                    EntityUtils.consumeQuietly(response.getEntity());
                }
                File finalName = checkImageTypeAndRename(outputFile, imageFileData.getImageName(), image.getIndex());
                imageFileData.setFileName(finalName.getName());
            }
        } catch (Exception e) {
            if (Thread.interrupted()) {
                throw new InterruptedException("Download was interrupted");
            }
            throw new DownloadException(e);
        }
    }

    private File checkImageTypeAndRename(File outputFile, String imageName, int index) throws HostException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(outputFile)) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
            if (!it.hasNext()) {
                throw new HostException("Image file is not recognized!");
            }
            ImageReader reader = it.next();
            if (reader.getFormatName().toUpperCase().equals("JPEG")) {
                String imageNameLC = imageName.toLowerCase();
                if (!imageNameLC.endsWith("_jpg") && !imageNameLC.endsWith("_jpeg")) {
                    imageName += ".jpg";
                } else {
                    String toReplace = null;
                    if (imageNameLC.endsWith("_jpg")) {
                        toReplace = "_jpg";
                    } else if (imageNameLC.endsWith("_jpeg")) {
                        toReplace = "_jpeg";
                    }
                    if (toReplace != null) {
                        imageName = imageName.substring(0, imageName.length() - toReplace.length()) + ".jpg";
                    }
                }
            }
        } catch (Exception e) {
            throw new HostException("Failed to guess image format", e);
        }
        try {
            File outImage = new File(outputFile.getParent(), (appSettingsService.isForceOrder() ? String.format("%03d_", index) : "") + imageName);
            if (outImage.exists() && outImage.delete()) {
                logger.debug(String.format("%s is deleted", outImage.toString()));
            }
            return Files.move(outputFile.toPath(), outImage.toPath(), StandardCopyOption.ATOMIC_MOVE).toFile();
        } catch (Exception e) {
            throw new HostException("Failed to rename the image", e);
        }
    }

    final String getDefaultImageName(final String imgUrl) {
        String imageTitle = imgUrl.substring(imgUrl.lastIndexOf('/') + 1);
        logger.debug(String.format("Extracting name from url %s: %s", imgUrl, imageTitle));
        return imgUrl;
    }

    final Response getResponse(final String url, final HttpClientContext context) throws HostException {
        String basePage;

        HttpClient client = cm.getClient().build();
        HttpGet httpGet = cm.buildHttpGet(url);
        Header[] headers;
        logger.debug(String.format("Requesting %s", url));
        try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(httpGet, context)) {
            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                throw new HostException(String.format("Unexpected response code: %d", response.getStatusLine().getStatusCode()));
            }
            headers = response.getAllHeaders();
            basePage = EntityUtils.toString(response.getEntity());
            logger.debug(String.format("%s response: %n%s", url, basePage));
            EntityUtils.consumeQuietly(response.getEntity());
        } catch (IOException e) {
            throw new HostException(e);
        }

        try {
            logger.debug(String.format("Cleaning %s response", url));
            return new Response(htmlProcessorService.clean(basePage), headers);
        } catch (HtmlProcessorException e) {
            throw new HostException(e);
        }
    }

    protected String appendUri(String uri, String appendQuery) throws URISyntaxException {
        URI oldUri = new URI(uri);

        String newQuery = oldUri.getQuery();
        if (newQuery == null) {
            newQuery = appendQuery;
        } else {
            newQuery += "&" + appendQuery;
        }

        return new URI(oldUri.getScheme(), oldUri.getAuthority(),
                oldUri.getPath(), newQuery, oldUri.getFragment()).toString();
    }

    private void setImageRequest(final ImageFileData imageFileData) {
        HttpGet httpGet = cm.buildHttpGet(imageFileData.getImageUrl());
        httpGet.addHeader("Referer", imageFileData.getPageUrl());
        imageFileData.setImageRequest(httpGet);
    }

    protected abstract void setNameAndUrl(final String url, final ImageFileData imageFileData, final HttpClientContext context) throws HostException;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Host host = (Host) o;
        return Objects.equals(getHost(), host.getHost());
    }

    @Getter
    public static class Response {

        protected Response(Document document, Header[] headers) {
            this.document = document;
            this.headers = headers;
        }

        private Document document;
        private Header[] headers;
    }

    @Override
    public String toString() {
        return getHost();
    }
}
