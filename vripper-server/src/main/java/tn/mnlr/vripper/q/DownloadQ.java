package tn.mnlr.vripper.q;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.services.AppStateService;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Service
public class DownloadQ {

    private static final Logger logger = LoggerFactory.getLogger(DownloadQ.class);

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private ExecutionService executionService;

    private BlockingQueue<DownloadJob> downloadQ = new LinkedBlockingQueue<>();

    @Getter
    private boolean notPauseQ = true;

    public synchronized void put(Image image) throws InterruptedException {
        logger.info(String.format("Enqueuing a job for %s", image.getUrl()));
        DownloadJob downloadJob = new DownloadJob(image);
        downloadQ.put(downloadJob);
        appStateService.newDownloadJob(downloadJob);
    }

    public DownloadJob take() throws InterruptedException {
        DownloadJob downloadJob = downloadQ.take();
        logger.info(String.format("Retrieving a job for %s", downloadJob.getImage().getUrl()));
        return downloadJob;
    }

    public void enqueue(Post post) throws InterruptedException {

        for (Image image : post.getImages()) {
            put(image);
        }
    }

    public void restart(String postId) throws InterruptedException {
        if (appStateService.getRunningPosts().get(postId) != null && appStateService.getRunningPosts().get(postId).get() > 0) {
            logger.warn(String.format("Cannot restart, jobs are currently running for post id %s", postId));
            return;
        }
        List<Image> images = appStateService.getPost(postId)
                .getImages()
                .stream()
                .filter(e -> !e.getStatus().equals(Image.Status.COMPLETE))
                .collect(Collectors.toList());
        if (images.isEmpty()) {
            return;
        }
        appStateService.getPost(postId).setStatus(Post.Status.PENDING);
        logger.info(String.format("Restarting %d jobs for post id %s", images.size(), postId));
        for (Image image : images) {
            image.init();
            put(image);
        }
    }

    public void removeScheduled(Image image) {
        image.setStatus(Image.Status.STOPPED);
        logger.info(String.format("Removing scheduled job for %s", image.getUrl()));

        Iterator<DownloadJob> iterator = downloadQ.iterator();
        boolean removed = false;
        while(iterator.hasNext()) {
            DownloadJob next = iterator.next();
            if(next.getImage().getPostId().equals(image.getPostId())) {
                iterator.remove();
                appStateService.doneDownloadJob(image);
                logger.info(String.format("Scheduled job for %s is removed", image.getUrl()));
                removed = true;
                break;
            }
        }

        if(!removed) {
            logger.warn(String.format("Job for %s does not exist", image.getUrl()));
        }
    }

    public void removeRunning(String postId) {
        logger.info(String.format("Interrupting running jobs for post id %s", postId));
        executionService.stop(postId);
    }


    public synchronized void stop(String postId) {
        try {
            notPauseQ = false;
            appStateService.getPost(postId).setStatus(Post.Status.STOPPED);
            List<Image> images = appStateService.getPost(postId)
                    .getImages()
                    .stream()
                    .filter(e -> !e.getStatus().equals(Image.Status.COMPLETE))
                    .collect(Collectors.toList());
            if (images.isEmpty()) {
                return;
            }
            logger.info(String.format("Stopping %d jobs for post id %s", images.size(), postId));
            images.forEach(image -> removeScheduled(image));
            removeRunning(postId);
        } finally {
            notPauseQ = true;
        }
    }
}
