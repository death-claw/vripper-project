import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {WebSocketSubject} from 'rxjs/webSocket';
import {environment} from 'src/environments/environment';
import {ElectronService} from 'ngx-electron';
import {ServerService} from './server-service';
import {GrabQueueState} from './grab-queue/grab-queue.model';
import {WSMessage} from './common/ws-message.model';
import {PostDetails} from './post-detail/post-details.model';
import {PostState} from './posts/post-state.model';
import {LoggedUser} from './common/logged-user.model';
import {DownloadSpeed} from './common/download-speed.model';
import {GlobalState} from './common/global-state.model';
import {filter, map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class WsConnectionService {
  private websocket: WebSocketSubject<any>;
  private online: Subject<boolean> = new BehaviorSubject(false);

  private open = false;

  constructor(private electronService: ElectronService, private serverService: ServerService) {
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
      });
    } else {
      this.serverService.baseUrl = environment.localhost;
      this.serverService.wsBaseUrl = environment.ws;
      this.connect();
    }
  }

  connect() {
    console.log('Connecting...');
    if (this.websocket != null) {
      this.websocket.unsubscribe();
    }
    this.websocket = new WebSocketSubject({
      url: this.serverService.wsBaseUrl + '/endpoint',
      openObserver: {
        next: () => {
          console.log('Connection established');
          if (this.open !== true) {
            this.open = true;
            this.online.next(true);
          }
        }
      },
      closeObserver: {
        next: () => {
          if (this.open !== false) {
            this.open = false;
            this.online.next(false);
          }
          setTimeout(() => this.connect(), 1000);
        }
      }
    });
    this.websocket.subscribe();
  }

  public get state(): Observable<boolean> {
    return this.online.asObservable();
  }

  disconnect() {
    this.websocket.unsubscribe();
  }

  subscribeForGlobalState(): Observable<GlobalState[]> {
    return this.websocket.pipe(
      filter(e => e.length > 0 && e.filter(v => v.type === 'globalState').length > 0),
      map(e => {
        const state: Array<GlobalState> = [];
        (<Array<any>>e).forEach(element => {
          state.push(new GlobalState(element.running, element.queued, element.remaining, element.error));
        });
        return state;
      })
    );
  }

  subscribeForSpeed(): Observable<DownloadSpeed[]> {
    return this.websocket.pipe(
      filter(e => e.length > 0 && e.filter(v => v.type === 'downSpeed').length > 0),
      map(e => {
        const speed: Array<DownloadSpeed> = [];
        (<Array<any>>e).forEach(element => {
          speed.push(new DownloadSpeed(element.speed));
        });
        return speed;
      })
    );
  }

  subscribeForUser(): Observable<LoggedUser[]> {
    return this.websocket.pipe(
      filter(e => e.length > 0 && e.filter(v => v.type === 'user').length > 0),
      map(e => {
        const user: Array<LoggedUser> = [];
        (<Array<any>>e).forEach(element => {
          user.push(new LoggedUser(element.user));
        });
        return user;
      })
    );
  }

  subscribeForPosts(): Observable<PostState[]> {
    return this.websocket.pipe(
      filter(e => e.length > 0 && e.filter(v => v.type === 'post').length > 0),
      map(e => {
        const posts: Array<PostState> = [];
        (<Array<any>>e).forEach(element => {
          posts.push(
            new PostState(
              element.type,
              element.postId,
              element.title,
              element.done === 0 && element.total === 0 ? 0 : (element.done / element.total) * 100,
              element.status,
              element.removed,
              element.url,
              element.done,
              element.total,
              element.hosts,
              element.metadata.PREVIEWS,
              element.metadata.THANKED,
              element.metadata.RESOLVED_NAME
            )
          );
        });
        return posts;
      })
    );
  }

  subscribeForPostDetails(): Observable<PostDetails[]> {
    return this.websocket.pipe(
      filter(e => e.length > 0 && e[0].type === 'img'),
      map(e => {
        const values = [];
        (<Array<any>>e).forEach(element => {
          values.push(
            new PostDetails(
              element.postId,
              element.postName,
              element.url,
              element.current === 0 && element.total === 0 ? 0 : (element.current / element.total) * 100,
              element.status,
              element.index
            )
          );
        });
        return values;
      })
    );
  }

  subscribeForGrabQueue(): Observable<GrabQueueState[]> {
    return this.websocket.pipe(
      filter(e => e.length > 0 && e.filter(v => v.type === 'grabQueue').length > 0),
      map(e => {
        const grabQueue: Array<GrabQueueState> = [];
        (<Array<any>>e).forEach(element => {
          grabQueue.push(
            new GrabQueueState(element.type, element.link, element.threadId, element.postId, element.removed, element.count, element.loading)
          );
        });
        return grabQueue;
      })
    );
  }

  send(wsMessage: WSMessage) {
    this.websocket.next(wsMessage);
  }
}
