import {ComponentRef, Injectable} from '@angular/core';
import {ComponentPortal} from '@angular/cdk/portal';
import {PostContextMenuComponent} from '../posts/context-menu/post-context-menu.component';
import {Overlay, OverlayPositionBuilder, OverlayRef} from '@angular/cdk/overlay';
import {fromEvent, Subscription} from 'rxjs';
import {Post} from '../domain/post-state.model';
import {filter, take} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class PostContextMenuService {

  postContextMenuPortal = new ComponentPortal(PostContextMenuComponent);
  postContext!: Subscription;
  private postContextMenuOverlayRef: OverlayRef | null = null;

  constructor(private overlayPositionBuilder: OverlayPositionBuilder, private overlay: Overlay) {
  }

  openPostContextMenu(mouseEvent: MouseEvent, post: Post) {
    mouseEvent.preventDefault();
    this.closePostContextMenu();
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
    this.postContextMenuOverlayRef = this.overlay.create({
      positionStrategy
    });
    const ref: ComponentRef<PostContextMenuComponent> = this.postContextMenuOverlayRef.attach(this.postContextMenuPortal);
    ref.instance.post = post;
    ref.instance.contextMenuService = this;
    this.postContext = fromEvent<MouseEvent>(document, 'click')
      .pipe(
        filter(event => {
          const clickTarget = event.target as HTMLElement;
          return !!this.postContextMenuOverlayRef && !this.postContextMenuOverlayRef.overlayElement.contains(clickTarget);
        }),
        take(1)
      )
      .subscribe(() => this.closePostContextMenu());
  }

  closePostContextMenu() {
    if (this.postContext != null) {
      this.postContext.unsubscribe();
    }
    if (this.postContextMenuOverlayRef) {
      this.postContextMenuOverlayRef.dispose();
      this.postContextMenuOverlayRef = null;
    }
  }
}
