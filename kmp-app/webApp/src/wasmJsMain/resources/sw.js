const CACHE_NAME = "transparencia-caninde-v1";
const STATIC_ASSETS = ["./", "./index.html", "./404.html", "./styles.css", "./favicon.svg", "./manifest.webmanifest"];

self.addEventListener("install", (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => cache.addAll(STATIC_ASSETS)).then(() => self.skipWaiting())
    );
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys().then((keys) =>
            Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)))
        ).then(() => self.clients.claim())
    );
});

self.addEventListener("fetch", (event) => {
    if (event.request.method !== "GET") return;
    const url = new URL(event.request.url);
    if (url.origin !== self.location.origin) return;

    const isRuntimeAsset = /\.(js|wasm)(\?|$)/i.test(url.pathname);
    if (isRuntimeAsset) {
        event.respondWith(
            fetch(event.request).catch(() => caches.match(event.request))
        );
        return;
    }

    event.respondWith(
        caches.match(event.request).then((cached) => cached || fetch(event.request))
    );
});
