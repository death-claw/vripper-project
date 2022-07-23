import {AppPreviewComponent} from './preview-tooltip.component';
import {ComponentRef, Directive, ElementRef, HostListener, Input, NgZone, OnDestroy, OnInit} from '@angular/core';
import {Overlay, OverlayPositionBuilder, OverlayRef} from '@angular/cdk/overlay';
import {ComponentPortal} from '@angular/cdk/portal';

@Directive({selector: '[appPreview]'})
export class AppPreviewDirective implements OnInit, OnDestroy {
  tooltipPortal = new ComponentPortal(AppPreviewComponent);
  @Input() appPreview!: string[];
  private overlayRef!: OverlayRef;

  constructor(
    private overlayPositionBuilder: OverlayPositionBuilder,
    private elementRef: ElementRef,
    private overlay: Overlay,
    private ngZone: NgZone
  ) {
  }

  ngOnInit() {
    const positionStrategy = this.overlayPositionBuilder
      .flexibleConnectedTo(this.elementRef)
      .withPush(true)
      .withGrowAfterOpen(true)
      .withPositions([
        {
          originX: 'center',
          originY: 'bottom',
          overlayX: 'center',
          overlayY: 'top'
        },
        {
          originX: 'center',
          originY: 'top',
          overlayX: 'center',
          overlayY: 'bottom'
        }
      ]);
    this.overlayRef = this.overlay.create({positionStrategy, scrollStrategy: this.overlay.scrollStrategies.close()});
  }

  ngOnDestroy() {
    this.ngZone.run(() => {
      this.overlayRef.dispose();
    });
  }

  @HostListener('mouseenter')
  show() {
    this.ngZone.run(() => {
      const tooltipRef: ComponentRef<AppPreviewComponent> = this.overlayRef.attach(this.tooltipPortal);
      tooltipRef.instance.links = this.appPreview;
    });
  }

  @HostListener('mouseout')
  hide() {
    this.ngZone.run(() => {
      this.overlayRef.detach();
    });
  }
}
