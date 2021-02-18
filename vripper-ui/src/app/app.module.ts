import {PostContextMenuComponent} from './posts/context-menu/post-context-menu.component';
import {AppPreviewDirective} from './preview-tooltip/preview-tooltip.directive';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {MaterialModule} from './material.module';
import {PostsComponent} from './posts/posts.component';
import {PhotosComponent} from './photos/photos.component';
import {FlexLayoutModule} from '@angular/flex-layout';
import {AppRoutingModule} from './app-routing.module';
import {HttpClientModule} from '@angular/common/http';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {AgGridModule} from 'ag-grid-angular';
import {HomeComponent} from './home/home.component';
import {SettingsComponent} from './settings/settings.component';

import {ConfirmDialogComponent} from './confirmation-component/confirmation-dialog';
import {MultiPostItemsComponent} from './multi-post-items/multi-post-items.component';
import {OverlayModule} from '@angular/cdk/overlay';
import {AppPreviewComponent} from './preview-tooltip/preview-tooltip.component';
import {ScanComponent} from './scan/scan.component';
import {StatusBarComponent} from './status-bar/status-bar.component';
import {ToolbarComponent} from './toolbar/toolbar.component';
import {MultiPostGridComponent} from './multi-post-grid/multi-post-grid.component';
import {AlternativeTitleComponent} from './posts/alternative-title/alternative-title.component';
import {NgxElectronModule} from 'ngx-electron';
import {MatIconRegistry} from '@angular/material/icon';
import {DomSanitizer} from '@angular/platform-browser';
import {EventLogComponent} from './event-log/event-log.component';
import {EventLogMessageDialogComponent} from './event-log/message-dialog/event-log-message-dialog.component';
import {AboutComponent} from './about/about-component';

@NgModule({
  declarations: [
    AppComponent,
    PostsComponent,
    PhotosComponent,
    HomeComponent,
    SettingsComponent,
    ConfirmDialogComponent,
    MultiPostItemsComponent,
    AppPreviewComponent,
    AppPreviewDirective,
    ScanComponent,
    StatusBarComponent,
    ToolbarComponent,
    MultiPostGridComponent,
    PostContextMenuComponent,
    AlternativeTitleComponent,
    EventLogComponent,
    EventLogMessageDialogComponent,
    AboutComponent
  ],
  imports: [
    BrowserAnimationsModule,
    FormsModule,
    HttpClientModule,
    MaterialModule,
    FlexLayoutModule,
    AppRoutingModule,
    ReactiveFormsModule,
    AgGridModule,
    OverlayModule,
    NgxElectronModule
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor(matIconRegistry: MatIconRegistry, domSanitizer: DomSanitizer) {
    matIconRegistry.addSvgIconSet(
      domSanitizer.bypassSecurityTrustResourceUrl('./assets/mdi.svg')
    );
  }
}
