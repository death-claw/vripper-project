export interface Settings {
  connectionSettings: ConnectionSettings;
  downloadSettings: DownloadSettings;
  viperSettings: ViperSettings;
  systemSettings: SystemSettings;
}

export interface ConnectionSettings {
  maxConcurrentPerHost: number;
  maxGlobalConcurrent: number;
  timeout: number;
  maxAttempts: number;
}

export interface DownloadSettings {
  downloadPath: string;
  autoStart: boolean;
  autoQueueThreshold: number;
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

export interface SystemSettings {
  tempPath: string;
  cachePath: string;
  maxEventLog: number;
}
