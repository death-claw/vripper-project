import { DialogRef } from '@angular/cdk/dialog';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-scan',
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatInputModule,
    ReactiveFormsModule,
  ],
  templateUrl: './scan.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
})
export class ScanComponent {
  formControl = new FormControl<string>('');
  constructor(
    public dialogRef: DialogRef<never>,
    breakpointObserver: BreakpointObserver
  ) {
    breakpointObserver
      .observe(Breakpoints.HandsetPortrait)
      .subscribe((result) => {
        if (result.matches) {
          this.dialogRef.updateSize('100vw', '80vh');
        } else {
          this.dialogRef.updateSize('80vw', '80vh');
        }
        this.dialogRef.updatePosition();
      });
  }

  onPaste(event: ClipboardEvent) {
    event.preventDefault();
    const oldValue = this.formControl.value || '';
    this.formControl.setValue(
      `${oldValue}${event?.clipboardData?.getData('text')}\n`
    );
  }
}
