export function formatBytes(bytes: number, decimals = 2) {
  if (!+bytes) return '0 Bytes';

  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = [
    'Bytes',
    'KiB',
    'MiB',
    'GiB',
    'TiB',
    'PiB',
    'EiB',
    'ZiB',
    'YiB',
  ];

  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

export function statusIcon(status: string): string {
  switch (status) {
    case 'DOWNLOADING':
      return 'download_for_offline';
    case 'PENDING':
      return 'pending';
    case 'FINISHED':
      return 'check_circle';
    case 'ERROR':
      return 'error';
    case 'STOPPED':
      return 'pause_circle';
    default:
      return 'question_mark';
  }
}

export function progress(done: number, total: number): number {
  return done === 0 || total === 0 ? 0 : (done / total) * 100;
}

export function totalFormatter(
  done: number,
  total: number,
  downloaded: number
): string {
  return `${done}/${total} (${formatBytes(downloaded)})`;
}

export function isDisplayed(columns: string[], column: string): boolean {
  return columns.findIndex(v => v === column) > -1;
}
