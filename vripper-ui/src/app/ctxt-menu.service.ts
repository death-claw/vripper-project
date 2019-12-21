import { PostState } from './posts/post-state.model';
import { Injectable, ComponentRef } from '@angular/core';
import { filter, take } from 'rxjs/operators';
import { fromEvent, Subscription } from 'rxjs';
import { OverlayPositionBuilder, Overlay, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { PostContextMenuComponent } from './posts/post-context-menu.component';

@Injectable()
export class ContextMenuService {
  constructor(private overlayPositionBuilder: OverlayPositionBuilder, private overlay: Overlay) {}

  postCtxtMenuPortal = new ComponentPortal(PostContextMenuComponent);
  private postCtxtMenuOverlayRef: OverlayRef;
  sub: Subscription;

  openPostCtxtMenu(mouseEvent: MouseEvent, postState: PostState) {
    mouseEvent.preventDefault();
    this.closePostCtxtMenu();
    const positionStrategy = this.overlayPositionBuilder
      .flexibleConnectedTo({ x: mouseEvent.x, y: mouseEvent.y })
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
    this.sub = fromEvent<MouseEvent>(document, 'click')
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
    if (this.sub != null) {
      this.sub.unsubscribe();
    }
    if (this.postCtxtMenuOverlayRef) {
      this.postCtxtMenuOverlayRef.dispose();
      this.postCtxtMenuOverlayRef = null;
    }
  }
}

export interface Theme {
  darkTheme: boolean;
}
