import {
  animate,
  state,
  style,
  transition,
  trigger,
} from '@angular/animations';
import { CommonModule, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { PostRow } from '../domain/post-row.model';
import { ImageDialogData, ImagesComponent } from '../images/images.component';

@Component({
  selector: 'app-post-contextmenu',
  imports: [CommonModule, MatCardModule, MatListModule, MatIconModule, NgIf],
  templateUrl: './post-contextmenu.component.html',
  animations: [
    trigger('simpleFadeAnimation', [
      state('in', style({ opacity: 1 })),
      transition(':enter', [style({ opacity: 0 }), animate(300)]),
    ]),
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
})
export class PostContextmenuComponent {
  post!: PostRow;

  constructor(public dialog: MatDialog) {}

  openImages = () => {
    const data: ImageDialogData = { postId: this.post.postId };
    this.dialog.open(ImagesComponent, {
      data,
      maxWidth: '100vw',
      maxHeight: '100vh',
      width: '80vw',
      height: '80vh',
    });
  };

  onPostStart!: () => void;
  onPostStop!: () => void;
  onPostRename!: () => void;
  onPostDelete!: () => void;
  close!: () => void;
}
