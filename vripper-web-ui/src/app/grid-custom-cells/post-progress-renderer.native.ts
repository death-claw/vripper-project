import {GridApi, ICellRendererParams, IRowNode} from 'ag-grid-community';
import {PostContextMenuService} from '../services/post-context-menu.service';
import {ProgressRendererNative} from './progress-renderer.native';

export class PostProgressRendererNative extends ProgressRendererNative {

  private gridApi!: GridApi;
  private node!: IRowNode;
  private contextMenuService!: PostContextMenuService;

  override destroy(): void {
    if (this.gui) {
      this.gui.removeEventListener('contextmenu', this.context.bind(this));
    }
  }

  override init(params: ICellRendererParams): void {
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
