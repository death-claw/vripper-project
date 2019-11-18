import { PostsDataService } from './posts-data.service';
import { AppPreviewDirective } from './common/preview-tooltip.directive';
import { WsConnectionService } from './ws-connection.service';
import { AppService } from './app.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { MaterialModule } from './material.module';
import { PostsComponent } from './posts/posts.component';
import { PostDetailComponent } from './post-detail/post-detail.component';
import { FlexLayoutModule } from '@angular/flex-layout';
import { AppRoutingModule } from './app-routing.module';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AgGridModule } from 'ag-grid-angular';
import { PostProgressRendererComponent } from './posts/post-progress.renderer.component';
import { PostDetailsProgressRendererComponent } from './post-detail/post-details-progress.component';
import { LoginComponent } from './login/login.component';
import { XhrInterceptorService } from './xhr-interceptor.service';
import { HomeComponent } from './home/home.component';
import { SettingsComponent } from './settings/settings.component';

import { NgxElectronModule } from 'ngx-electron';
import { ServerService } from './server-service';
import { ConfirmDialogComponent } from './common/confirmation-component/confirmation-dialog';
import { SharedService } from './posts/shared.service';
import { MultiPostComponent } from './multi-post/multi-post.component';
import { OverlayModule } from '@angular/cdk/overlay';
import { AppPreviewComponent } from './common/preview-tooltip.component';
import { UrlRendererComponent } from './multi-post/url-renderer.component';
import { FilterComponent } from './filter/filter.component';
import { ScanComponent } from './scan/scan.component';
import { StatusBarComponent } from './status-bar/status-bar.component';
import { SelectionService } from './selection-service';
import { ToolbarComponent } from './toolbar/toolbar.component';

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
    FilterComponent,
    ScanComponent,
    StatusBarComponent,
    ToolbarComponent
  ],
  entryComponents: [
    PostDetailComponent,
    SettingsComponent,
    ConfirmDialogComponent,
    AppPreviewComponent,
    ScanComponent
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
      UrlRendererComponent
    ]),
    OverlayModule
  ],
  providers: [
    AppService,
    WsConnectionService,
    { provide: HTTP_INTERCEPTORS, useClass: XhrInterceptorService, multi: true },
    ServerService,
    SharedService,
    SelectionService,
    PostsDataService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
