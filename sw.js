const CACHE='mybills-v125-restore';
const CORE=['./','./index.html','./manifest.webmanifest','./mybills-v125-192.png','./mybills-v125-512.png','./mybills-v125-favicon.png'];
self.addEventListener('install',e=>{self.skipWaiting();e.waitUntil(caches.open(CACHE).then(c=>c.addAll(CORE)))});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim()))});
self.addEventListener('fetch',e=>{if(e.request.mode==='navigate'){e.respondWith(fetch(e.request,{cache:'no-store'}).catch(()=>caches.match('./index.html')));return;}e.respondWith(fetch(e.request,{cache:'no-store'}).catch(()=>caches.match(e.request)))});
