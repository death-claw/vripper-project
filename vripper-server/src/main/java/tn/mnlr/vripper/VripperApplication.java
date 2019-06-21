package tn.mnlr.vripper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.host.ImageZillaHost;
import tn.mnlr.vripper.q.DownloadQ;
import tn.mnlr.vripper.services.AppStateService;

@SpringBootApplication
public class VripperApplication {

	public static void main(String[] args) {
		SpringApplication.run(VripperApplication.class, args);
	}

    @Component
    public class AppCommandRunner implements CommandLineRunner {

        @Autowired
        private AppStateService appStateService;

        @Autowired
        private ImageZillaHost imageZillaHost;

        @Autowired
        private DownloadQ downloadQ;

        private ObjectMapper om = new ObjectMapper();

        @Override
        public void run(String... args) throws Exception {
//			om.addMixIn(Image.class, ImageUIMixin.class).addMixIn(Post.class, PostUIMixin.class);
//
//			Disposable subscription = FlowableProcessor.merge(appStateService.getAllImageState(), appStateService.getLiveImageUpdates())
//					.observeOn(Schedulers.io())
//					.filter(e -> e.getPostId().equals("001"))
//					.buffer(2, TimeUnit.SECONDS)
//					.filter(e -> !e.isEmpty())
//					.map(e -> e.stream().distinct().collect(Collectors.toList()))
//					.map(om::writeValueAsString)
//					.map(TextMessage::new)
//					.doOnNext(msg -> System.out.println(msg.getPayload()))
//					.subscribe();
//
//			List<Image> images = Arrays.asList(appStateService.createImage("http://imagezilla.net/show/1wovAmO-zt4_0002.jpg", "001", "test", imageZillaHost));
//
//			Post post = appStateService.createPost("test", "https://vipergirls.to/threads/4537276-Casey-Set-9057-5600px-94X-(unreleased)", images, new HashMap<>(), "001");
//
//			downloadQ.enqueue(Arrays.asList(post));
//
////			Post post = appStateService.getPost("001");
//
//			post.getImages().get(0).init(appStateService);
//			downloadQ.put(post.getImages().get(0));
//
//			post.getImages().get(0).init(appStateService);
//			downloadQ.put(post.getImages().get(0));
//
//			post.getImages().get(0).init(appStateService);
//			downloadQ.put(post.getImages().get(0));
        }
    }

}

