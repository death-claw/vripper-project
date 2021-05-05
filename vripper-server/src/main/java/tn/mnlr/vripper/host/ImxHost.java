package tn.mnlr.vripper.host;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.HtmlProcessorException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.services.ConnectionService;
import tn.mnlr.vripper.services.HostService;
import tn.mnlr.vripper.services.HtmlProcessorService;
import tn.mnlr.vripper.services.XpathService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ImxHost extends Host {

  private static final String host = "imx.to";
  private static final String CONTINUE_BUTTON_XPATH = "//*[@name='imgContinue']";
  private static final String IMG_XPATH = "//img[@class='centred']";

  private final HostService hostService;
  private final XpathService xpathService;
  private final ConnectionService cm;
  private final HtmlProcessorService htmlProcessorService;

  @Autowired
  public ImxHost(
      HostService hostService,
      XpathService xpathService,
      ConnectionService cm,
      HtmlProcessorService htmlProcessorService) {
    this.hostService = hostService;
    this.xpathService = xpathService;
    this.cm = cm;
    this.htmlProcessorService = htmlProcessorService;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public String getLookup() {
    return host;
  }

  @Override
  public HostService.NameUrl getNameAndUrl(final String _url, final HttpClientContext context)
      throws HostException {

    String url = _url.replace("http://", "https://");
    HostService.Response resp = hostService.getResponse(url, context);
    Document doc = resp.getDocument();

    Node contDiv;
    String value = null;
    try {
      log.debug(String.format("Looking for xpath expression %s in %s", CONTINUE_BUTTON_XPATH, url));
      contDiv = xpathService.getAsNode(doc, CONTINUE_BUTTON_XPATH);
      if (contDiv == null) {
        throw new HostException(CONTINUE_BUTTON_XPATH + " cannot be found");
      }
      Node node = contDiv.getAttributes().getNamedItem("value");
      if (node != null) {
        value = node.getTextContent();
      }
    } catch (XpathException e) {
      throw new HostException(e);
    }

    if (value == null) {
      throw new HostException("Failed to obtain value attribute from continue input");
    }
    log.debug(String.format("Click button found for %s", url));
    HttpClient client = cm.getClient().build();
    HttpPost httpPost = cm.buildHttpPost(url, context);
    List<NameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("imgContinue", value));
    try {
      httpPost.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new HostException(e);
    }
    log.debug(String.format("Requesting %s", httpPost));
    try (CloseableHttpResponse response =
        (CloseableHttpResponse) client.execute(httpPost, context)) {
      log.debug(String.format("Cleaning response for %s", httpPost));
      doc = htmlProcessorService.clean(EntityUtils.toString(response.getEntity()));
      EntityUtils.consumeQuietly(response.getEntity());
    } catch (IOException | HtmlProcessorException e) {
      throw new HostException(e);
    }

    Node imgNode;
    try {
      log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
      imgNode = xpathService.getAsNode(doc, IMG_XPATH);
    } catch (XpathException e) {
      throw new HostException(e);
    }

    try {
      log.debug(String.format("Resolving name and image url for %s", url));
      String imgTitle = imgNode.getAttributes().getNamedItem("alt").getTextContent().trim();
      String imgUrl = imgNode.getAttributes().getNamedItem("src").getTextContent().trim();

      return new HostService.NameUrl(
          imgTitle.isEmpty() ? imgUrl.substring(imgUrl.lastIndexOf('/') + 1) : imgTitle, imgUrl);
    } catch (Exception e) {
      throw new HostException("Unexpected error occurred", e);
    }
  }
}
