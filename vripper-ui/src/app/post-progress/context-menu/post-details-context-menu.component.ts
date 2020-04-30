import {ChangeDetectionStrategy, Component} from '@angular/core';
import {PostDetails} from '../../domain/post-details.model';
import {CtxtMenuService} from "./ctxt-menu.service";
import {ElectronService} from "ngx-electron";

@Component({
  selector: 'app-post-details-ctx-menu',
  template: `
    <mat-card class="mat-elevation-z4">
      <mat-action-list>
        <button (click)="goTo()" ngxClipboard [cbContent]="postDetail.url" mat-list-item>
          <mat-icon>open_in_new</mat-icon>
          <span>Open link</span>
        </button>
      </mat-action-list>
    </mat-card>
  `,
  styles: [
    `
      mat-card {
        padding: 0 0 8px 0;
      }
      mat-icon {
        margin-right: 5px;
      }
    `
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostDetailsContextMenuComponent {
  postDetail: PostDetails;
  fs;
  contextMenuService: CtxtMenuService;

  constructor(
    public electronService: ElectronService
  ) {
  }

  goTo() {
    this.contextMenuService.closePostDetailsCtxtMenu();
    if (this.electronService.isElectronApp) {
      this.electronService.shell.openExternal(this.postDetail.url);
    } else {
      window.open(this.postDetail.url, '_blank');
    }
  }
}
