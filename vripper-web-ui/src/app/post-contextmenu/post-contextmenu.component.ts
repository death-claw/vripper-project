import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Output,
} from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
import { Post } from '../domain/post.model';
import {
  animate,
  state,
  style,
  transition,
  trigger,
} from '@angular/animations';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { ImageDialogData, ImagesComponent } from '../images/images.component';

@Component({
  selector: 'app-post-contextmenu',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatListModule, MatIconModule, NgIf],
  templateUrl: './post-contextmenu.component.html',
  styleUrls: ['./post-contextmenu.component.scss'],
  animations: [
    trigger('simpleFadeAnimation', [
      state('in', style({ opacity: 1 })),

      transition(':enter', [style({ opacity: 0 }), animate(300)]),
    ]),
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostContextmenuComponent {
  post!: Post;

  constructor(public dialog: MatDialog) {}

  openImages = () => {
    const data: ImageDialogData = { postId: this.post.postId };
    this.dialog.open(ImagesComponent, { data });
  };

  onPostStart!: () => void;

  onPostStop!: () => void;

  onPostDelete!: () => void;
}
