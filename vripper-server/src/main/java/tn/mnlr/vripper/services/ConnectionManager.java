package tn.mnlr.vripper.services;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
public class ConnectionManager {

    private ConnectionManager() {
        buildConnectionPool();
    }

    private PoolingHttpClientConnectionManager pcm;

    private RequestConfig rc = RequestConfig.custom()
            .setConnectionRequestTimeout(5_000)
            .setConnectTimeout(5_000)
            .setSocketTimeout(5_000)
            .setCookieSpec(CookieSpecs.STANDARD)
            .build();

    private void buildConnectionPool() {

        pcm = new PoolingHttpClientConnectionManager();
        pcm.setMaxTotal(50);
        pcm.setDefaultMaxPerRoute(4);
    }

    @Scheduled(fixedDelay = 1000)
    private void idleConnectionMonitoring() {
        pcm.closeExpiredConnections();
        pcm.closeIdleConnections(30, TimeUnit.SECONDS);
    }

    public HttpClientBuilder getClient() {
        return HttpClients.custom()
                .setConnectionManager(pcm)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .disableAutomaticRetries()
                .setDefaultRequestConfig(rc);
    }

    public HttpGet buildHttpGet(String url) {
        HttpGet httpGet = new HttpGet(url.replace(" ", "+"));
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        return httpGet;
    }

    public HttpPost buildHttpPost(String url) {
        HttpPost httpPost = new HttpPost(url.replace(" ", "+"));
        httpPost.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        return httpPost;
    }

    public HttpGet buildHttpGet(URI uri) {
        HttpGet httpGet = new HttpGet(uri);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        return httpGet;
    }
}
