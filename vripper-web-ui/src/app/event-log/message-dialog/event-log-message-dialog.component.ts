import {ChangeDetectionStrategy, Component, Inject, NgZone} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-event-log-message-dialog',
  templateUrl: './event-log-message-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EventLogMessageDialogComponent {

  constructor(public dialogRef: MatDialogRef<EventLogMessageDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: string,
              private ngZone: NgZone) {
  }

  close() {
    this.ngZone.run(() => this.dialogRef.close());
  }
}
