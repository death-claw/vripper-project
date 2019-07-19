import { WsConnectionService } from '../ws-connection.service';
import { PostState } from './post-state.model';
import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { Subscription } from 'rxjs';
import { WsHandler } from '../ws-handler';
import { ICellRendererParams } from 'ag-grid-community';

@Component({
  selector: 'app-progress-cell',
  template: `
    <div style="height: 100%;">
      <div class="progress-text" fxLayout="row" fxLayoutAlign="space-between">
        <div>{{ postState.title + postState.postCounter }}</div>
        <div>{{ trunc(postState.progress) + '%' }}</div>
      </div>
      <div
        class="progress-bar"
        [ngClass]="{
          'complete': postState.status === 'COMPLETE',
          'downloading': postState.status === 'DOWNLOADING',
          'stopped': postState.status === 'STOPPED',
          'partial': postState.status === 'PARTIAL',
          'error': postState.status === 'ERROR',
          'pending': postState.status === 'PENDING'
        }"
        [style.width]="trunc(postState.progress) + '%'"
      ></div>
      <div
        class="progress-bar-back"
        [ngClass]="{
          'complete-back': postState.status === 'COMPLETE',
          'downloading-back': postState.status === 'DOWNLOADING',
          'stopped-back': postState.status === 'STOPPED',
          'partial-back': postState.status === 'PARTIAL',
          'error-back': postState.status === 'ERROR',
          'pending-back': postState.status === 'PENDING'
        }"
      ></div>
    </div>
  `,
  styles: [
    `
      .progress-text {
        position: relative;
        padding: 0 8px;
        z-index: 2;
      }
      .progress-bar {
        position: relative;
        height: 100%;
        bottom: 45px;
        transition: width 0.5s, background-color 0.5s;
        z-index: 1;
      }
      .progress-bar-back {
        position: relative;
        height: 100%;
        bottom: 90px;
        transition: background-color 0.5s;
      }
      .complete {
        background-color: #3865a3;
      }
      .complete-back {
        background-color: lightgrey;
      }
      .pending {
        background-color: white;
      }
      .pending-back {
        background-color: lightgrey;
      }
      .error {
        background-color: #eb6060;
      }
      .error-back {
        background-color: #eb6060;
      }
      .downloading {
        background-color: #87a2c7;
      }
      .downloading-back {
        background-color: white;
      }
      .stopped {
        background-color: grey;
      }
      .stopped-back {
        background-color: lightgrey;
      }
      .partial {
        background-color: #ffcc00;
      }
      .partial-back {
        background-color: white;
      }
    `
  ]
})
export class PostProgressRendererComponent implements AgRendererComponent, OnInit, OnDestroy {
  constructor(private wsConnectionService: WsConnectionService, private zone: NgZone) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  params: ICellRendererParams;
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

  agInit(params: ICellRendererParams): void {
    this.params = params;
    this.postState = params.data;
  }

  refresh(params: ICellRendererParams): boolean {
    this.postState = params.data;
    return true;
  }
}
