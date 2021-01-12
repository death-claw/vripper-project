package tn.mnlr.vripper.services.domain;

import lombok.Getter;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.jpa.domain.Queued;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ApiThreadHandler extends DefaultHandler {

    private final Queued queued;
    private final Collection<Host> supportedHosts;
    private final Map<Host, AtomicInteger> hostMap = new HashMap<>();
    @Getter
    private final List<MultiPostItem> posts = new ArrayList<>();
    private List<String> previews = new ArrayList<>();
    private String threadTitle;
    private String postId;
    private String postTitle;
    private int imageCount;
    private int postCounter;
    private int previewCounter = 0;

    public ApiThreadHandler(Queued queued) {
        this.queued = queued;
        this.supportedHosts = SpringContext.getBeansOfType(Host.class).values();
    }

    @Override
    public void startDocument() {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

        switch (qName.toLowerCase()) {
            case "thread":
                threadTitle = attributes.getValue("title").trim();
                break;
            case "post":
                imageCount = Integer.parseInt(attributes.getValue("imagecount").trim());
                postId = attributes.getValue("id").trim();
                postCounter = Integer.parseInt(attributes.getValue("number").trim());
                postTitle = Optional.ofNullable(attributes.getValue("title")).map(e -> e.trim().isEmpty() ? null : e.trim()).orElse(threadTitle);
                break;
            case "image":
                Optional.ofNullable(attributes.getValue("main_url"))
                        .map(String::trim)
                        .flatMap(mainUrl -> supportedHosts.stream().filter(host -> host.isSupported(mainUrl)).findFirst())
                        .ifPresent(host -> Optional.ofNullable(hostMap.get(host)).ifPresentOrElse(AtomicInteger::incrementAndGet, () -> hostMap.put(host, new AtomicInteger(1))));
                if (previewCounter++ < 4) {
                    Optional.ofNullable(attributes.getValue("thumb_url")).map(String::trim).ifPresent(previews::add);
                }
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("post".equalsIgnoreCase(qName)) {
            if (imageCount != 0) {
                posts.add(new MultiPostItem(
                        queued.getThreadId(),
                        postId,
                        postCounter,
                        postTitle,
                        imageCount,
                        String.format("https://vipergirls.to/threads/?p=%s&viewfull=1#post%s", postId, postId),
                        previews,
                        hostMap.entrySet().stream().filter(v -> v.getValue().get() > 0).map(e -> e.getKey().getHost() + " (" + e.getValue().get() + ")").collect(Collectors.joining(", "))
                ));
                queued.increment();
            }
            previewCounter = 0;
            previews = new ArrayList<>();
            hostMap.clear();
        }
    }

    @Override
    public void endDocument() {
        queued.done();
    }
}
