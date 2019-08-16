import { WsConnectionService } from '../ws-connection.service';
import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { Subscription } from 'rxjs';
import { PostDetails } from './post-details.model';
import { WsHandler } from '../ws-handler';
import { ICellRendererParams } from 'ag-grid-community';
import { ElectronService } from 'ngx-electron';

@Component({
  selector: 'app-progress-cell',
  template: `
    <div style="height: 100%;">
      <div
        class="progress-bar"
        [ngClass]="{
          complete: postDetails.status === 'COMPLETE',
          downloading: postDetails.status === 'DOWNLOADING'
        }"
        [style.width]="trunc(postDetails.progress) + '%'"
      ></div>
      <div
        class="progress-bar-back"
        fxLayout="row"
        fxLayoutAlign="space-between"
        [ngClass]="{
          error: postDetails.status === 'ERROR',
          stopped: postDetails.status === 'STOPPED',
          pending: postDetails.status === 'PENDING'
        }"
      >
        <a style="color: rgba(0, 0, 0, 0.87);" href="javascript:void(0)" (click)="goTo()">{{ postDetails.url }}</a>
        <div>{{ trunc(postDetails.progress) + '%' }}</div>
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
    `
  ]
})
export class PostDetailsProgressRendererComponent implements AgRendererComponent, OnInit, OnDestroy {
  constructor(
    private wsConnectionService: WsConnectionService,
    private zone: NgZone,
    public electronService: ElectronService
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  subscription: Subscription;
  params: ICellRendererParams;
  postDetails: PostDetails;

  trunc(value: number): number {
    return Math.trunc(value);
  }

  ngOnInit(): void {
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      this.subscription = handler.subscribeForPostDetails(e => {
        this.zone.run(() => {
          e.forEach(v => {
            if (this.postDetails.url === v.url) {
              this.postDetails = v;
            }
          });
        });
      });
    });
  }

  goTo() {
    if (this.electronService.isElectronApp) {
      this.electronService.shell.openExternal(this.postDetails.url);
    } else {
      window.open(this.postDetails.url, '_blank');
    }
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  agInit(params: ICellRendererParams): void {
    this.params = params;
    this.postDetails = params.data;
  }

  refresh(params: ICellRendererParams): boolean {
    this.params = params;
    this.postDetails = params.data;
    return true;
  }
}
