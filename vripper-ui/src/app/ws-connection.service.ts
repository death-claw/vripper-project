import { Injectable } from '@angular/core';
import { Observer, Subject, BehaviorSubject, ConnectableObservable, Observable, Subscription } from 'rxjs';
import { environment } from 'src/environments/environment';
import { multicast, map, filter } from 'rxjs/operators';
import { PostState } from './posts/post-state.model';
import { PostDetails } from './post-detail/post-details.model';
declare var SockJS;

export enum WSState {
  INIT, CONNECTING, ERROR, CLOSE, OPEN
}

@Injectable()
export class WsConnectionService {

  private websocket: Subject<any>;

  private state$: BehaviorSubject<WSState> = new BehaviorSubject(WSState.INIT);
  private sock;
  private _state: WSState = WSState.INIT;

  constructor() {
    this.tryConnect();
  }

  public get state(): Observable<WSState> {
    return this.state$.asObservable();
  }

  tryConnect() {
    this._state = WSState.CONNECTING;
    this.state$.next(this._state);
    this.sock = new SockJS(environment.localhost + '/endpoint');
    const observable = Observable.create((obs: Observer<string>) => {
      this.sock.onmessage = (message) => obs.next(message.data);
      this.sock.onerror = (error) => {
        obs.error(error);
        this._state = WSState.ERROR;
        this.state$.next(this._state);
      };
      this.sock.onclose = () => {
        obs.complete();
        this._state = WSState.CLOSE;
        this.state$.next(this._state);
      };
    });

    this.sock.onopen = () => {
      console.log('Sockjs connection established');
      this._state = WSState.OPEN;
      this.state$.next(this._state);
    };

    const observer = {
      next: (data: Object) => {
        if (this.sock.readyState === WebSocket.OPEN) {
          this.sock.send(JSON.stringify(data));
        }
      },
    };

    this.websocket = Subject.create(observer, observable).pipe(multicast(() => new Subject()));
    (<ConnectableObservable<any>><unknown>this.websocket).connect();
  }

  subscribeForPosts(callback: (stream: Array<PostState>) => void): Subscription {
    return this.websocket
      .pipe(
        map(e => JSON.parse(e)),
        filter(e => e.length > 0 && e[0].type === 'post'),
        map(e => {
          const values = [];
          (<Array<any>>e).forEach(element => {
            values.push(new PostState(
              element.postId,
              element.title,
              element.done === 0 && element.total === 0 ? 0 : (element.done / element.total) * 100,
              element.status)
            );
          });
          return values;
        })
      )
      .subscribe(e => {
        callback(e);
      });
  }

  subscribeForPostDetails(callback: (stream: Array<PostDetails>) => void): Subscription {

    return this.websocket
      .pipe(
        map(e => JSON.parse(e)),
        filter(e => e.length > 0 && e[0].type === 'img'),
        map(e => {
          const values = [];
          (<Array<any>>e).forEach(element => {
            values.push(new PostDetails(element.postId,
              element.postName,
              element.url,
              element.current === 0 && element.total === 0 ? 0 : (element.current / element.total) * 100,
              element.status));
          });
          return values;
        })
      )
      .subscribe(e => callback(e));
  }

  getConnection() {
    if (this.sock == null || this.sock.readyState === 3 || this.sock.readyState === 2) {
      this.tryConnect();
    }
    return this.websocket;
  }

  disconnect() {
    console.log('Sockjs disconnecting');
    this.sock.close();
  }
}
