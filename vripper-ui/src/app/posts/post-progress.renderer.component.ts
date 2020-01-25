import { WsConnectionService } from '../ws-connection.service';
import { PostState } from './post-state.model';
import {
  Component,
  OnInit,
  OnDestroy,
  NgZone,
  ChangeDetectionStrategy,
  AfterViewInit,
  EventEmitter
} from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { Subscription, BehaviorSubject, Subject } from 'rxjs';
import { WsHandler } from '../ws-handler';
import { ICellRendererParams, RowNode, GridApi } from 'ag-grid-community';
import { ElectronService } from 'ngx-electron';
import { MatDialog } from '@angular/material';
import { ContextMenuService } from '../ctxt-menu.service';

@Component({
  selector: 'app-progress-cell',
  templateUrl: 'post-progress.renderer.component.html',
  styleUrls: ['post-progress.renderer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostProgressRendererComponent implements AgRendererComponent, OnInit, OnDestroy, AfterViewInit {
  constructor(
    private ws: WsConnectionService,
    private zone: NgZone,
    public electronService: ElectronService,
    public dialog: MatDialog,
    private contextMenuService: ContextMenuService
  ) {}

  websocketHandlerPromise: Promise<WsHandler>;
  postState$: EventEmitter<PostState> = new EventEmitter();
  private postState: PostState;
  updatesSubscription: Subscription;
  expanded = false;
  loaded: Subject<boolean> = new BehaviorSubject(false);
  loading;
  node: RowNode;
  gridApi: GridApi;

  postsSub: Subscription;
  stateSub: Subscription;

  trunc(value: number): number {
    return Math.trunc(value);
  }

  ngOnInit(): void {
    this.stateSub = this.ws.state.subscribe(state => {
      if (state) {
        this.postsSub = this.ws.subscribeForPosts().subscribe(e => {
          this.zone.run(() => {
            e.forEach(v => {
              if (this.postState.postId === v.postId) {
                this.postState = v;
                this.postState$.emit(this.postState);
              }
            });
          });
        });
      } else if (this.postsSub != null) {
        this.postsSub.unsubscribe();
      }
    });
  }

  ngAfterViewInit(): void {
    this.postState$.emit(this.postState);
    this.loading = setTimeout(() => this.loaded.next(true), 100);
  }

  ngOnDestroy(): void {
    if (this.updatesSubscription != null) {
      this.updatesSubscription.unsubscribe();
    }
    if (this.postsSub != null) {
      this.postsSub.unsubscribe();
    }
    if (this.stateSub != null) {
      this.stateSub.unsubscribe();
    }
    clearTimeout(this.loading);
  }

  agInit(params: ICellRendererParams): void {
    this.node = params.node;
    this.gridApi = params.api;
    this.postState = params.data;
  }

  refresh(params: ICellRendererParams): boolean {
    this.postState = params.data;
    this.postState$.emit(this.postState);
    return true;
  }

  goTo() {
    if (this.electronService.isElectronApp) {
      this.electronService.shell.openExternal(this.postState.url);
    } else {
      window.open(this.postState.url, '_blank');
    }
  }

  context(event: MouseEvent) {
    this.gridApi.getSelectedNodes().forEach(e => e.setSelected(false));
    this.node.setSelected(true);
    this.contextMenuService.openPostCtxtMenu(event, this.postState);
  }
}
