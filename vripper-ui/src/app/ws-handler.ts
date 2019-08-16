import { LoggedUser } from './common/logged-user.model';
import { Subject, Subscription } from 'rxjs';
import { WSMessage } from './common/ws-message.model';
import { PostState } from './posts/post-state.model';
import { map, filter } from 'rxjs/operators';
import { PostDetails } from './post-detail/post-details.model';
import { GlobalState } from './common/global-state.model';
import { DownloadSpeed } from './common/download-speed.model';
import { VRPostParse, VRThreadParseState } from './common/vr-post-parse.model';

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
                element.status
              )
            );
          });
          return values;
        })
      )
      .subscribe(e => callback(e));
  }

  subscribeForThreadParsing(
    callback: (stateArray: Array<VRThreadParseState>, dataArray: Array<VRPostParse>) => void
  ): Subscription {
    return this.websocket
      .pipe(
        map(e => JSON.parse(e)),
        filter(e => e.length > 0 && (e[0].type === 'postParse' || e[0].type === 'threadParseState')),
        map(e => {
          const stateValues: VRThreadParseState[] = [];
          const dataValues: VRPostParse[] = [];
          e.forEach(element => {
            if (element.type === 'postParse') {
              dataValues.push(
                new VRPostParse(
                  element.threadId,
                  element.postId,
                  element.number,
                  element.title,
                  element.imageCount,
                  element.url,
                  element.previews
                )
              );
            } else if (element.type === 'threadParseState') {
              stateValues.push(new VRThreadParseState(element.threadId, element.state));
            }
          });
          return { states: stateValues, data: dataValues };
        })
      )
      .subscribe(e => callback(e.states, e.data));
  }

  send(wsMessage: WSMessage) {
    this.websocket.next(wsMessage);
  }
}
