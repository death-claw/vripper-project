import { element } from 'protractor';
import { GrabQueueState } from './grab-queue/grab-queue.model';
import { LoggedUser } from './common/logged-user.model';
import { Subject, Subscription } from 'rxjs';
import { WSMessage } from './common/ws-message.model';
import { PostState } from './posts/post-state.model';
import { map, filter } from 'rxjs/operators';
import { PostDetails } from './post-detail/post-details.model';
import { GlobalState } from './common/global-state.model';
import { DownloadSpeed } from './common/download-speed.model';

export class WsHandler {
  constructor(private websocket: Subject<any>) {}

  subscribeForGlobalState(callback: (stateStream: Array<GlobalState>) => void): Subscription {
    return this.websocket
      .pipe(
        map(e => JSON.parse(e)),
        filter(e => e.length > 0 && e.filter(v => v.type === 'globalState').length > 0),
        map(e => {
          const state: Array<GlobalState> = [];
          (<Array<any>>e).forEach(element => {
            state.push(new GlobalState(element.running, element.queued, element.remaining, element.error));
          });
          return state;
        })
      )
      .subscribe(e => {
        callback(e);
      });
  }

  subscribeForSpeed(callback: (speedStream: Array<DownloadSpeed>) => void): Subscription {
    return this.websocket
      .pipe(
        map(e => JSON.parse(e)),
        filter(e => e.length > 0 && e.filter(v => v.type === 'downSpeed').length > 0),
        map(e => {
          const speed: Array<DownloadSpeed> = [];
          (<Array<any>>e).forEach(element => {
            speed.push(new DownloadSpeed(element.speed));
          });
          return speed;
        })
      )
      .subscribe(e => {
        callback(e);
      });
  }

  subscribeForUser(callback: (userStream: Array<LoggedUser>) => void): Subscription {
    return this.websocket
      .pipe(
        map(e => JSON.parse(e)),
        filter(e => e.length > 0 && e.filter(v => v.type === 'user').length > 0),
        map(e => {
          const user: Array<LoggedUser> = [];
          (<Array<any>>e).forEach(element => {
            user.push(new LoggedUser(element.user));
          });
          return user;
        })
      )
      .subscribe(e => {
        callback(e);
      });
  }

  subscribeForPosts(callback: (postStream: Array<PostState>) => void): Subscription {
    return this.websocket
      .pipe(
        map(e => JSON.parse(e)),
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
                element.metadata.PREVIEWS
              )
            );
          });
          return posts;
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
      )
      .subscribe(e => callback(e));
  }

  subscribeForGrabQueue(callback: (grabQueueStream: Array<GrabQueueState>) => void): Subscription {
    return this.websocket
      .pipe(
        map(e => JSON.parse(e)),
        filter(e => e.length > 0 && e.filter(v => v.type === 'grabQueue').length > 0),
        map(e => {
          const grabQueue: Array<GrabQueueState> = [];
          (<Array<any>>e).forEach(element => {
            grabQueue.push(
              new GrabQueueState(element.type, element.link, element.threadId, element.postId, element.removed)
            );
          });
          return grabQueue;
        })
      )
      .subscribe(e => {
        callback(e);
      });
  }

  send(wsMessage: WSMessage) {
    this.websocket.next(wsMessage);
  }
}
