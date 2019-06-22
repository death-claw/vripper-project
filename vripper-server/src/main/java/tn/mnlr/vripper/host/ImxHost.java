package tn.mnlr.vripper.host;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ImxHost extends Host {

    private static final Logger logger = LoggerFactory.getLogger(ImxHost.class);

    private static final String host = "imx.to";
    public static final String CONTINUE_BUTTON_XPATH = "//div[@id='continuetoimage']";
    public static final String IMG_XPATH = "//img[@class='centred']";

    @Autowired
    private ConnectionManager cm;

    @Override
    protected String getHost() {
        return host;
    }

    @Override
    protected void setNameAndUrl(final String url, final ImageFileData imageFileData) throws HostException {

        Document doc = getDocument(url);

        Node contDiv;
        try {
            logger.info(String.format("Looking for xpath expression %s in %s", CONTINUE_BUTTON_XPATH, url));
            contDiv = xpathService.getAsNode(doc, CONTINUE_BUTTON_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        if (contDiv != null) {
            logger.info(String.format("Click button found for %s", url));
            HttpClient client = cm.getClient().build();
            HttpPost httpPost = cm.buildHttpPost(url);
            httpPost.addHeader("Referer", url);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("imgContinue", "Continue to image ... "));
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(params));
            } catch (Exception e) {
                throw new HostException(e);
            }
            logger.info(String.format("Requesting %s", httpPost));
            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(httpPost)) {
                logger.info(String.format("Cleaning response for %s", httpPost));
                doc = htmlProcessorService.clean(EntityUtils.toString(response.getEntity()));
                EntityUtils.consumeQuietly(response.getEntity());
            } catch (IOException | HtmlProcessorException e) {
                throw new HostException(e);
            }
        }

        Node imgNode;
        try {
            logger.info(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            imgNode = xpathService.getAsNode(doc, IMG_XPATH);
        } catch (XpathException e) {
            throw new HostException(e);
        }

        try {
            logger.info(String.format("Resolving name and image url for %s", url));
            String imgTitle = imgNode.getAttributes().getNamedItem("alt").getTextContent().trim();
            String imgUrl = imgNode.getAttributes().getNamedItem("src").getTextContent().trim();

            imageFileData.setImageUrl(imgUrl);
            imageFileData.setImageName(imgTitle.isEmpty() ? imgUrl.substring(imgUrl.lastIndexOf('/') + 1) : imgTitle);
        } catch(Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
