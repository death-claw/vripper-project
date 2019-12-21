import { Component, OnInit, ChangeDetectionStrategy, Inject, NgZone, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatDialogRef, MAT_DIALOG_DATA, MatSnackBar } from '@angular/material';
import { ServerService } from '../server-service';
import { PostState } from '../posts/post-state.model';
import { Image, PlainGalleryConfig, PlainGalleryStrategy, AdvancedLayout } from '@ks89/angular-modal-gallery';
import { ImageData } from '@ks89/angular-modal-gallery/lib/model/image.class';
import { BehaviorSubject, Subject } from 'rxjs';

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
  interval;

  customPlainGalleryRowDescConfig: PlainGalleryConfig = {
    strategy: PlainGalleryStrategy.CUSTOM,
    layout: new AdvancedLayout(-1, true)
  };

  openImageModalRowDescription(image: Image) {
    const index: number = this.getCurrentIndexCustomLayout(image, this._images);
    this.customPlainGalleryRowDescConfig = Object.assign({}, this.customPlainGalleryRowDescConfig, {
      layout: new AdvancedLayout(index, true)
    });
  }

  private getCurrentIndexCustomLayout(image: Image, images: Image[]): number {
    return image ? images.indexOf(image) : -1;
  }

  ngOnInit() {
    this.refresh();
    this.interval = setInterval(() => this.refresh(), 5000);
  }

  ngOnDestroy() {
    if (this.interval != null) {
      clearInterval(this.interval);
    }
  }

  refresh() {
    this.httpClient
      .get<ImageData[]>(this.serverService.baseUrl + '/gallery/' + this.dialogData.postId)
      .subscribe(
        response => {
          this.ngZone.run(() => {
            const images = response.map((v, i) => new Image(i, v, v));
            images.forEach(i => {
              const cloneModal = {...i.modal};
              const clonePlain = {...i.plain};
              cloneModal.img = this.serverService.baseUrl + '/image/' + this.dialogData.postId + '/' + cloneModal.img;
              clonePlain.img = this.serverService.baseUrl + '/image/thumb/' + this.dialogData.postId + '/' + clonePlain.img;
              i.modal = cloneModal;
              i.plain = clonePlain;
            });
            this._images = images;
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
