import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { TextFieldModule } from '@angular/cdk/text-field';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { DialogRef } from '@angular/cdk/dialog';

@Component({
  selector: 'app-scan',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatInputModule,
    ReactiveFormsModule,
    TextFieldModule,
  ],
  templateUrl: './scan.component.html',
  styleUrls: ['./scan.component.scss'],
})
export class ScanComponent {
  formControl = new FormControl<string>('');
  constructor(
    public dialogRef: DialogRef<never>,
    breakpointObserver: BreakpointObserver
  ) {
    breakpointObserver
      .observe(Breakpoints.HandsetPortrait)
      .subscribe(result => {
        if (result.matches) {
          this.dialogRef.updateSize('100vw', '80vh');
        } else {
          this.dialogRef.updateSize('80vw', '80vh');
        }
        this.dialogRef.updatePosition();
      });
  }
}
