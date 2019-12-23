import { IImage, PhotoSwipeComponent } from './../photo-swipe/photo-swipe.component';
import { Component, OnInit, ChangeDetectionStrategy, Inject, NgZone, OnDestroy, ViewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatDialogRef, MAT_DIALOG_DATA, MatSnackBar } from '@angular/material';
import { ServerService } from '../server-service';
import { PostState } from '../posts/post-state.model';
import { BehaviorSubject, Subject } from 'rxjs';

class Image extends IImage {
  constructor(public src: string, public msrc: string, public w: number, public h: number, public _initialized: boolean) {
    super(src, msrc, w, h);
  }
}

@Component({
  selector: 'app-gallery',
  templateUrl: './gallery.component.html',
  styleUrls: ['./gallery.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GalleryComponent implements OnInit, OnDestroy {
  constructor(
    public dialogRef: MatDialogRef<GalleryComponent>,
    @Inject(MAT_DIALOG_DATA) public dialogData: PostState,
    private httpClient: HttpClient,
    private serverService: ServerService,
    private _snackBar: MatSnackBar,
    private ngZone: NgZone
  ) {}

  images: Subject<Image[]> = new BehaviorSubject([]);
  _images: Image[] = [];

  @ViewChild('photoSwipe', { static: true }) photoSwipe: PhotoSwipeComponent;

  loaded(img: Image) {
    img._initialized = true;
  }

  ngOnInit() {
    this.refresh();
  }

  ngOnDestroy() {}

  openSlideshow(index: number) {
    this.photoSwipe.openGallery(this._images, {
      index: index,
      getThumbBoundsFn: this.getThumbBoundsFn,
      showHideOpacity: true,
      preload: [1, 8]
    });
  }

  getThumbBoundsFn(index: number) {
    // See Options -> getThumbBoundsFn section of documentation for more info
    const thumbnail = document.querySelectorAll('.masonry-brick img')[index],
      pageYScroll = window.pageYOffset || document.documentElement.scrollTop,
      rect = thumbnail.getBoundingClientRect();

    return { x: rect.left, y: rect.top + pageYScroll, w: rect.width };
  }

  refresh() {
    this.httpClient.get<Image[]>(this.serverService.baseUrl + '/gallery/' + this.dialogData.postId).subscribe(
      response => {
        this.ngZone.run(() => {
          response.forEach(i => {
            i.src = this.serverService.baseUrl + '/image/' + this.dialogData.postId + '/' + i.src;
            i.msrc = this.serverService.baseUrl + '/image/thumb/' + this.dialogData.postId + '/' + i.msrc;
            i._initialized = false;
          });
          this._images = response;
          this.images.next(this._images);
        });
      },
      error => {
        this.ngZone.run(() => {
          this.dialogRef.close();
          this._snackBar.open(error.error || 'Unexpected error, check log file', null, {
            duration: 5000
          });
        });
      }
    );
  }
}
