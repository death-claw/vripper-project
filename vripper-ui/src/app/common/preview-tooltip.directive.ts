import { AppPreviewComponent } from './preview-tooltip.component';
import { Directive, Input, HostListener, ComponentRef, OnInit, ElementRef, NgZone } from '@angular/core';
import { OverlayRef, Overlay, OverlayPositionBuilder } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';

@Directive({ selector: '[appPreview]' })
export class AppPreviewDirective implements OnInit {
  private overlayRef: OverlayRef;
  tooltipPortal = new ComponentPortal(AppPreviewComponent);

  @Input() appPreview: string[];

  constructor(private overlayPositionBuilder: OverlayPositionBuilder,
    private elementRef: ElementRef,
    private overlay: Overlay,
    private ngZone: NgZone) {}

  ngOnInit() {
    const positionStrategy = this.overlayPositionBuilder
    .flexibleConnectedTo(this.elementRef)
    .withPush(true)
    .withGrowAfterOpen(true)
    .withPositions([{
      originX: 'start',
      originY: 'bottom',
      overlayX: 'start',
      overlayY: 'top',
    }]);
    this.overlayRef = this.overlay.create({ positionStrategy });
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
