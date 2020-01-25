import { Subscription } from 'rxjs';
import { LinkCollectorService } from './../link-collector.service';
import { ServerService } from './../server-service';
import { ElectronService } from 'ngx-electron';
import { ClipboardService } from './../clipboard.service';
import { Component, OnInit, OnDestroy, NgZone, ChangeDetectionStrategy } from '@angular/core';
import { MatDialog, MatSnackBar } from '@angular/material';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeComponent implements OnInit, OnDestroy {
  constructor(
    private clipboardService: ClipboardService,
    public dialog: MatDialog,
    public electronService: ElectronService,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    private ngZone: NgZone,
    public linkCollectorService: LinkCollectorService
  ) {}

  clipboardSub: Subscription;

  ngOnInit() {
    this.clipboardSub = this.clipboardService.links.subscribe(e => {
      this.ngZone.run(() => {
        this.httpClient
          .post<{ threadId: string; postId: string }>(this.serverService.baseUrl + '/post', { url: e })
          .subscribe(
            response => {
              this._snackBar.open('Clipboard successfully scanned', null, {
                duration: 5000
              });
            },
            error => {
              this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
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
}
