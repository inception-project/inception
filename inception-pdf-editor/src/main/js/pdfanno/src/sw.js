const version = '#VERSION'
const baseDir = '#BASEDIR'
console.log('service worker version:', version)

const CACHE_NAME = 'pdfanno-cache-v' + version
const urlsToCache = [
    // `/${baseDir}/embedded-sample.html`,
    // `/${baseDir}/embedded-sample.bundle.js`,
    // `/${baseDir}/pdfanno.core.bundle.js`,
    // `/${baseDir}/pages/viewer.html`,
    // `/${baseDir}/pages/viewer.css`,
    // `/${baseDir}/pages/compatibility.js`,
    // `/${baseDir}/pages/l10n.js`,
    // `/${baseDir}/pages/debugger.js`,
    // `/${baseDir}/pages/viewer.js`,
    // `/${baseDir}/pages/images/texture.png`,
    // `/${baseDir}/pages/images/toolbarButton-menuArrows.png`,
    // `/${baseDir}/pages/images/toolbarButton-viewThumbnail.png`,
    // `/${baseDir}/pages/images/toolbarButton-viewOutline.png`,
    // `/${baseDir}/pages/images/toolbarButton-viewAttachments.png`,
    // `/${baseDir}/pages/images/toolbarButton-sidebarToggle.png`,
    // `/${baseDir}/pages/images/toolbarButton-pageUp.png`,
    // `/${baseDir}/pages/images/toolbarButton-secondaryToolbarToggle.png`,
    // `/${baseDir}/pages/images/toolbarButton-zoomIn.png`,
    // `/${baseDir}/pages/images/toolbarButton-zoomOut.png`,
    // `/${baseDir}/pages/images/toolbarButton-pageDown.png`,
    // `/${baseDir}/pages/images/shadow.png`,
    // `/${baseDir}/pages/images/loading-icon.gif`,
    // `/${baseDir}/pages/images/loading-small.png`,
    // `/${baseDir}/pages/locale/locale.properties`,
    // `/${baseDir}/pages/locale/ja/viewer.properties`,
    // `/${baseDir}/build/pdf.js`,
    // `/${baseDir}/build/pdf.worker.js`,
    // 'https://code.jquery.com/jquery-3.1.1.min.js',
]
console.log('urlsToCache:', urlsToCache.join('\n'))

self.addEventListener('install', event => {
    console.log('install:', event)
    self.skipWaiting()
    event.waitUntil(
        caches.open(CACHE_NAME).then(cache => {
            console.log('Opened cache.')
            return cache.addAll(urlsToCache)
        })
    )
})

self.addEventListener('fetch', event => {
    // console.log('fetch:', event.request.method, event.request.url)
    event.respondWith(
        caches.match(event.request, { ignoreSearch : true }).then(response => {
            if (response) {
                return response
            }
            return fetch(event.request)
        })
    )
})

self.addEventListener('activate', event => {
    console.log('activate:', event)
    event.waitUntil(
        caches.keys().then(cacheNames => {
            return Promise.all(
                cacheNames.map(cacheName => {
                    if (cacheName != CACHE_NAME) {
                        return caches.delete(cacheName)
                    }
                })
            )
        })
    )
})
