package tn.mnlr.vripper.host;

import lombok.Getter;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Random;

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
    private ConnectionManager cm;

    @Autowired
    private DownloadSpeedService downloadSpeedService;

    @Autowired
    private PathService pathService;

    abstract public String getHost();

    public boolean isSupported(String url) {
        return url.contains(getHost());
    }

    public void download(Image image, ImageFileData imageFileData) throws DownloadException, InterruptedException {

        try {

            imageFileData.setPageUrl(image.getUrl());

            /**
             * HOST SPECIFIC
             */
            logger.info(String.format("Getting image url and name from %s using %s", image.getUrl(), this.getHost()));
            setNameAndUrl(image.getUrl(), imageFileData);
            logger.info(String.format("Resolved name for %s: %s", image.getUrl(), imageFileData.getImageName()));
            logger.info(String.format("Resolved image url for %s: %s", image.getUrl(), imageFileData.getImageUrl()));

            logger.info(String.format("Building image request for %s", image.getUrl()));
            setImageRequest(imageFileData);
            /**
             * END HOST SPECIFIC
             */

            String formatImageFileName = pathService.formatImageFileName(imageFileData.getImageName());
            logger.info(String.format("Sanitizing image name from %s to %s", imageFileData.getImageName(), formatImageFileName));
            imageFileData.setImageName(formatImageFileName);
            File destinationFolder = pathService.getDownloadDestinationFolder(image.getPostId());
            logger.info(String.format("Saving to %s", destinationFolder.getPath()));
            if (!destinationFolder.exists()) {
                logger.info(String.format("Creating %s", destinationFolder.getPath()));
                destinationFolder.mkdirs();
            }

            HttpClient client = cm.getClient().build();

            logger.info(String.format("Downloading %s", imageFileData.getImageUrl()));
            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(imageFileData.getImageRequest())) {

                if(response.getStatusLine().getStatusCode() / 100 != 2) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    throw new DownloadException(String.format("Server returned code %d", response.getStatusLine().getStatusCode()));
                }

                File outputFile = new File(destinationFolder.getPath() + File.separator + imageFileData.getImageName() + ".tmp");
                InputStream downloadStream = response.getEntity().getContent();
                FileOutputStream fos = new FileOutputStream(outputFile);
                try {
                    image.setTotal(response.getEntity().getContentLength());
                    logger.info(String.format("%s length is %d", imageFileData.getImageUrl(), image.getTotal()));
                    logger.info(String.format("Starting data transfer for %s", imageFileData.getImageUrl()));

                    byte[] buffer = new byte[READ_BUFFER_SIZE];
                    int read;
                    while ((read = downloadStream.read(buffer, 0, READ_BUFFER_SIZE)) != -1) {
                        fos.write(buffer, 0, read);
                        image.increase(read);
                        downloadSpeedService.increase(read);
                    }
                    EntityUtils.consumeQuietly(response.getEntity());
                } finally {
                    if (downloadStream != null) {
                        downloadStream.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                }
                checkImageTypeAndRename(outputFile, imageFileData.getImageName(), image.getIndex());
            }
        } catch (Exception e) {
            if(Thread.interrupted()) {
                throw new InterruptedException("Download was interrupted");
            }
            throw new DownloadException(e);
        }
    }

    private void checkImageTypeAndRename(File outputFile, String imageName, int index) throws HostException {
        String formatName;
        try (ImageInputStream iis = ImageIO.createImageInputStream(outputFile)) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
            if (!it.hasNext()) {
                throw new HostException("Image file is not recognized!");
            }
            ImageReader reader = it.next();
            formatName = reader.getFormatName();
            if (formatName.toUpperCase().equals("JPEG")) {
                formatName = "jpg";
            }
        } catch (Exception e) {
            throw new HostException("Failed to guess image format", e);
        }
        try {
            File outImage = new File(outputFile.getParent(), (appSettingsService.isForceOrder() ? String.format("%03d_", index) : "") + imageName + "." + formatName.toLowerCase());
            if (outImage.exists()) {
                outImage.delete();
            }
            Files.move(outputFile.toPath(), outImage.toPath());
        } catch (Exception e) {
            throw new HostException("Failed to rename the image", e);
        }
    }

    /**
     * Just for testing, you may ignore
     * @throws Exception
     */
    private void randomFail() throws Exception {
        Random random = new Random();
        int i = random.nextInt(100);
        if (i < 50) {
            throw new Exception("Purpose error");
        }
    }

    protected final String getDefaultImageName(final String imgUrl) {
        String imageTitle  = imgUrl.substring(imgUrl.lastIndexOf('/') + 1);
        logger.debug(String.format("Extracting name from url %s: %s", imgUrl, imageTitle));
        return imgUrl;
    }

    protected final Response getResponse(final String url) throws HostException {
        String basePage;

        HttpClient client = cm.getClient().build();
        HttpGet httpGet = cm.buildHttpGet(url);
        Header[] headers;
        logger.info(String.format("Requesting %s", url));
        try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(httpGet)) {
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
            logger.info(String.format("Cleaning %s response", url));
            return new Response(htmlProcessorService.clean(basePage), headers);
        } catch (HtmlProcessorException e) {
            throw new HostException(e);
        }
    }

    protected void setImageRequest(final ImageFileData imageFileData) {
        HttpGet httpGet = cm.buildHttpGet(imageFileData.getImageUrl());
        httpGet.addHeader("Referer", imageFileData.getPageUrl());
        imageFileData.setImageRequest(httpGet);
    }

    protected abstract void setNameAndUrl(final String url, final ImageFileData imageFileData) throws HostException;

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
