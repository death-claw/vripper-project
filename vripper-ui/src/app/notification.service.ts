import { Injectable } from '@angular/core';
import { AppService } from './app.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  constructor(private appService: AppService) {}

  notifyFromGrabQueue(title: string, body: string) {
    if (!this.appService.settings.notification) {
      return;
    }
    if (!('Notification' in window)) {
      return;
    } else if (Notification.permission === 'granted') {
      const notification = new Notification(title, { body: body, icon: 'assets/icon.png' });
    } else if (Notification.permission !== 'denied') {
      Notification.requestPermission().then(function(permission) {
        if (permission === 'granted') {
          const notification = new Notification(title, { body: body, icon: 'assets/icon.png' });
        }
      });
    }
  }
}
