import {WsConnectionService} from '../ws-connection.service';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  NgZone,
  OnDestroy,
  OnInit
} from '@angular/core';
import {AgRendererComponent} from 'ag-grid-angular';
import {BehaviorSubject, Subject, Subscription} from 'rxjs';
import {PostDetails} from './post-details.model';
import {ICellRendererParams} from 'ag-grid-community';
import {ElectronService} from 'ngx-electron';
import {CtxtMenuService} from "./ctxt-menu.service";

@Component({
  selector: 'app-details-cell',
  templateUrl: 'post-details-progress.component.html',
  styleUrls: ['post-details-progress.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostDetailsProgressRendererComponent implements AgRendererComponent, OnInit, OnDestroy, AfterViewInit {
  constructor(
    private ws: WsConnectionService,
    private zone: NgZone,
    public electronService: ElectronService,
    private contextMenuService: CtxtMenuService) {
  }

  subscription: Subscription;
  postDetails$: EventEmitter<PostDetails> = new EventEmitter();
  private postDetails: PostDetails;
  loaded: Subject<boolean> = new BehaviorSubject(false);
  loading;

  stateSub: Subscription;
  postDetailsSub: Subscription;

  trunc(value: number): number {
    return Math.trunc(value);
  }

  ngOnInit(): void {
    this.stateSub = this.ws.state.subscribe(state => {
      if (state) {
        this.postDetailsSub = this.ws.subscribeForPostDetails().subscribe(e => {
          this.zone.run(() => {
            e.forEach(v => {
              if (this.postDetails.url === v.url) {
                this.postDetails = v;
                this.postDetails$.emit(this.postDetails);
              }
            });
          });
        });
      } else if (this.postDetailsSub != null) {
        this.postDetailsSub.unsubscribe();
      }
    });
  }

  ngAfterViewInit(): void {
    this.postDetails$.emit(this.postDetails);
    this.loading = setTimeout(() => this.loaded.next(true), 100);
  }

  ngOnDestroy(): void {
    if (this.subscription != null) {
      this.subscription.unsubscribe();
    }
    if (this.postDetailsSub != null) {
      this.postDetailsSub.unsubscribe();
    }
    if (this.stateSub != null) {
      this.stateSub.unsubscribe();
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

  context(event: MouseEvent) {
    this.contextMenuService.openPostDetailsCtxtMenu(event, this.postDetails);
  }
}
