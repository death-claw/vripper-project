import {Component, ElementRef, ViewChild} from '@angular/core';

import PhotoSwipe from 'photoswipe';
import PhotoSwipeUI_Default from 'photoswipe/dist/photoswipe-ui-default';

export class IImage {
  constructor(public src: string, public msrc: string, public w: number, public h: number) {}
}

@Component({
  selector: 'app-photo-swipe',
  templateUrl: './photo-swipe.component.html',
  styleUrls: ['./photo-swipe.component.css']
})
export class PhotoSwipeComponent {
  @ViewChild('photoSwipe') photoSwipe: ElementRef;

  openGallery(images: IImage[], options?: any) {
    options = { ...options, history: false };
    const gallery = new PhotoSwipe(this.photoSwipe.nativeElement, PhotoSwipeUI_Default, images, options);
    gallery.init();
  }
}
