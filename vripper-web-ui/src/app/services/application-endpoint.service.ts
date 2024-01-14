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
import { QueueState } from '../domain/queue-state.model';
import { DownloadSpeed } from '../domain/download.speed';
import { ErrorCount } from '../domain/error.count';

@Injectable({
  providedIn: 'root',
})
export class ApplicationEndpointService {
  constructor(
    @Inject(WS_BASE_URL) private wsBaseUrl: string,
    @Inject(BASE_URL) private baseUrl: string,
    private httpClient: HttpClient
  ) {
    this.init();
  }

  private rxStomp!: RxStomp;

  connectionState = signal(RxStompState.CLOSED);

  private newPosts!: Observable<Post[]>;

  get newPosts$(): Observable<Post[]> {
    return this.newPosts;
  }

  private updatedPosts!: Observable<Post[]>;

  get updatedPosts$(): Observable<Post[]> {
    return this.updatedPosts;
  }

  private deletedPosts!: Observable<number[]>;

  get deletedPosts$(): Observable<number[]> {
    return this.deletedPosts;
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

  private newLogs!: Observable<Log[]>;

  get newLogs$(): Observable<Log[]> {
    return this.newLogs;
  }

  private updatedLogs!: Observable<Log[]>;

  get updatedLogs$(): Observable<Log[]> {
    return this.updatedLogs;
  }

  private logsRemove!: Observable<number[]>;

  get logsRemove$(): Observable<number[]> {
    return this.logsRemove;
  }

  postDetails$(postId: number): Observable<Image[]> {
    return this.rxStomp.watch('/topic/images/' + postId).pipe(
      map(e => {
        return JSON.parse(e.body).map((element: any) => {
          return new Image(
            element.postId,
            element.url,
            element.status,
            element.index + 1,
            element.downloaded,
            element.size
          );
        });
      }),
      share()
    );
  }

  init() {
    this.connect();
    this.prepareTopics();
  }

  destroy() {
    this.rxStomp.deactivate();
  }

  private queueState!: Observable<QueueState>;

  get queueState$(): Observable<QueueState> {
    return this.queueState;
  }

  private downloadSpeed!: Observable<DownloadSpeed>;

  get downloadSpeed$(): Observable<DownloadSpeed> {
    return this.downloadSpeed;
  }

  private vgUsername!: Observable<string>;

  get vgUsername$(): Observable<string> {
    return this.vgUsername;
  }

  private errorCount!: Observable<ErrorCount>;

  get errorCount$(): Observable<ErrorCount> {
    return this.errorCount;
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

    this.downloadSpeed = this.rxStomp.watch('/topic/download-speed').pipe(
      map(e => {
        return JSON.parse(e.body);
      }),
      share()
    );

    this.vgUsername = this.rxStomp.watch('/topic/vg-username').pipe(
      map(e => {
        return e.body;
      }),
      share()
    );

    this.errorCount = this.rxStomp.watch('/topic/error-count').pipe(
      map(e => {
        return JSON.parse(e.body);
      }),
      share()
    );

    this.deletedPosts = this.rxStomp.watch('/topic/posts/deleted').pipe(
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

    this.queueState = this.rxStomp.watch('/topic/queue-state').pipe(
      map(e => {
        const state: QueueState = JSON.parse(e.body);
        return state;
      }),
      share()
    );

    this.newPosts = this.rxStomp.watch('/topic/posts/new').pipe(
      map(e => {
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
            element.downloadDirectory,
            element.folderName,
            element.downloadFolder,
            element.downloaded
          );
        });
      }),
      share()
    );

    this.updatedPosts = this.rxStomp.watch('/topic/posts/updated').pipe(
      map(e => {
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
            element.downloadDirectory,
            element.folderName,
            element.downloadFolder,
            element.downloaded
          );
        });
      }),
      share()
    );

    this.newLogs = this.rxStomp.watch('/topic/logs/new').pipe(
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

    this.updatedLogs = this.rxStomp.watch('/topic/logs/updated').pipe(
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
          return new Thread(
            element.link,
            element.title,
            element.threadId,
            element.total
          );
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

  getThreadPosts(threadId: number) {
    return this.httpClient.get<PostItem[]>(
      this.baseUrl + `/api/grab/${threadId}`
    );
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

  renamePost(data: { postId: number; name: string }) {
    return this.httpClient.post<void>(this.baseUrl + '/api/post/rename', data);
  }
}
