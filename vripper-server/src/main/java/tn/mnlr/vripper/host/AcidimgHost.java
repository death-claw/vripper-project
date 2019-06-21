package tn.mnlr.vripper.host;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.HtmlProcessorException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.q.ImageFileData;
import tn.mnlr.vripper.services.ConnectionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AcidimgHost extends Host {

    String host = "acidimg.cc";
    boolean https = true;

    @Autowired
    private ConnectionManager cm;

    @Override
    protected String getHost() {
        return host;
    }

    @Override
    protected void setNameAndUrl(final String url, final ImageFileData imageFileData) throws HostException {

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

        Node contDiv = null;
        try {
            contDiv = xpathService.getAsNode(doc, "//input[@id='continuebutton']");
        } catch (XpathException e) {
            e.printStackTrace();
        }

        if (contDiv != null) {
            client = this.cm.getClient().build();
            HttpPost httpPost = this.cm.buildHttpPost(url);
            httpPost.addHeader("Referer", url);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("imgContinue", "Continue to your image"));
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(params));
            } catch (Exception e) {
                throw new HostException(e);
            }

            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(httpPost)) {
                doc = htmlProcessorService.clean(EntityUtils.toString(response.getEntity()));
                EntityUtils.consumeQuietly(response.getEntity());
            } catch (IOException | HtmlProcessorException e) {
                throw new HostException(e);
            }
        }

        Node imgNode = null;
        try {
            imgNode = xpathService.getAsNode(doc, "//img[@class='centred']");
        } catch (XpathException e) {
            throw new HostException(e);
        }

        String imgTitle = imgNode.getAttributes().getNamedItem("alt").getTextContent().trim().concat(".jpg");
        String imgUrl = imgNode.getAttributes().getNamedItem("src").getTextContent().trim();

        imageFileData.setImageUrl(imgUrl);
        imageFileData.setImageName(imgTitle.isEmpty() ? imgUrl.substring(imgUrl.lastIndexOf('/') + 1) : imgTitle);

    }

    @Override
    protected void setImageRequest(ImageFileData imageFileData) throws IOException {

//        HttpClient client = this.cm.getClient().build();
        HttpGet httpGet = this.cm.buildHttpGet(imageFileData.getImageUrl());
        httpGet.addHeader("Referer", imageFileData.getPageUrl());
        imageFileData.setImageRequest(httpGet);
    }
}
