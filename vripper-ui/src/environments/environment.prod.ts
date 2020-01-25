export const environment = {
  production: true,
  localhost: `${window.location.protocol}://${window.location.host}`,
  ws: `${window.location.protocol === 'https' ? 'wss' : 'ws'}://${window.location.host}`,
  version: '2.10.7'
};
