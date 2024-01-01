import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { Thread } from '../domain/thread.model';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

@Component({
  selector: 'app-thread-contextmenu',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatListModule,
    MatIconModule,
    MatDialogModule,
  ],
  templateUrl: './thread-contextmenu.component.html',
  styleUrls: ['./thread-contextmenu.component.scss'],
})
export class ThreadContextmenuComponent {
  thread!: Thread;

  constructor(private dialog: MatDialog) {}

  onThreadSelection!: () => void;

  onThreadDelete!: () => void;
  close!: () => void;
}
