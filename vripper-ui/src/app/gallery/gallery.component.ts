import {IImage, PhotoSwipeComponent} from './../photo-swipe/photo-swipe.component';
import {ChangeDetectionStrategy, Component, Inject, NgZone, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ServerService} from '../server-service';
import {PostState} from '../posts/post-state.model';
import {BehaviorSubject, Subject} from 'rxjs';

class Image extends IImage {
  constructor(
    public title: string,
    public src: string,
    public msrc: string,
    public w: number,
    public h: number,
    public _initialized: boolean
  ) {
    super(src, msrc, w, h);
  }
}

interface Progress {
  loading: boolean;
  progress: number;
  done: boolean;
}

const initialProgressState: Progress = { loading: false, progress: 0, done: false };

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
  disableRefresh: Subject<boolean> = new BehaviorSubject(true);
  loadProgress: Subject<Progress> = new BehaviorSubject(initialProgressState);
  loadedImages = 0;

  @ViewChild('photoSwipe') photoSwipe: PhotoSwipeComponent;

  loaded(img: Image) {
    img._initialized = true;
    this.loadedImages++;
    this.loadProgress.next({
      loading: true,
      progress: Math.floor((this.loadedImages / this._images.length) * 100),
      done: this.loadedImages === this._images.length
    });
    if (this.loadedImages === this._images.length) {
      this.disableRefresh.next(false);
    }
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
    this.loadProgress.next(initialProgressState);
    this.loadedImages = 0;
    this.disableRefresh.next(true);
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
          this._snackBar.open(error?.error?.message.error || 'Unexpected error, check log file', null, {
            duration: 5000
          });
        });
      }
    );
  }
}
