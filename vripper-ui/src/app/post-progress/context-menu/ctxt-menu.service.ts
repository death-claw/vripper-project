import {ComponentRef, Injectable} from '@angular/core';
import {PostDetails} from "../../domain/post-details.model";
import {PostDetailsContextMenuComponent} from "./post-details-context-menu.component";
import {fromEvent, Subscription} from "rxjs";
import {filter, take} from "rxjs/operators";
import {ComponentPortal} from "@angular/cdk/portal";
import {Overlay, OverlayPositionBuilder, OverlayRef} from "@angular/cdk/overlay";

@Injectable({
  providedIn: 'root'
})
export class CtxtMenuService {

  postDetailsCtxtMenuPortal = new ComponentPortal(PostDetailsContextMenuComponent);
  private postDetailsCtxtMenuOverlayRef: OverlayRef;
  postDetailsCtxt: Subscription;

  constructor(
    private overlayPositionBuilder: OverlayPositionBuilder,
    private overlay: Overlay
  ) {
  }

  openPostDetailsCtxtMenu(mouseEvent: MouseEvent, postDetails: PostDetails) {
    mouseEvent.preventDefault();
    this.closePostDetailsCtxtMenu();
    const positionStrategy = this.overlayPositionBuilder
      .flexibleConnectedTo({x: mouseEvent.x, y: mouseEvent.y})
      .withPush(true)
      .withGrowAfterOpen(true)
      .withPositions([
        {
          originX: 'start',
          originY: 'bottom',
          overlayX: 'start',
          overlayY: 'top'
        },
        {
          originX: 'start',
          originY: 'top',
          overlayX: 'start',
          overlayY: 'bottom'
        }
      ]);
    this.postDetailsCtxtMenuOverlayRef = this.overlay.create({
      positionStrategy
    });
    const ref: ComponentRef<PostDetailsContextMenuComponent> = this.postDetailsCtxtMenuOverlayRef.attach(this.postDetailsCtxtMenuPortal);
    ref.instance.postDetail = postDetails;
    ref.instance.contextMenuService = this;
    this.postDetailsCtxt = fromEvent<MouseEvent>(document, 'click')
      .pipe(
        filter(event => {
          const clickTarget = event.target as HTMLElement;
          return !!this.postDetailsCtxtMenuOverlayRef && !this.postDetailsCtxtMenuOverlayRef.overlayElement.contains(clickTarget);
        }),
        take(1)
      )
      .subscribe(() => this.closePostDetailsCtxtMenu());
  }

  closePostDetailsCtxtMenu() {
    if (this.postDetailsCtxt != null) {
      this.postDetailsCtxt.unsubscribe();
    }
    if (this.postDetailsCtxtMenuOverlayRef) {
      this.postDetailsCtxtMenuOverlayRef.dispose();
      this.postDetailsCtxtMenuOverlayRef = null;
    }
  }
}
