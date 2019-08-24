import { Injectable } from '@angular/core';
import { Observer, Subject, BehaviorSubject, ConnectableObservable, Observable, Subscription } from 'rxjs';
import { environment } from 'src/environments/environment';
import { multicast } from 'rxjs/operators';
import { ElectronService } from 'ngx-electron';
import { WsHandler } from './ws-handler';
import { ServerService } from './server-service';
declare var SockJS;

export enum WSState {
  INIT,
  CONNECTING,
  ERROR,
  CLOSE,
  OPEN
}

const maxAttemps = 5;

@Injectable()
export class WsConnectionService {
  private websocket: Subject<any>;
  private state$: BehaviorSubject<WSState> = new BehaviorSubject(WSState.INIT);
  private sock;
  private _state: WSState = WSState.INIT;
  private wsHandlerPromise: Promise<WsHandler>;
  private wsHandler: WsHandler;

  constructor(private electronService: ElectronService, private serverService: ServerService) {
    this.wsHandlerPromise = new Promise((resolve, reject) => {
      if (this.wsHandler != null) {
        return this.wsHandler;
      }
      if (this.electronService.isElectronApp) {
        this.electronService.ipcRenderer.send('get-port');

        // wait for MainIPC
        this.electronService.ipcRenderer.once('port', (event, port) => {
          console.log('server running on port', port);
          this.serverService.baseUrl = 'http://localhost:' + port;
          this.tryConnect(resolve, reject);
        });
      } else {
        this.serverService.baseUrl = environment.localhost;
        this.tryConnect(resolve, reject);
      }
    });
  }

  public get state(): Observable<WSState> {
    return this.state$.asObservable();
  }

  tryConnect(resolve, reject) {
    let attempts = 0;
    this.connect();
    const subscription: Subscription = this.state$.subscribe(e => {
      if (e === WSState.OPEN) {
        this.wsHandler = new WsHandler(this.websocket);
        resolve(this.wsHandler);
      }
    });
    const interval = setInterval(() => {
      if (this._state !== WSState.OPEN && attempts >= maxAttemps) {
        clearInterval(interval);
        this._state = WSState.ERROR;
        this.state$.next(this._state);
        reject('Failed to connect to server');
        if (subscription !=  null) {
          subscription.unsubscribe();
        }
        return;
      } else if (this._state === WSState.OPEN) {
        clearInterval(interval);
        if (subscription !=  null) {
          subscription.unsubscribe();
        }
        return;
      }
      this.connect();
      attempts++;
    }, 5000);
  }

  connect() {
    this._state = WSState.CONNECTING;
    this.state$.next(this._state);
    this.sock = new SockJS(this.serverService.baseUrl + '/endpoint');
    const observable = Observable.create((obs: Observer<string>) => {
      this.sock.onmessage = message => obs.next(message.data);
      this.sock.onerror = error => {
        console.log('Sockjs error', error);
        obs.error(error);
        this._state = WSState.ERROR;
        this.state$.next(this._state);
      };
      this.sock.onclose = () => {
        console.log('Sockjs disconnected');
        obs.complete();
        this._state = WSState.CLOSE;
        this.state$.next(this._state);
      };
    });

    this.sock.onopen = () => {
      console.log('Sockjs connection established');
      const observer = {
        next: (data: Object) => {
          if (this.sock.readyState === WebSocket.OPEN) {
            this.sock.send(JSON.stringify(data));
          }
        }
      };
      this.websocket = Subject.create(observer, observable).pipe(multicast(() => new Subject()));
      (<ConnectableObservable<any>>(<unknown>this.websocket)).connect();
      this._state = WSState.OPEN;
      this.state$.next(this._state);
    };
  }

  getConnection(): Promise<WsHandler> {
    return this.wsHandlerPromise;
  }

  disconnect() {
    console.log('Sockjs disconnecting');
    this.sock.close();
  }
}
