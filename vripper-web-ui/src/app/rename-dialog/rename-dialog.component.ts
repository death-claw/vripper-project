import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface RenameDialogData {
  postId: number;
  name: string;
}

export interface RenameDialogResult {
  postId: number;
  name: string;
}

@Component({
  selector: 'app-rename-dialog',
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
  ],
  templateUrl: './rename-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
})
export class RenameDialogComponent {
  formControl = new FormControl(this.data.name);

  constructor(
    public dialogRef: MatDialogRef<RenameDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RenameDialogData
  ) {}

  confirm() {
    this.dialogRef.close({
      postId: this.data.postId,
      name: this.formControl.value,
    } as RenameDialogResult);
  }
}
