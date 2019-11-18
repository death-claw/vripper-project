import { AppService } from './../app.service';
import { ElectronService } from 'ngx-electron';
import { ClipboardService } from './../clipboard.service';
import { Component, OnInit, OnDestroy, NgZone, ChangeDetectionStrategy } from '@angular/core';
import { MatDialog, MatSnackBar } from '@angular/material';

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
    private appService: AppService,
    private _snackBar: MatSnackBar,
    private ngZone: NgZone
  ) {}

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

  ngOnDestroy() {
  }
}
