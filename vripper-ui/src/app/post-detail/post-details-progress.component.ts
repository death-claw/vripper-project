import { WsConnectionService } from '../ws-connection.service';
import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { Subscription } from 'rxjs';
import { PostDetails } from './post-details.model';
import { WsHandler } from '../ws-handler';
import { ICellRendererParams } from 'ag-grid-community';
import { ElectronService } from 'ngx-electron';

@Component({
  selector: 'app-details-cell',
  templateUrl: 'post-details-progress.component.html',
  styleUrls: ['post-details-progress.component.scss']
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
    if (this.subscription != null) {
      this.subscription.unsubscribe();
    }
  }

  agInit(params: ICellRendererParams): void {
    this.params = params;
    this.postDetails = params.data;
  }

  refresh(params: ICellRendererParams): boolean {
    return false;
  }
}
