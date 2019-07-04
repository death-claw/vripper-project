import { Subject, Subscription } from 'rxjs';
import { WSMessage } from './common/ws-message.model';
import { PostState } from './posts/post-state.model';
import { map, filter } from 'rxjs/operators';
import { PostDetails } from './post-detail/post-details.model';
import { Action } from './common/action.model';

export class WsHandler {
  constructor(private websocket: Subject<any>) {}

  subscribeForPosts(callback: (postStream: Array<PostState>, actionStream: Array<Action>) => void): Subscription {
    return this.websocket
      .pipe(
        map(e => JSON.parse(e)),
        filter(e => e.length > 0 && e.filter(v => v.type === 'post' || v.type === 'action').length > 0),
        map(e => {
          const posts: Array<PostState> = [];
          const actions: Array<Action> = [];
          (<Array<any>>e).forEach(element => {
            if (element.type === 'post') {
              posts.push(
                new PostState(
                  element.type,
                  element.postId,
                  element.title,
                  element.done === 0 && element.total === 0 ? 0 : (element.done / element.total) * 100,
                  element.status
                )
              );
            } else if (element.type === 'action') {
              actions.push(new Action(element.type, element.stateAction, element.payload));
            }
          });
          return { posts: posts, actions: actions };
        })
      )
      .subscribe(e => {
        callback(e.posts, e.actions);
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

  send(wsMessage: WSMessage) {
    this.websocket.next(wsMessage);
  }
}
