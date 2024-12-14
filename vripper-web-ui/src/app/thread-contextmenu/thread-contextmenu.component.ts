import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { Thread } from '../domain/thread.model';

@Component({
  selector: 'app-thread-contextmenu',
  imports: [
    CommonModule,
    MatCardModule,
    MatListModule,
    MatIconModule,
    MatDialogModule,
  ],
  templateUrl: './thread-contextmenu.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
})
export class ThreadContextmenuComponent {
  thread!: Thread;

  onThreadSelection!: () => void;

  onThreadDelete!: () => void;
  close!: () => void;
}
