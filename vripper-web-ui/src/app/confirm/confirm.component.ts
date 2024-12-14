import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface ConfirmDialogData {
  message: string;
  confirmCallback: () => void;
}
@Component({
  selector: 'app-confirm',
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  templateUrl: './confirm.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
})
export class ConfirmComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData) {}

  confirm = () => {
    this.data.confirmCallback();
  };
}
