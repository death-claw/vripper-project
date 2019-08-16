import { SharedService } from './shared.service';
import { WsConnectionService } from '../ws-connection.service';
import { PostState } from './post-state.model';
import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { Subscription } from 'rxjs';
import { WsHandler } from '../ws-handler';
import { ICellRendererParams } from 'ag-grid-community';
import { ElectronService } from 'ngx-electron';

@Component({
  selector: 'app-progress-cell',
  template: `
    <div style="height: 100%;">
      <div class="progress-text" fxLayout="row" fxLayoutAlign="space-between" (click)="toggleExpand()">
        <div>
          <div class="chevron" style="display: inline-block;">
            <mat-icon *ngIf="expanded; else notExpanded" style="vertical-align: middle;">expand_more</mat-icon>
            <ng-template #notExpanded>
              <mat-icon style="vertical-align: middle;">chevron_right</mat-icon>
            </ng-template>
          </div>
          {{ postState.title }}
        </div>
        <div>{{ postState.done + '/' + postState.total }}</div>
      </div>
      <div
        class="progress-bar"
        [ngClass]="{
          complete: postState.status === 'COMPLETE',
          downloading: postState.status === 'DOWNLOADING',
          stopped: postState.status === 'STOPPED',
          partial: postState.status === 'PARTIAL',
          error: postState.status === 'ERROR',
          pending: postState.status === 'PENDING'
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
      <section *ngIf="expanded">
        <div class="details table">
          <div class="row" style="display: table-row">
            <h4 class="cell attribute">Status:</h4>
            <p class="cell value">
              {{ postState.status | titlecase }}
            </p>
          </div>
          <div class="row" style="display: table-row">
            <h4 class="cell attribute">Post URL:</h4>
            <p class="cell value">
              <a href="javascript:void(0)" [appPreview]="postState.previews" (click)="goTo()"
                >https://vipergirls/threads/?p={{ postState.postId }}</a
              >
            </p>
          </div>
          <div style="display: table-row">
            <h4 class="cell attribute">Host:</h4>
            <p class="cell value">
              {{ postState.hosts }}
            </p>
          </div>
        </div>
      </section>
    </div>
  `,
  styles: [
    `
      .progress-text {
        position: relative;
        padding: 0 8px;
        z-index: 2;
        height: 46px;
      }
      .progress-bar {
        position: relative;
        height: 46px;
        bottom: 46px;
        transition: width 0.5s, background-color 0.5s;
        z-index: 1;
      }
      .progress-bar-back {
        position: relative;
        height: 46px;
        bottom: 92px;
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

      .table {
        display: table;
      }
      .row {
        display: table-row;
      }
      .cell {
        display: table-cell;
      }

      section {
        width: 100%;
        position: absolute;
        top: 46px;
        padding: 10px;
      }

      .details {
        line-height: 24px;
      }

      .value {
        padding-left: 5px;
      }
    `
  ]
})
export class PostProgressRendererComponent implements AgRendererComponent, OnInit, OnDestroy {
  constructor(
    private wsConnectionService: WsConnectionService,
    private zone: NgZone,
    private sharedService: SharedService,
    public electronService: ElectronService
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  params: ICellRendererParams;
  postState: PostState;
  updatesSubscription: Subscription;
  expandSubscription: Subscription;
  expanded = false;

  trunc(value: number): number {
    return Math.trunc(value);
  }

  ngOnInit(): void {
    this.websocketHandlerPromise.then((handler: WsHandler) => {
      this.updatesSubscription = handler.subscribeForPosts(e => {
        this.zone.run(() => {
          e.forEach(v => {
            if (this.postState.postId === v.postId) {
              this.postState = v;
            }
          });
        });
      });
    });
    this.expandSubscription = this.sharedService.expandedPost.subscribe(postId => {
      if (this.expanded && this.postState.postId !== postId) {
        setTimeout(this.toggleExpand.bind(this), 100);
      }
    });
  }

  ngOnDestroy(): void {
    this.updatesSubscription.unsubscribe();
    this.expandSubscription.unsubscribe();
  }

  agInit(params: ICellRendererParams): void {
    this.params = params;
    this.postState = params.data;
  }

  refresh(params: ICellRendererParams): boolean {
    this.params = params;
    this.postState = params.data;
    return true;
  }

  toggleExpand() {
    if (this.expanded) {
      this.params.node.setRowHeight(48);
    } else {
      this.params.node.setRowHeight(130);
      this.sharedService.publishExpanded(this.postState.postId);
    }
    this.params.api.onRowHeightChanged();
    this.expanded = !this.expanded;
  }

  goTo() {
    if (this.electronService.isElectronApp) {
      this.electronService.shell.openExternal(this.postState.url);
    } else {
      window.open(this.postState.url, '_blank');
    }
  }
}
