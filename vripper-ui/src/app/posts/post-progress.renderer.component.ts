import { WsConnectionService } from '../ws-connection.service';
import { PostState } from './post-state.model';
import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { Subscription } from 'rxjs';
import { WsHandler } from '../ws-handler';

@Component({
  selector: 'app-progress-cell',
  template: `
    <div style="height: 100%;">
      <div
        class="progress-bar"
        [ngClass]="{
          complete: postState.status === 'COMPLETE',
          downloading: postState.status === 'DOWNLOADING',
          stopped: postState.status === 'STOPPED',
          partial: postState.status === 'PARTIAL'
        }"
        [style.width]="trunc(postState.progress) + '%'"
      ></div>
      <div
        class="progress-bar-back"
        fxLayout="row"
        fxLayoutAlign="space-between"
        [ngClass]="{ error: postState.status === 'ERROR', pending: postState.status === 'PENDING' }"
      >
        <div>{{ postState.title }}</div>
        <div>{{ trunc(postState.progress) + '%' }}</div>
      </div>
    </div>
  `,
  styles: [
    `
      .progress-bar-back {
        position: relative;
        height: 45px;
        bottom: 45px;
        padding: 0 8px;
        transition: background-color 0.5s;
      }
      .progress-bar {
        background-color: #87a2c7;
        height: 100%;
        transition: width 0.5s, background-color 0.5s;
      }
      .complete {
        background-color: #3865a3;
      }
      .pending {
        background-color: white;
      }
      .error {
        background-color: #eb6060;
      }
      .downloading {
        background-color: #87a2c7;
      }
      .stopped {
        background-color: grey;
      }
      .partial {
          background-color: #ffcc00;
      }
    `
  ]
})
export class PostProgressRendererComponent implements AgRendererComponent, OnInit, OnDestroy {
  constructor(
    private wsConnectionService: WsConnectionService,
    private zone: NgZone
    ) {
    this.websocketHandlerPromise = wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  params: any;
  postState: PostState;
  subscription: Subscription;

  trunc(value: number): number {
    return Math.trunc(value);
  }

  ngOnInit(): void {
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      this.subscription = handler.subscribeForPosts(e => {
        this.zone.run(() => {
          e.forEach(v => {
            if (this.postState.postId === v.postId) {
              this.postState = v;
            }
          });
        });
      });
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  agInit(params: any): void {
    this.params = params;
    this.postState = params.data;
  }

  refresh(params: any): boolean {
    this.postState = params.data;
    return true;
  }
}
