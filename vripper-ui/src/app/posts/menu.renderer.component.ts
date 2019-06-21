import { Component, OnInit, OnDestroy } from '@angular/core';
import { AgRendererComponent } from 'ag-grid-angular';
import { PostState } from './post-state.model';
import { PostDetailComponent } from '../post-detail/post-detail.component';
import { MatDialog } from '@angular/material';
import { environment } from 'src/environments/environment';
import { HttpClient } from '@angular/common/http';
import { BreakpointObserver, BreakpointState, Breakpoints } from '@angular/cdk/layout';
import { Observable, Subscription } from 'rxjs';
import { WsConnectionService } from '../ws-connection.service';

@Component({
  selector: 'app-menu-cell',
  template: `
    <div fxLayout="column" fxLayoutAlign="center center" style="height: 100%">
      <button fxFlex="nogrow" mat-icon-button [matMenuTriggerFor]="menu">
        <mat-icon>more_vert</mat-icon>
      </button>
    </div>

    <mat-menu #menu="matMenu">
      <button
        *ngIf="
          postData.status === 'PENDING' ||
          postData.status === 'COMPLETE' ||
          postData.status === 'ERROR' ||
          postData.status === 'STOPPED'
        "
        (click)="restart()"
        mat-menu-item
      >
        <mat-icon>play_arrow</mat-icon>
        <span>Start</span>
      </button>
      <button *ngIf="postData.status === 'DOWNLOADING' || postData.status === 'PARTIAL'" (click)="stop()" mat-menu-item>
        <ng-container>
          <mat-icon>stop</mat-icon>
          <span>Stop</span>
        </ng-container>
      </button>
      <button (click)="seeDetails()" mat-menu-item>
        <mat-icon>details</mat-icon>
        <span>Details</span>
      </button>
    </mat-menu>
  `
})
export class MenuRendererComponent implements OnInit, OnDestroy, AgRendererComponent {
  isExtraSmall: Observable<BreakpointState> = this.breakpointObserver.observe(Breakpoints.XSmall);

  params: any;

  postData: PostState;

  subscription: Subscription;

  constructor(
    public dialog: MatDialog,
    private httpClient: HttpClient,
    private breakpointObserver: BreakpointObserver,
    private wsConnectionService: WsConnectionService
  ) {}

  ngOnInit(): void {
    this.subscription = this.wsConnectionService.subscribeForPosts(e => {
      e.forEach(v => {
        if (this.postData.postId === v.postId) {
          this.postData = v;
        }
      });
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  seeDetails() {
    const dialogRef = this.dialog.open(PostDetailComponent, {
      width: '90%',
      height: '90%',
      maxWidth: '100vw',
      maxHeight: '100vh',
      data: this.postData
    });

    const smallDialogSubscription = this.isExtraSmall.subscribe(result => {
      if (result.matches) {
        dialogRef.updateSize('100%', '100%');
      } else {
        dialogRef.updateSize('90%', '90%');
      }
    });

    dialogRef.afterClosed().subscribe(() => {
      smallDialogSubscription.unsubscribe();
    });
  }

  restart() {
    this.httpClient.post(environment.localhost + '/post/restart', { postId: this.postData.postId }).subscribe(
      data => {},
      error => {
        console.error(error);
      }
    );
  }

  stop() {
    this.httpClient.post(environment.localhost + '/post/stop', { postId: this.postData.postId }).subscribe(
      data => {},
      error => {
        console.error(error);
      }
    );
  }

  agInit(params: any): void {
    this.params = params;
    this.postData = params.data;
  }

  refresh(params: any): boolean {
    return false;
  }
}
