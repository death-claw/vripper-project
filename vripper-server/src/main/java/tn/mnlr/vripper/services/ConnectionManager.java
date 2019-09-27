package tn.mnlr.vripper.services;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class ConnectionManager {

    private ConnectionManager() {
        buildConnectionPool();
    }

    private PoolingHttpClientConnectionManager pcm;

    private RequestConfig rc = RequestConfig.custom()
            .setConnectionRequestTimeout(10_000)
            .setConnectTimeout(10_000)
            .setSocketTimeout(10_000)
            .setCookieSpec(CookieSpecs.STANDARD)
            .build();

    private void buildConnectionPool() {

        pcm = new PoolingHttpClientConnectionManager();
        pcm.setMaxTotal(200);
        pcm.setDefaultMaxPerRoute(10);
    }

    public HttpClientBuilder getClient() {
        return HttpClients.custom()
                .setConnectionManager(pcm)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setRetryHandler(new DefaultHttpRequestRetryHandler(5, true))
                .setDefaultRequestConfig(rc);
    }

    public HttpGet buildHttpGet(String url) {
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        return httpGet;
    }

    public HttpPost buildHttpPost(String url) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        return httpPost;
    }

    public HttpGet buildHttpGet(URI uri) {
        HttpGet httpGet = new HttpGet(uri);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        return httpGet;
    }
}
