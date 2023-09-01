import { Inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { map, share } from 'rxjs/operators';
import { RxStomp, RxStompConfig, RxStompState } from '@stomp/rx-stomp';
import { Post } from '../domain/post.model';
import { BASE_URL, WS_BASE_URL } from '../base-url.token';
import { Thread } from '../domain/thread.model';
import { Log } from '../domain/log.model';
import { Image } from '../domain/image.model';
import { HttpClient } from '@angular/common/http';
import { PostItem } from '../domain/post-item.model';
import { Settings } from '../domain/settings.model';
import { GlobalState } from '../domain/global-state.model';

@Injectable({
  providedIn: 'root',
})
export class ApplicationEndpointService {
  private rxStomp!: RxStomp;

  private posts!: Observable<Post[]>;

  private globalState!: Observable<GlobalState>;
  connectionState = signal(RxStompState.CLOSED);

  get posts$(): Observable<Post[]> {
    return this.posts;
  }

  private postsRemove!: Observable<string[]>;

  get postsRemove$(): Observable<string[]> {
    return this.postsRemove;
  }

  private threads!: Observable<Thread[]>;

  get threads$(): Observable<Thread[]> {
    return this.threads;
  }

  private threadRemove!: Observable<string[]>;

  get threadRemove$(): Observable<string[]> {
    return this.threadRemove;
  }

  private threadRemoveAll!: Observable<void>;

  get threadRemoveAll$(): Observable<void> {
    return this.threadRemoveAll;
  }

  private logs!: Observable<Log[]>;

  get logs$(): Observable<Log[]> {
    return this.logs;
  }

  private logsRemove!: Observable<number[]>;

  get logsRemove$(): Observable<number[]> {
    return this.logsRemove;
  }

  postDetails$(postId: string): Observable<Image[]> {
    return this.rxStomp.watch('/topic/images/' + postId).pipe(
      map(e => {
        return JSON.parse(e.body).map((element: any) => {
          return new Image(
            element.postId,
            element.url,
            element.status,
            element.index + 1,
            element.current,
            element.total
          );
        });
      }),
      share()
    );
  }

  constructor(
    @Inject(WS_BASE_URL) private wsBaseUrl: string,
    @Inject(BASE_URL) private baseUrl: string,
    private httpClient: HttpClient
  ) {
    this.init();
  }

  init() {
    this.connect();
    this.prepareTopics();
  }

  destroy() {
    this.rxStomp.deactivate();
  }

  get globalState$(): Observable<GlobalState> {
    return this.globalState;
  }

  connect() {
    const stompConfig: RxStompConfig = {
      brokerURL: this.wsBaseUrl + '/ws',
      reconnectDelay: 200,
    };
    this.rxStomp = new RxStomp();
    this.rxStomp.configure(stompConfig);
    this.rxStomp.activate();
  }

  prepareTopics() {
    this.rxStomp.connectionState$.subscribe(e => this.connectionState.set(e));

    this.postsRemove = this.rxStomp.watch('/topic/posts/deleted').pipe(
      map(e => {
        return JSON.parse(e.body);
      }),
      share()
    );

    this.logsRemove = this.rxStomp.watch('/topic/logs/deleted').pipe(
      map(e => {
        return JSON.parse(e.body);
      }),
      share()
    );

    this.threadRemove = this.rxStomp.watch('/topic/threads/deleted').pipe(
      map(e => {
        return JSON.parse(e.body);
      }),
      share()
    );

    this.threadRemoveAll = this.rxStomp.watch('/topic/threads/deletedAll').pipe(
      map(() => {
        return;
      }),
      share()
    );

    this.globalState = this.rxStomp.watch('/topic/state').pipe(
      map(e => {
        const state: GlobalState = JSON.parse(e.body);
        return state;
      }),
      share()
    );

    this.posts = this.rxStomp.watch('/topic/posts').pipe(
      map(e => {
        // const posts: Array<Post> = [];
        return JSON.parse(e.body).map((element: any) => {
          return new Post(
            element.postId,
            element.postTitle,
            element.status,
            element.url,
            element.done,
            element.total,
            element.hosts,
            element.addedOn,
            element.rank + 1,
            element.downloadDirectory
          );
        });
        // return posts;
      }),
      share()
    );

    this.logs = this.rxStomp.watch('/topic/logs').pipe(
      map(e => {
        return JSON.parse(e.body).map((element: any) => {
          return new Log(
            element.id,
            element.type,
            element.status,
            element.time,
            element.message
          );
        });
      }),
      share()
    );

    this.threads = this.rxStomp.watch('/topic/threads').pipe(
      map(e => {
        return JSON.parse(e.body).map((element: any) => {
          return new Thread(element.link, element.threadId, element.total);
        });
      }),
      share()
    );
  }

  startPosts(posts: Post[]) {
    return this.httpClient.post<void>(
      this.baseUrl + '/api/post/restart',
      posts.map(e => e.postId)
    );
  }

  stopPosts(posts: Post[]) {
    return this.httpClient.post<void>(
      this.baseUrl + '/api/post/stop',
      posts.map(e => e.postId)
    );
  }

  getThreadPosts(threadId: string) {
    return this.httpClient
      .get<PostItem[]>(this.baseUrl + `/api/grab/${threadId}`)
      .pipe(map(v => v.map(p => ({ ...p, hosts: p.hosts }))));
  }

  startDownload() {
    return this.httpClient.post<void>(
      this.baseUrl + '/api/post/restart/all',
      {}
    );
  }

  stopDownload() {
    return this.httpClient.post<void>(this.baseUrl + '/api/post/stop/all', {});
  }

  links(links: string) {
    return this.httpClient.post<void>(this.baseUrl + '/api/post', { links });
  }

  settings() {
    return this.httpClient.get<Settings>(this.baseUrl + '/api/settings');
  }

  newSettings(settings: Settings) {
    return this.httpClient.post<Settings>(
      this.baseUrl + '/api/settings',
      settings
    );
  }

  download(items: PostItem[]) {
    return this.httpClient.post<void>(
      this.baseUrl + '/api/post/add',
      items.map(e => ({ threadId: e.threadId, postId: e.postId }))
    );
  }

  deletePosts(posts: Post[]) {
    return this.httpClient.post<void>(
      this.baseUrl + '/api/post/remove',
      posts.map(e => e.postId)
    );
  }

  deleteThreads(threads: Thread[]) {
    return this.httpClient.post<void>(
      this.baseUrl + '/api/grab/remove',
      threads.map(e => e.threadId)
    );
  }

  clearDownloads() {
    return this.httpClient.post<void>(this.baseUrl + '/api/post/clear/all', {});
  }

  clearLogs() {
    return this.httpClient.get<void>(this.baseUrl + '/api/events/clear');
  }

  clearThreads() {
    return this.httpClient.get<void>(this.baseUrl + '/api/grab/clear');
  }
}
