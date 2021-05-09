package tn.mnlr.vripper.download;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.PostService;
import tn.mnlr.vripper.services.SettingsService;
import tn.mnlr.vripper.services.domain.Settings;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DownloadService {

  private static final List<Status> FINISHED =
      Arrays.asList(Status.ERROR, Status.COMPLETE, Status.STOPPED);
  private final int MAX_POOL_SIZE = 12;

  private final ConcurrentHashMap<Host, AtomicInteger> threadCount = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newFixedThreadPool(MAX_POOL_SIZE);
  private final List<DownloadJob> running = Collections.synchronizedList(new ArrayList<>());
  private final List<DownloadJob> pending = new ArrayList<>();

  private final SettingsService settingsService;
  private final DataService dataService;
  private final PostService postService;

  private final List<Host> hosts;

  private boolean pauseQ = false;
  private Thread pollThread;

  @Autowired
  public DownloadService(
      SettingsService settingsService,
      DataService dataService,
      PostService postService,
      List<Host> hosts) {
    this.settingsService = settingsService;
    this.dataService = dataService;
    this.postService = postService;
    this.hosts = hosts;
  }

  @PostConstruct
  private void init() {
    pollThread = new Thread(this::start, "Polling thread");
    pollThread.start();
  }

  public void destroy() throws Exception {
    log.info("Shutting down ExecutionService");
    pollThread.interrupt();
    executor.shutdown();
    dataService
        .findAllPosts()
        .forEach(
            p -> {
              log.debug(String.format("Stopping download jobs for %s", p));
              this.stopRunning(p.getPostId());
            });
    executor.awaitTermination(5, TimeUnit.SECONDS);
  }

  private void stopRunning(@NonNull String postId) {
    List<DownloadJob> stopping = new ArrayList<>();
    Iterator<DownloadJob> iterator = running.iterator();
    while (iterator.hasNext()) {
      DownloadJob downloadJob = iterator.next();
      if (postId.equals(downloadJob.getPost().getPostId())) {
        downloadJob.stop();
        iterator.remove();
        stopping.add(downloadJob);
      }
    }

    while (!stopping.isEmpty()) {
      stopping.removeIf(DownloadJob::isFinished);
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void stopAll(List<String> posIds) {
    if (posIds != null) {
      posIds.forEach(this::stop);
    } else {
      dataService.findAllPosts().forEach(p -> this.stop(p.getPostId()));
    }
  }

  public void restartAll(List<String> posIds) {
    if (posIds != null) {
      posIds.forEach(this::restart);
    } else {
      dataService.findAllPosts().forEach(p -> this.restart(p.getPostId()));
    }
  }

  private void restart(@NonNull String postId) {
    if (isPending(postId)) {
      log.warn(String.format("Cannot restart, jobs are currently running for post id %s", postId));
      return;
    }
    List<Image> images = dataService.findByPostIdAndIsNotCompleted(postId);
    if (images.isEmpty()) {
      return;
    }
    Post post = dataService.findPostByPostId(postId).orElseThrow();
    post.setStatus(Status.PENDING);
    dataService.updatePostStatus(post.getStatus(), post.getId());
    log.debug(String.format("Restarting %d jobs for post id %s", images.size(), postId));
    enqueue(post, images);
  }

  private boolean isPending(String postId) {
    return pending.stream().anyMatch(p -> p.getPost().getPostId().equals(postId));
  }

  private boolean isRunning(String postId) {
    return running.stream().anyMatch(p -> p.getPost().getPostId().equals(postId));
  }

  private void stop(String postId) {
    try {
      pauseQ = true;
      final Post post = dataService.findPostByPostId(postId).orElseThrow();
      if (post == null) {
        return;
      }
      if (FINISHED.contains(post.getStatus())) {
        return;
      }
      pending.removeIf(p -> p.getPost().equals(post));
      stopRunning(postId);
      dataService.stopImagesByPostIdAndIsNotCompleted(postId);
      dataService.finishPost(post);
      postService.stopFetchingMetadata(post);
    } finally {
      pauseQ = false;
    }
  }

  private boolean canRun(Host host) {
    boolean canRun;
    int totalRunning = threadCount.values().stream().mapToInt(AtomicInteger::get).sum();
    canRun =
        threadCount.get(host).get() < settingsService.getSettings().getMaxThreads()
            && (settingsService.getSettings().getMaxTotalThreads() == 0
                ? totalRunning < MAX_POOL_SIZE
                : totalRunning < settingsService.getSettings().getMaxTotalThreads());
    if (canRun && !pauseQ) {
      threadCount.get(host).incrementAndGet();
      return true;
    }
    return false;
  }

  private Map<Host, Integer> candidateCount() {
    HashMap<Host, Integer> map = new HashMap<>();
    hosts.forEach(
        h -> {
          AtomicInteger count = threadCount.get(h);
          if (count == null) {
            count = new AtomicInteger(0);
            threadCount.put(h, count);
          }
          map.put(h, settingsService.getSettings().getMaxThreads() - count.get());
        });
    return map;
  }

  private List<DownloadJob> getCandidates(Map<Host, Integer> candidateCount, int max) {
    int i = 0;
    List<DownloadJob> candidates = new ArrayList<>();
    for (DownloadJob downloadJob : pending) {
      Host host = downloadJob.getImage().getHost();
      Integer maxPerHost = candidateCount.get(host);
      if (maxPerHost > 0 && i < max) {
        candidates.add(downloadJob);
        candidateCount.put(host, maxPerHost + 1);
        i++;
      }
      if (i >= max) {
        break;
      }
    }
    return candidates;
  }

  public void enqueue(Post post, Collection<Image> imageList) {
    synchronized (this) {
      for (Image image : imageList) {
        log.debug(String.format("Enqueuing a job for %s", image.getUrl()));
        image.init();
        dataService.updateImageStatus(image.getStatus(), image.getId());
        dataService.updateImageCurrent(image.getCurrent(), image.getId());
        DownloadJob downloadJob =
            new DownloadJob(post, image, (Settings) settingsService.getSettings().clone());
        pending.add(downloadJob);
      }
      pending.sort(Comparator.comparing(e -> e.getPost().getAddedOn()));

      this.notify();
    }
  }

  private void start() {
    while (!Thread.interrupted()) {
      try {
        synchronized (this) {
          List<DownloadJob> accepted = new ArrayList<>();
          List<DownloadJob> candidates = getCandidates(candidateCount(), MAX_POOL_SIZE);
          candidates.forEach(
              c -> {
                if (canRun(c.getImage().getHost())) {
                  accepted.add(c);
                }
              });
          pending.removeAll(accepted);
          accepted.forEach(this::push);
          accepted.clear();
          this.wait();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private void push(DownloadJob downloadJob) {
    log.debug(String.format("Scheduling a job for %s", downloadJob.getImage().getUrl()));
    executor.execute(new DownloadJobWrapper(downloadJob));
    running.add(downloadJob);
  }

  public void afterJobFinish(DownloadJob downloadJob) {
    synchronized (this) {
      running.remove(downloadJob);
      threadCount.get(downloadJob.getImage().getHost()).decrementAndGet();
      if (!isPending(downloadJob.getPost().getPostId())
          && !isRunning(downloadJob.getPost().getPostId())) {
        dataService.finishPost(downloadJob.getPost());
      }
      this.notify();
    }
  }

  public int pendingCount() {
    return pending.size();
  }

  public int runningCount() {
    return running.size();
  }
}
