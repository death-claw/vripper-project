import {PostContextMenuComponent} from './posts/post-context-menu.component';
import {PostDetailsContextMenuComponent} from './post-detail/post-details-context-menu.component';
import {AppPreviewDirective} from './common/preview-tooltip.directive';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {MaterialModule} from './material.module';
import {PostsComponent} from './posts/posts.component';
import {PostDetailComponent} from './post-detail/post-detail.component';
import {FlexLayoutModule} from '@angular/flex-layout';
import {AppRoutingModule} from './app-routing.module';
import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {AgGridModule} from 'ag-grid-angular';
import {PostProgressRendererComponent} from './posts/post-progress.renderer.component';
import {PostDetailsProgressRendererComponent} from './post-detail/post-details-progress.component';
import {LoginComponent} from './login/login.component';
import {XhrInterceptorService} from './xhr-interceptor.service';
import {HomeComponent} from './home/home.component';
import {SettingsComponent} from './settings/settings.component';

import {NgxElectronModule} from 'ngx-electron';
import {ConfirmDialogComponent} from './common/confirmation-component/confirmation-dialog';
import {MultiPostComponent} from './multi-post/multi-post.component';
import {OverlayModule} from '@angular/cdk/overlay';
import {AppPreviewComponent} from './common/preview-tooltip.component';
import {UrlRendererComponent} from './multi-post/url-renderer.component';
import {FilterComponent} from './filter/filter.component';
import {ScanComponent} from './scan/scan.component';
import {StatusBarComponent} from './status-bar/status-bar.component';
import {ToolbarComponent} from './toolbar/toolbar.component';
import {GrabQueueComponent} from './grab-queue/grab-queue.component';
import {UrlGrabRendererComponent} from './grab-queue/url-renderer.component';
import {GalleryComponent} from './gallery/gallery.component';
import {PhotoSwipeComponent} from './photo-swipe/photo-swipe.component';
import {ClipboardModule} from 'ngx-clipboard';
import {AlternativeTitleComponent} from './posts/alternative-title/alternative-title.component';

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
    GalleryComponent,
    PhotoSwipeComponent,
    AlternativeTitleComponent
  ],
  entryComponents: [
    PostDetailComponent,
    SettingsComponent,
    ConfirmDialogComponent,
    AppPreviewComponent,
    ScanComponent,
    MultiPostComponent,
    PostContextMenuComponent,
    PostDetailsContextMenuComponent,
    GalleryComponent
  ],
  imports: [
    BrowserAnimationsModule,
    FormsModule,
    HttpClientModule,
    MaterialModule,
    FlexLayoutModule,
    AppRoutingModule,
    NgxElectronModule,
    ReactiveFormsModule,
    AgGridModule.withComponents([
      PostProgressRendererComponent,
      PostDetailsProgressRendererComponent,
      UrlRendererComponent,
      UrlGrabRendererComponent
    ]),
    OverlayModule,
    ClipboardModule
  ],
  providers: [
    {provide: HTTP_INTERCEPTORS, useClass: XhrInterceptorService, multi: true}
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
