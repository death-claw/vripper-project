import { Injectable } from '@angular/core';

@Injectable()
export class ServerService {

  private _baseUrl: string;
  private _wsBaseUrl: string;

  constructor() {}

  set baseUrl(baseUrl: string) {
    this._baseUrl = baseUrl;
  }

  get baseUrl(): string {
    return this._baseUrl;
  }

  set wsBaseUrl(wsBaseUrl: string) {
    this._wsBaseUrl = wsBaseUrl;
  }

  get wsBaseUrl(): string {
    return this._wsBaseUrl;
  }
}
