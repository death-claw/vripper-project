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
import { MenuRendererComponent } from './posts/menu.renderer.component';
import { PostDetailsProgressRendererComponent } from './post-detail/post-details-progress.component';
import { LoginComponent } from './login/login.component';
import { XhrInterceptorService } from './xhr-interceptor.service';
import { HomeComponent } from './home/home.component';
import { SettingsComponent } from './settings/settings.component';

import { NgxElectronModule } from 'ngx-electron';
import { ServerService } from './server-service';
import { ConfirmDialogComponent } from './common/confirmation-component/confirmation-dialog';

@NgModule({
  declarations: [
    AppComponent,
    PostsComponent,
    PostDetailComponent,
    PostProgressRendererComponent,
    MenuRendererComponent,
    PostDetailsProgressRendererComponent,
    LoginComponent,
    HomeComponent,
    SettingsComponent,
    ConfirmDialogComponent
  ],
  entryComponents: [
    PostDetailComponent,
    SettingsComponent,
    ConfirmDialogComponent
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
    AgGridModule.withComponents([PostProgressRendererComponent, MenuRendererComponent, PostDetailsProgressRendererComponent])
  ],
  providers: [AppService, WsConnectionService, { provide: HTTP_INTERCEPTORS, useClass: XhrInterceptorService, multi: true }, ServerService],
  bootstrap: [AppComponent]
})
export class AppModule { }
