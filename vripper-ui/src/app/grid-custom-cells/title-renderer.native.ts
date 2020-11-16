import {GridApi, ICellRendererComp, ICellRendererParams, RowNode} from 'ag-grid-community';
import {PostContextMenuService} from '../services/post-context-menu.service';
import {Overlay, OverlayPositionBuilder, OverlayRef} from '@angular/cdk/overlay';
import {ComponentPortal} from '@angular/cdk/portal';
import {AppPreviewComponent} from '../preview-tooltip/preview-tooltip.component';
import {ComponentRef, NgZone} from '@angular/core';

export class TitleRendererNative implements ICellRendererComp {
  private gui: HTMLElement;
  private text: HTMLSpanElement;
  private gridApi: GridApi;
  private node: RowNode;
  private contextMenuService: PostContextMenuService;
  private icon: HTMLSpanElement;

  private overlayPositionBuilder: OverlayPositionBuilder;
  private overlay: Overlay;
  private zone: NgZone;

  private overlayRef: OverlayRef;
  tooltipPortal = new ComponentPortal(AppPreviewComponent);
  appPreview: string[];

  destroy(): void {
    if(this.gui) {
      this.gui.removeEventListener('contextmenu', this.context.bind(this));
    }
    if(this.overlayRef) {
      this.overlayRef.dispose();
    }

    if(this.icon) {
      this.icon.removeEventListener('mouseenter', this.mouseenter.bind(this));
      this.icon.removeEventListener('mouseleave', this.mouseout.bind(this));
    }
  }

  getGui(): HTMLElement {
    return this.gui;
  }

  init(params: ICellRendererParams): void {
    // @ts-ignore
    this.contextMenuService = params.contextMenuService;
    // @ts-ignore
    this.overlayPositionBuilder = params.overlayPositionBuilder;
    // @ts-ignore
    this.overlay = params.overlay;
    // @ts-ignore
    this.zone = params.zone;

    this.gridApi = params.api;
    this.node = params.node;
    this.appPreview = this.node.data.previews;
    this.gui = document.createElement('div');
    this.gui.classList.add('text-cell');
    this.gui.setAttribute('style', 'display: flex; align-items: center');
    this.icon = document.createElement('span');
    this.icon.setAttribute('style', 'height: 24px; cursor: pointer');
    this.icon.classList.add('cell-icon');
    this.icon.innerHTML = `<svg style="width:24px;height:24px" viewBox="0 0 24 24">
    <path fill="currentColor" d="M8.5,13.5L11,16.5L14.5,12L19,18H5M21,19V5C21,3.89 20.1,3 19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19Z" />
</svg>`;
    this.text = document.createElement('span');
    this.text.classList.add('text-cell');
    this.text.innerText = params.value;
    this.text.setAttribute('title', params.value);
    this.gui.append(this.icon, this.text);
    this.gui.addEventListener('contextmenu', this.context.bind(this));

    const positionStrategy = this.overlayPositionBuilder
      .flexibleConnectedTo(this.icon)
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

    this.icon.addEventListener('mouseenter', this.mouseenter.bind(this));
    this.icon.addEventListener('mouseleave', this.mouseout.bind(this));
  }

  mouseenter() {
    this.zone.run(() => {
      const tooltipRef: ComponentRef<AppPreviewComponent> = this.overlayRef.attach(this.tooltipPortal);
      tooltipRef.instance.links = this.appPreview;
    });
  }

  mouseout() {
    this.zone.run(() => this.overlayRef.detach());
  }

  refresh(params: ICellRendererParams): boolean {
    this.text.innerText = params.value;
    this.text.setAttribute('title', params.value);
    return true;
  }

  context(event: MouseEvent) {
    event.preventDefault();
    this.gridApi.getSelectedNodes().forEach(e => e.setSelected(false));
    this.node.setSelected(true);
    this.contextMenuService.openPostContextMenu(event, this.node.data);
  }

}
