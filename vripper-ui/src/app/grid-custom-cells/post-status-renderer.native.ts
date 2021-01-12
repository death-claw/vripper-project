import {StatusRendererNative} from './status-renderer.native';
import {GridApi, ICellRendererParams, RowNode} from 'ag-grid-community';
import {PostContextMenuService} from '../services/post-context-menu.service';

export class PostStatusRendererNative extends StatusRendererNative {

  private gridApi: GridApi;
  private node: RowNode;
  private contextMenuService: PostContextMenuService;

  destroy(): void {
    if (this.gui) {
      this.gui.removeEventListener('contextmenu', this.context.bind(this));
    }
  }

  init(params: ICellRendererParams): void {
    super.init(params);
    // @ts-ignore
    this.contextMenuService = params.contextMenuService;
    this.gridApi = params.api;
    this.node = params.node;
    this.gui.addEventListener('contextmenu', this.context.bind(this));
  }

  context(event: MouseEvent) {
    event.preventDefault();
    this.gridApi.getSelectedNodes().forEach(e => e.setSelected(false));
    this.node.setSelected(true);
    this.contextMenuService.openPostContextMenu(event, this.node.data);
  }
}
