import { Image } from './image.model';
import { signal, WritableSignal } from '@angular/core';
import { progress, statusIcon } from '../utils/utils';

export class ImageRow extends Image {
  public progress: WritableSignal<number>;
  public statusIcon: WritableSignal<string>;

  constructor(
    postId: number,
    url: string,
    status: string,
    index: number,
    downloaded: number,
    size: number
  ) {
    super(postId, url, status, index, downloaded, size);
    this.progress = signal(progress(downloaded, size));
    this.statusIcon = signal(statusIcon(status));
  }
}
