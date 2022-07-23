export interface Settings {
  desktopClipboard: boolean;
  maxEventLog: number;
  connectionSettings: ConnectionSettings;
  downloadSettings: DownloadSettings;
  viperSettings: ViperSettings;
}

export interface ConnectionSettings {
  maxThreads: number;
  maxTotalThreads: number;
  timeout: number;
  maxAttempts: number;
}

export interface DownloadSettings {
  downloadPath: string;
  autoStart: boolean;
  forceOrder: boolean;
  forumSubfolder: boolean;
  threadSubLocation: boolean;
  clearCompleted: boolean;
  appendPostId: boolean;
}

export interface ViperSettings {
  login: boolean;
  username: string;
  password: string;
  thanks: boolean;
  proxy: string;
}
