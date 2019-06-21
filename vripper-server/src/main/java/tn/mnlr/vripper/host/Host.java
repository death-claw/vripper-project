package tn.mnlr.vripper.host;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.AppSettings;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.exception.DownloadException;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.q.ImageFileData;
import tn.mnlr.vripper.services.ConnectionManager;
import tn.mnlr.vripper.services.HtmlProcessorService;
import tn.mnlr.vripper.services.XpathService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

@Service
abstract public class Host {

    @Autowired
    protected HtmlProcessorService htmlProcessorService;

    @Autowired
    protected XpathService xpathService;

    @Autowired
    private AppSettings appSettings;

    @Autowired
    private ConnectionManager cm;

    int readBufferSize = 8192;

    abstract protected String getHost();

    public boolean isSupported(String url) {
        return url.contains(getHost());
    }

    public void download(Image image, ImageFileData imageFileData) throws DownloadException, InterruptedException {

        try {

            imageFileData.setPageUrl(image.getUrl());

            /**
             * HOST SPECIFIC
             */
            setNameAndUrl(image.getUrl(), imageFileData);
            setImageRequest(imageFileData);
            /**
             * END HOST SPECIFIC
             */

            if(!imageFileData.getImageName().toLowerCase().endsWith(".jpg")) {
                imageFileData.setImageName(imageFileData.getImageName() + ".jpg");
            }

            File destinationFolder = new File(appSettings.getDownloadPath(), this.sanitize(image.getPostName()));
            if (!destinationFolder.exists()) {
                destinationFolder.mkdirs();
            }

            HttpClient client = this.cm.getClient().build();

            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(imageFileData.getImageRequest())) {

                if(response.getStatusLine().getStatusCode() / 100 != 2) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    throw new DownloadException(String.format("Server returned code %d", response.getStatusLine().getStatusCode()));
                }

                try (
                        InputStream downloadStream = response.getEntity().getContent();
                        FileOutputStream fos = new FileOutputStream(destinationFolder.getPath() + File.separator + imageFileData.getImageName())
                ) {

                    image.setTotal(response.getEntity().getContentLength());

                    byte[] buffer = new byte[readBufferSize];
                    int read;
                    while ((read = downloadStream.read(buffer, 0, readBufferSize)) != -1) {
//                                    randomFail();
                        fos.write(buffer, 0, read);
                        image.increase(read);
                    }
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
        } catch (Exception e) {
            if(Thread.interrupted()) {
                throw new InterruptedException("Download was interrupted");
            }
            throw new DownloadException(e);
        }
    }

    private void randomFail() throws Exception {
        Random random = new Random();
        int i = random.nextInt(100);
        if (i < 50) {
            throw new Exception("Purpose error");
        }
    }

    protected String sanitize(String folderName) {
        return folderName.replaceAll("\\.|\\\\|/|\\||:|\\?|\\*|\"|<|>|\\p{Cntrl}", "_");
    }

    protected abstract void setImageRequest(ImageFileData imageFileData) throws IOException;

    protected abstract void setNameAndUrl(String url, ImageFileData imageFileData) throws HostException;
}
