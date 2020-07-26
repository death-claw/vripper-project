import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, Subject, Subscription} from 'rxjs';
import {environment} from 'src/environments/environment';
import {ServerService} from './server-service';
import {GrabQueueState} from './domain/grab-queue.model';
import {WSMessage} from './domain/ws-message.model';
import {PostDetails} from './domain/post-details.model';
import {PostState} from './domain/post-state.model';
import {LoggedUser} from './domain/logged-user.model';
import {DownloadSpeed} from './domain/download-speed.model';
import {GlobalState} from './domain/global-state.model';
import {map, share} from 'rxjs/operators';
import {RxStomp, RxStompConfig, RxStompState} from '@stomp/rx-stomp';
import {ElectronService} from 'ngx-electron';

@Injectable({
  providedIn: 'root'
})
export class WsConnectionService {
  rxStomp: RxStomp;
  private globalState: Observable<GlobalState>;
  private speed: Observable<DownloadSpeed>;
  private user: Observable<LoggedUser>;
  private posts: Observable<Array<PostState>>;
  private postDetails: Observable<Array<PostDetails>>;
  private queued: Observable<Array<GrabQueueState>>;
  private queuedRemove: Observable<Array<string>>;
  private postsRemove: Observable<Array<string>>;
  private postIdDetails: string;

  private connectionState$: Subject<RxStompState> = new BehaviorSubject(RxStompState.CLOSED);
  private stateSubscription: Subscription;

  constructor(private serverService: ServerService, private electronService: ElectronService) {
    this.init();
  }

  init() {
    if (this.electronService.isElectronApp) {
      const portRequest = setInterval(() => {
        this.electronService.ipcRenderer.send('get-port');
      }, 1000);

      // wait for MainIPC
      this.electronService.ipcRenderer.once('port', (event, port) => {
        clearInterval(portRequest);
        console.log('server running on port', port);
        this.serverService.baseUrl = 'http://localhost:' + port;
        this.serverService.wsBaseUrl = 'ws://localhost:' + port;
        this.connect();
        this.subscribe();
      });
    } else {
      this.serverService.baseUrl = environment.localhost;
      this.serverService.wsBaseUrl = environment.ws;
      this.connect();
      this.subscribe();
    }
  }

  connect() {
    const stompConfig: RxStompConfig = {
      connectHeaders: {
        Authorization: localStorage.getItem('auth') ? 'Bearer ' + localStorage.getItem('auth') : ''
      },
      brokerURL: this.serverService.wsBaseUrl + '/ws',
      reconnectDelay: 5000,
      // debug: function (str) {
      //   console.log('STOMP: ' + str);
      // },
    };
    this.rxStomp = new RxStomp();
    this.rxStomp.configure(stompConfig);
    this.rxStomp.activate();
  }

  public get state(): Observable<RxStompState> {
    return this.connectionState$.asObservable();
  }

  disconnect() {
    this.rxStomp.deactivate();
  }

  subscribe() {

    this.stateSubscription = this.rxStomp.connectionState$.subscribe(e => this.connectionState$.next(e));

    this.postsRemove = this.rxStomp.watch('/topic/posts/deleted').pipe(
      map(e => {
        return JSON.parse(e.body);
      }),
      share()
    );

    this.queuedRemove = this.rxStomp.watch('/topic/queued/deleted').pipe(
      map(e => {
        return JSON.parse(e.body);
      }),
      share()
    );

    this.globalState = this.rxStomp.watch('/topic/download-state').pipe(
      map(e => {
        const state: GlobalState = JSON.parse(e.body);
        return state;
      }),
      share()
    );

    this.speed = this.rxStomp.watch('/topic/speed').pipe(
      map(e => {
        return <DownloadSpeed>JSON.parse(e.body);
      }),
      share()
    );

    this.user = this.rxStomp.watch('/topic/user').pipe(
      map(e => {
        return <LoggedUser>JSON.parse(e.body);
      }),
      share()
    );

    this.posts = this.rxStomp.watch('/topic/posts').pipe(
      map(e => {
        const posts: Array<PostState> = [];
        (<Array<any>>JSON.parse(e.body)).forEach(element => {
          posts.push(
            new PostState(
              element.postId,
              element.title,
              element.done === 0 && element.total === 0 ? 0 : (element.done / element.total) * 100,
              element.status,
              element.removed,
              element.url,
              element.done,
              element.total,
              element.hosts,
              element.thanked,
              element.previews,
              element.metadata
            )
          );
        });
        return posts;
      }),
      share()
    );

    this.queued = this.rxStomp.watch('/topic/queued').pipe(
      map(e => {
        const grabQueue: Array<GrabQueueState> = [];
        (<Array<any>>JSON.parse(e.body)).forEach(element => {
          grabQueue.push(
            new GrabQueueState(element.type, element.link, element.threadId, element.postId, element.removed, element.total, element.loading)
          );
        });
        return grabQueue;
      }),
      share()
    );
  }

  get globalState$(): Observable<GlobalState> {
    return this.globalState;
  }

  get speed$(): Observable<DownloadSpeed> {
    return this.speed;
  }

  get user$(): Observable<LoggedUser> {
    return this.user;
  }

  get posts$(): Observable<PostState[]> {
    return this.posts;
  }

  get postsRemove$(): Observable<string[]> {
    return this.postsRemove;
  }

  get queuedRemove$(): Observable<string[]> {
    return this.queuedRemove;
  }

  postDetails$(postId: string): Observable<PostDetails[]> {
    if (this.postDetails != null && this.postIdDetails === postId) {
      return this.postDetails;
    }
    this.postIdDetails = postId;
    this.postDetails = this.rxStomp.watch('/topic/images/' + this.postIdDetails).pipe(
      map(e => {
        const postDetails: PostDetails[] = [];
        (<Array<any>>JSON.parse(e.body)).forEach(element => {
          postDetails.push(
            new PostDetails(
              element.postId,
              element.url,
              element.current === 0 && element.total === 0 ? 0 : (element.current / element.total) * 100,
              element.status,
              element.index
            )
          );
        });
        return postDetails;
      }),
      share()
    );
    return this.postDetails;
  }

  get queued$(): Observable<GrabQueueState[]> {
    return this.queued;
  }

  send(wsMessage: WSMessage) {
    this.rxStomp.publish({destination: wsMessage.destination, body: wsMessage.payload});
  }
}
