import { ElectronService } from 'ngx-electron';
import { ClipboardService } from './../clipboard.service';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { MatDialog } from '@angular/material';
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
    private wsConnectionService: WsConnectionService
  ) {
    this.websocketHandlerPromise = this.wsConnectionService.getConnection();
  }

  websocketHandlerPromise: Promise<WsHandler>;

  ngOnInit() {
    this.clipboardService.links.subscribe(e => {
      // if (this.loading) {
      //   this._snackBar.open(e + ' was not processed, the app is beasy parsing another thread', null, {
      //     duration: 5000
      //   });
      //   return;
      // }
      // this.processUrl(e);
    });
  }

  view(chip) {
    console.log(chip);
  }

  ngOnDestroy() {
  }
}
