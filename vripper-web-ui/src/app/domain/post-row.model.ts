import { Post } from './post.model';
import { signal, WritableSignal } from '@angular/core';
import { progress, statusIcon, totalFormatter } from '../utils/utils';

export class PostRow extends Post {
  public statusIcon: WritableSignal<string>;
  public progress: WritableSignal<number>;
  public total2: WritableSignal<string>;
  public path: WritableSignal<string>;

  constructor(
    postId: number,
    postTitle: string,
    status: string,
    url: string,
    done: number,
    total: number,
    hosts: string[],
    addedOn: string,
    rank: number,
    downloadDirectory: string,
    folderName: string,
    downloadFolder: string,
    downloaded: number
  ) {
    super(
      postId,
      postTitle,
      status,
      url,
      done,
      total,
      hosts,
      addedOn,
      rank,
      downloadDirectory,
      folderName,
      downloadFolder,
      downloaded
    );
    this.statusIcon = signal(statusIcon(status));
    this.progress = signal(progress(done, total));
    this.total2 = signal(totalFormatter(done, total, downloaded));
    this.path = signal(downloadFolder);
  }
}
