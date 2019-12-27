package tn.mnlr.vripper.q;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.AppStateService;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

@Service
public class DownloadQ {

    private static final Logger logger = LoggerFactory.getLogger(DownloadQ.class);

    @Autowired
    private AppStateService appStateService;

    private final ConcurrentHashMap<Host, BlockingDeque<DownloadJob>> downloadQ = new ConcurrentHashMap<>();
    @Autowired
    private AppSettingsService appSettingsService;
    @Autowired
    private List<Host> hosts;

    @PostConstruct
    private void init() {
        hosts.forEach(host -> downloadQ.put(host, new LinkedBlockingDeque<>()));
    }

    public void put(Image image) throws InterruptedException {
        logger.debug(String.format("Enqueuing a job for %s", image.getUrl()));
        image.init();
        DownloadJob downloadJob = new DownloadJob(image);
        downloadQ.get(downloadJob.getImage().getHost()).putLast(downloadJob);
        appStateService.newDownloadJob(downloadJob);
    }

    public void rePut(final DownloadJob downloadJob) throws InterruptedException {
        downloadQ.get(downloadJob.getImage().getHost()).putFirst(downloadJob);
    }

    public List<DownloadJob> take() throws Exception {
        if (hosts.size() == 0) {
            throw new Exception("No host available in th application");
        }
        List<DownloadJob> downloadJobs = new ArrayList<>();
        for (Host host : hosts) {
            for (int i = 0; i < appSettingsService.getMaxThreads(); i++) {
                DownloadJob downloadJob = downloadQ.get(host).pollFirst();
                if (downloadJob != null) {
                    downloadJobs.add(downloadJob);
                }
            }
        }
        return downloadJobs;
    }

    public void enqueue(Post post) throws InterruptedException {
        for (Image image : post.getImages()) {
            put(image);
        }
    }

    public int size() {
        return downloadQ.values().stream().mapToInt(BlockingDeque::size).sum();
    }

    public Iterable<? extends Map.Entry<Host, BlockingDeque<DownloadJob>>> entries() {
        return downloadQ.entrySet();
    }
}
