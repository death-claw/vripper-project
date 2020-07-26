import {PostContextMenuComponent} from './posts/context-menu/post-context-menu.component';
import {PostDetailsContextMenuComponent} from './post-progress/context-menu/post-details-context-menu.component';
import {AppPreviewDirective} from './domain/preview-tooltip.directive';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {MaterialModule} from './material.module';
import {PostsComponent} from './posts/posts.component';
import {PostDetailComponent} from './post-progress/post-detail.component';
import {FlexLayoutModule} from '@angular/flex-layout';
import {AppRoutingModule} from './app-routing.module';
import {HttpClientModule} from '@angular/common/http';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {AgGridModule} from 'ag-grid-angular';
import {PostProgressRendererComponent} from './posts/renderer/post-progress.renderer.component';
import {PostDetailsProgressRendererComponent} from './post-progress/renderer/post-details-progress.component';
import {LoginComponent} from './login/login.component';
import {HomeComponent} from './home/home.component';
import {SettingsComponent} from './settings/settings.component';

import {ConfirmDialogComponent} from './confirmation-component/confirmation-dialog';
import {MultiPostComponent} from './multi-post/multi-post.component';
import {OverlayModule} from '@angular/cdk/overlay';
import {AppPreviewComponent} from './domain/preview-tooltip.component';
import {UrlRendererComponent} from './multi-post/renderer/url-renderer.component';
import {FilterComponent} from './filter/filter.component';
import {ScanComponent} from './scan/scan.component';
import {StatusBarComponent} from './status-bar/status-bar.component';
import {ToolbarComponent} from './toolbar/toolbar.component';
import {GrabQueueComponent} from './grab-queue/grab-queue.component';
import {UrlGrabRendererComponent} from './grab-queue/renderer/url-renderer.component';
import {AlternativeTitleComponent} from './posts/alternative-title/alternative-title.component';
import {NgxElectronModule} from 'ngx-electron';

@NgModule({
  declarations: [
    AppComponent,
    PostsComponent,
    PostDetailComponent,
    PostProgressRendererComponent,
    PostDetailsProgressRendererComponent,
    LoginComponent,
    HomeComponent,
    SettingsComponent,
    ConfirmDialogComponent,
    MultiPostComponent,
    AppPreviewComponent,
    AppPreviewDirective,
    UrlRendererComponent,
    UrlGrabRendererComponent,
    FilterComponent,
    ScanComponent,
    StatusBarComponent,
    ToolbarComponent,
    GrabQueueComponent,
    PostContextMenuComponent,
    PostDetailsContextMenuComponent,
    AlternativeTitleComponent
  ],
  imports: [
    BrowserAnimationsModule,
    FormsModule,
    HttpClientModule,
    MaterialModule,
    FlexLayoutModule,
    AppRoutingModule,
    ReactiveFormsModule,
    AgGridModule.withComponents([
      PostProgressRendererComponent,
      PostDetailsProgressRendererComponent,
      UrlRendererComponent,
      UrlGrabRendererComponent
    ]),
    OverlayModule,
    NgxElectronModule
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
