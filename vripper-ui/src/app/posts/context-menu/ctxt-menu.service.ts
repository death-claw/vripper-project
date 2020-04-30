import {ComponentRef, Injectable} from '@angular/core';
import {ComponentPortal} from "@angular/cdk/portal";
import {PostContextMenuComponent} from "./post-context-menu.component";
import {Overlay, OverlayPositionBuilder, OverlayRef} from "@angular/cdk/overlay";
import {fromEvent, Subscription} from "rxjs";
import {PostState} from "../../domain/post-state.model";
import {filter, take} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class CtxtMenuService {

  constructor(private overlayPositionBuilder: OverlayPositionBuilder, private overlay: Overlay) {
  }

  postCtxtMenuPortal = new ComponentPortal(PostContextMenuComponent);
  private postCtxtMenuOverlayRef: OverlayRef;
  postCtxt: Subscription;

  openPostCtxtMenu(mouseEvent: MouseEvent, postState: PostState) {
    mouseEvent.preventDefault();
    this.closePostCtxtMenu();
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
    this.postCtxtMenuOverlayRef = this.overlay.create({
      positionStrategy
    });
    const ref: ComponentRef<PostContextMenuComponent> = this.postCtxtMenuOverlayRef.attach(this.postCtxtMenuPortal);
    ref.instance.postState = postState;
    ref.instance.contextMenuService = this;
    this.postCtxt = fromEvent<MouseEvent>(document, 'click')
      .pipe(
        filter(event => {
          const clickTarget = event.target as HTMLElement;
          return !!this.postCtxtMenuOverlayRef && !this.postCtxtMenuOverlayRef.overlayElement.contains(clickTarget);
        }),
        take(1)
      )
      .subscribe(() => this.closePostCtxtMenu());
  }

  closePostCtxtMenu() {
    if (this.postCtxt != null) {
      this.postCtxt.unsubscribe();
    }
    if (this.postCtxtMenuOverlayRef) {
      this.postCtxtMenuOverlayRef.dispose();
      this.postCtxtMenuOverlayRef = null;
    }
  }
}
