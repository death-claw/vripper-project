import { AppPreviewComponent } from './preview-tooltip.component';
import { Directive, Input, HostListener, ComponentRef, OnInit, ElementRef, NgZone, OnDestroy } from '@angular/core';
import { OverlayRef, Overlay, OverlayPositionBuilder } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';

@Directive({ selector: '[appPreview]' })
export class AppPreviewDirective implements OnInit, OnDestroy {
  private overlayRef: OverlayRef;
  tooltipPortal = new ComponentPortal(AppPreviewComponent);

  @Input() appPreview: string[];

  constructor(
    private overlayPositionBuilder: OverlayPositionBuilder,
    private elementRef: ElementRef,
    private overlay: Overlay,
    private ngZone: NgZone
  ) {}

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
    this.overlayRef = this.overlay.create({ positionStrategy, scrollStrategy: this.overlay.scrollStrategies.close() });
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
