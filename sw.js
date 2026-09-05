const CACHE = 'mybills-v118';
const STATIC = ['./manifest.webmanifest','./icons/icon-192.png','./icons/icon-512.png'];

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(STATIC)));
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
  );
  self.clients.claim();
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  const req = event.request;

  // HTML/navigation must always try the newest network copy first.
  if (req.mode === 'navigate' || req.destination === 'document') {
    event.respondWith(
      fetch(req, {cache:'no-store'}).then(resp => {
        const copy = resp.clone();
        caches.open(CACHE).then(c => c.put('./index.html', copy));
        return resp;
      }).catch(() => caches.match('./index.html'))
    );
    return;
  }

  // Static assets can use cache, but refresh in the background.
  event.respondWith(
    caches.match(req).then(hit => {
      const fresh = fetch(req, {cache:'no-cache'}).then(resp => {
        const copy = resp.clone();
        caches.open(CACHE).then(c => c.put(req, copy));
        return resp;
      }).catch(() => hit);
      return hit || fresh;
    })
  );
});
