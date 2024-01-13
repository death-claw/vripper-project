import { Log } from './log.model';
import { signal, WritableSignal } from '@angular/core';

export class LogRow extends Log {
  public statusSignal: WritableSignal<string>;
  public messageSignal: WritableSignal<string>;

  constructor(
    id: number,
    type: string,
    status: string,
    time: string,
    message: string
  ) {
    super(id, formatType(type), status, time, message);
    this.statusSignal = signal(status);
    this.messageSignal = signal(message);
  }
}

export function formatType(status: string) {
  switch (status) {
    case 'POST':
      return '🖼️ New gallery';
    case 'THREAD':
      return '🧵 New thread';
    case 'THANKS':
      return '👍 Sending a like ';
    case 'SCAN':
      return '🔍 Links scan';
    case 'METADATA':
    case 'METADATA_CACHE_MISS':
      return '🗄️ Loading post metadata';
    case 'QUEUED':
    case 'QUEUED_CACHE_MISS':
      return '📋 Loading multi-post link';
    case 'DOWNLOAD':
      return '📥 Download';
    default:
      return status;
  }
}
