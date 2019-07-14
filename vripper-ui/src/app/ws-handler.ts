import { Subject, Subscription } from 'rxjs';
import { WSMessage } from './common/ws-message.model';
import { PostState } from './posts/post-state.model';
import { map, filter } from 'rxjs/operators';
import { PostDetails } from './post-detail/post-details.model';

export class WsHandler {
  constructor(private websocket: Subject<any>) {}

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
                element.removed
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

  send(wsMessage: WSMessage) {
    this.websocket.next(wsMessage);
  }
}
