import {Subscription} from 'rxjs';
import {LinkCollectorService} from '../services/link-collector.service';
import {ServerService} from '../services/server-service';
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
export class HomeComponent {

  constructor(
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

  onTabChange($event: MatTabChangeEvent) {
    this.homeTabsService.setIndex($event.index);
  }
}
