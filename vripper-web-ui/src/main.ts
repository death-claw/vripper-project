import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/app.component';
import { provideAnimations } from '@angular/platform-browser/animations';
import { BASE_URL, WS_BASE_URL } from './app/base-url.token';
import { HttpClientModule } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';

bootstrapApplication(AppComponent, {
  providers: [
    provideAnimations(),
    importProvidersFrom(HttpClientModule),
    {
      provide: BASE_URL,
      useFactory: () => '',
    },
    {
      provide: WS_BASE_URL,
      useFactory: () => {
        const loc = window.location;
        let newUri;
        if (loc.protocol === 'https:') {
          newUri = 'wss:';
        } else {
          newUri = 'ws:';
        }
        newUri += `//${loc.host}`;
        return newUri;
      },
    },
  ],
});
