import { Component, computed, signal, WritableSignal } from '@angular/core';
import { ToolbarComponent } from './toolbar/toolbar.component';
import { CommonModule, NgIf } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { DownloadTableComponent } from './download-table/download-table.component';
import { ThreadTableComponent } from './thread-table/thread-table.component';
import { LogTableComponent } from './log-table/log-table.component';
import { Post } from './domain/post.model';
import { ApplicationEndpointService } from './services/application-endpoint.service';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { SettingsComponent } from './settings/settings.component';
import { Settings } from './domain/settings.model';
import {
  ConfirmComponent,
  ConfirmDialogData,
} from './confirm/confirm.component';
import { EMPTY, mergeMap, Subject } from 'rxjs';
import { RxStompState } from '@stomp/rx-stomp';
import { StatusBarComponent } from './status-bar/status-bar.component';
import {
  RenameDialogComponent,
  RenameDialogData,
  RenameDialogResult,
} from './rename-dialog/rename-dialog.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    MatTabsModule,
    MatIconModule,
    MatBadgeModule,
    DownloadTableComponent,
    ThreadTableComponent,
    LogTableComponent,
    ToolbarComponent,
    MatDialogModule,
    NgIf,
    StatusBarComponent,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  downloadCount = signal(0);
  threadCount = signal(0);
  logCount = signal(0);
  tabIndex = signal(0);

  selectedPost: WritableSignal<Post | null> = signal(null);
  selectedPosts = signal([] as Post[]);
  noPostSelected = computed(() => this.selectedPosts().length === 0);
  loading = computed(
    () => this.applicationEndpoint.connectionState() != RxStompState.OPEN
  );

  clearThreads = new Subject<void>();
  clearLogs = new Subject<void>();

  constructor(
    private applicationEndpoint: ApplicationEndpointService,
    private dialog: MatDialog
  ) {}

  onTabChange = (index: number) => {
    this.tabIndex.set(index);
  };

  onPostsSelectionChange = (selectedPosts: Post[]) => {
    this.selectedPosts.set(selectedPosts);
  };

  onPostSelectionChange = (selectedPost: Post) => {
    this.selectedPost.set(selectedPost);
  };

  onStartSelected = () => {
    if (this.selectedPosts().length > 0) {
      this.applicationEndpoint.startPosts(this.selectedPosts()).subscribe();
    }
  };

  onStopSelected = () => {
    if (this.selectedPosts().length > 0) {
      this.applicationEndpoint.stopPosts(this.selectedPosts()).subscribe();
    }
  };

  onStartDownload = () => {
    this.applicationEndpoint.startDownload().subscribe();
  };

  onStopDownload = () => {
    this.applicationEndpoint.stopDownload().subscribe();
  };

  onLinks = (links: string) => {
    this.applicationEndpoint.links(links).subscribe();
  };

  onSettings = () => {
    this.applicationEndpoint.settings().subscribe(s =>
      this.dialog.open<SettingsComponent, Settings>(SettingsComponent, {
        data: s,
        maxWidth: '100vw',
        maxHeight: '100vh',
        width: '80vw',
        height: '80vh',
      })
    );
  };

  onDeleteSelected = () => {
    const confirmDialog: MatDialogRef<ConfirmComponent, ConfirmDialogData> =
      this.dialog.open<ConfirmComponent, ConfirmDialogData>(ConfirmComponent, {
        data: {
          message: `Confirm removal of ${this.selectedPosts().length} post${
            this.selectedPosts().length > 1 ? 's' : ''
          }`,
          confirmCallback: () =>
            this.applicationEndpoint
              .deletePosts(this.selectedPosts())
              .subscribe(() => confirmDialog.close()),
        },
      });
  };

  onRenameSelected = () => {
    if (this.selectedPost() == null) {
      return;
    }
    const post = this.selectedPost();
    if (post?.postId == null || post?.folderName == null) {
      return;
    }
    const dialog: MatDialogRef<RenameDialogComponent, RenameDialogResult> =
      this.dialog.open<
        RenameDialogComponent,
        RenameDialogData,
        RenameDialogResult
      >(RenameDialogComponent, {
        data: {
          postId: post.postId,
          name: post.folderName,
        },
      });
    dialog
      .afterClosed()
      .pipe(
        mergeMap(result => {
          if (result) {
            return this.applicationEndpoint.renamePost(result);
          } else {
            return EMPTY;
          }
        })
      )
      .subscribe();
  };

  onClearDownloads = () => {
    const confirmDialog: MatDialogRef<ConfirmComponent, ConfirmDialogData> =
      this.dialog.open<ConfirmComponent, ConfirmDialogData>(ConfirmComponent, {
        data: {
          message: `Confirm removal of finished posts${
            this.selectedPosts().length > 1 ? 's' : ''
          }`,
          confirmCallback: () =>
            this.applicationEndpoint
              .clearDownloads()
              .subscribe(() => confirmDialog.close()),
        },
      });
  };

  onClearLogs = () => {
    const confirmDialog: MatDialogRef<ConfirmComponent, ConfirmDialogData> =
      this.dialog.open<ConfirmComponent, ConfirmDialogData>(ConfirmComponent, {
        data: {
          message: `Confirm removal of logs${
            this.selectedPosts().length > 1 ? 's' : ''
          }`,
          confirmCallback: () =>
            this.applicationEndpoint.clearLogs().subscribe(() => {
              confirmDialog.close();
              this.clearLogs.next();
            }),
        },
      });
  };

  onClearThreads = () => {
    const confirmDialog: MatDialogRef<ConfirmComponent, ConfirmDialogData> =
      this.dialog.open<ConfirmComponent, ConfirmDialogData>(ConfirmComponent, {
        data: {
          message: `Confirm removal of threads${
            this.selectedPosts().length > 1 ? 's' : ''
          }`,
          confirmCallback: () =>
            this.applicationEndpoint.clearThreads().subscribe(() => {
              confirmDialog.close();
              this.clearThreads.next();
            }),
        },
      });
  };
}
