package tn.mnlr.vripper.host;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.HtmlProcessorException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.q.ImageFileData;
import tn.mnlr.vripper.services.ConnectionManager;

import java.io.IOException;

@Service
public class ImageZillaHost extends Host {

    @Autowired
    private ConnectionManager cm;

    String host = "imagezilla.net";

    boolean https = false;

    @Override
    protected String getHost() {
        return host;
    }

    @Override
    protected void setNameAndUrl(String url, ImageFileData imageFileData) throws HostException {

        String basePage;

        HttpClient client = this.cm.getClient().build();
        HttpGet httpGet = this.cm.buildHttpGet(url);

        try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(httpGet)) {
            basePage = EntityUtils.toString(response.getEntity());
            EntityUtils.consumeQuietly(response.getEntity());
        } catch (IOException e) {
            throw new HostException(e);
        }

        Document doc = null;
        try {
            doc = htmlProcessorService.clean(basePage);
        } catch (HtmlProcessorException e) {
            throw new HostException(e);
        }
        String title = null;
        try {
            title = xpathService.getAsNode(doc, "//img[@id='photo']").getAttributes().getNamedItem("title").getTextContent().trim();
        } catch (XpathException e) {
            throw new HostException(e);
        }

        imageFileData.setImageUrl(url.replace("show", "images"));
        imageFileData.setImageName(title.substring(8));

    }

    @Override
    protected void setImageRequest(ImageFileData imageFileData) throws IOException {

        HttpGet httpGet = this.cm.buildHttpGet(imageFileData.getImageUrl());
        httpGet.addHeader("Referer", imageFileData.getPageUrl());
        imageFileData.setImageRequest(httpGet);
    }
}
