import {Subscription} from 'rxjs';
import {LinkCollectorService} from '../services/link-collector.service';
import {ServerService} from '../services/server-service';
import {ClipboardService} from '../services/clipboard.service';
import {ChangeDetectionStrategy, Component, NgZone, OnDestroy, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {HttpClient} from '@angular/common/http';
import {EventLogService} from '../services/event-log.service';
import {PostsService} from '../services/posts.service';
import {MatTabChangeEvent} from '@angular/material/tabs';
import {HomeTabsService} from '../services/home-tabs.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeComponent implements OnInit, OnDestroy {
  clipboardSub: Subscription;

  constructor(
    private clipboardService: ClipboardService,
    public dialog: MatDialog,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    private ngZone: NgZone,
    public linkCollectorService: LinkCollectorService,
    public eventLogService: EventLogService,
    public postsService: PostsService,
    public homeTabsService: HomeTabsService
  ) {
  }

  ngOnInit() {
    this.clipboardSub = this.clipboardService.links.subscribe(e => {
      this.ngZone.run(() => {
        this.httpClient
          .post<{ threadId: string; postId: string }>(this.serverService.baseUrl + '/post', {url: e})
          .subscribe(
            response => {
              this._snackBar.open('Clipboard scan complete', null, {
                duration: 5000
              });
            },
            error => {
              this._snackBar.open(error?.error?.message || 'Unexpected error, check log file', null, {
                duration: 5000
              });
            }
          );
      });
    });
  }

  ngOnDestroy() {
    if (this.clipboardSub != null) {
      this.clipboardSub.unsubscribe();
    }
  }

  onTabChange($event: MatTabChangeEvent) {
    this.homeTabsService.setIndex($event.index);
  }
}
