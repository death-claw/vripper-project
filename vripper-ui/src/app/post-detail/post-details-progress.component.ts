import { WsConnectionService } from '../ws-connection.service';
import {
  Component,
  OnInit,
  OnDestroy,
  NgZone,
  ChangeDetectionStrategy,
  EventEmitter,
  AfterViewInit
} from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { Subscription, Subject, BehaviorSubject } from 'rxjs';
import { PostDetails } from './post-details.model';
import { WsHandler } from '../ws-handler';
import { ICellRendererParams } from 'ag-grid-community';
import { ElectronService } from 'ngx-electron';

@Component({
  selector: 'app-details-cell',
  templateUrl: 'post-details-progress.component.html',
  styleUrls: ['post-details-progress.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostDetailsProgressRendererComponent implements AgRendererComponent, OnInit, OnDestroy, AfterViewInit {
  constructor(
    private wsConnectionService: WsConnectionService,
    private zone: NgZone,
    public electronService: ElectronService
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;
  subscription: Subscription;
  postDetails$: EventEmitter<PostDetails> = new EventEmitter();
  private postDetails: PostDetails;
  loaded: Subject<boolean> = new BehaviorSubject(false);
  loading;

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
              this.postDetails$.emit(this.postDetails);
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

  ngAfterViewInit(): void {
    this.postDetails$.emit(this.postDetails);
    this.loading = setTimeout(() => this.loaded.next(true), 100);
  }

  ngOnDestroy(): void {
    if (this.subscription != null) {
      this.subscription.unsubscribe();
    }
    clearTimeout(this.loading);
  }

  agInit(params: ICellRendererParams): void {
    this.postDetails = params.data;
  }

  refresh(params: ICellRendererParams): boolean {
    this.postDetails = params.data;
    this.postDetails$.emit(this.postDetails);
    return true;
  }
}
