import { AppService } from './../app.service';
import { ElectronService } from 'ngx-electron';
import { ClipboardService } from './../clipboard.service';
import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { MatDialog, MatSnackBar } from '@angular/material';
import { WsConnectionService } from '../ws-connection.service';
import { WsHandler } from '../ws-handler';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit, OnDestroy {

  loading = false;
  spinnerMode = 'indeterminate';
  spinnerValue = 0;

  parseEndSubscription: Subscription;

  constructor(
    private clipboardService: ClipboardService,
    public dialog: MatDialog,
    public electronService: ElectronService,
    private wsConnectionService: WsConnectionService,
    private appService: AppService,
    private _snackBar: MatSnackBar,
    private ngZone: NgZone
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;

  ngOnInit() {
    this.clipboardService.links.subscribe(e => {
      this.ngZone.run(() => {
        if (this.appService.isScanOpen) {
          this._snackBar.open(e + ' was not processed, the app is busy parsing another thread', null, {
            duration: 5000
          });
          return;
        }
        this.appService.scan(e);
      });
    });
  }

  view(chip) {
    console.log(chip);
  }

  ngOnDestroy() {
  }
}
