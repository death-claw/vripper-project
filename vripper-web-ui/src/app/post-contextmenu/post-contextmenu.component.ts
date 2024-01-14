import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule, NgIf } from '@angular/common';
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
import { PostRow } from '../domain/post-row.model';

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
