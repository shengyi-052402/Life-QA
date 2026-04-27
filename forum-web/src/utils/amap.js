let loaderPromise = null
let amapPromise = null

const AMAP_PLUGINS = [
  'AMap.AutoComplete',
  'AMap.PlaceSearch',
  'AMap.Geocoder',
  'AMap.ToolBar',
  'AMap.Scale'
]

export function hasAmapConfig() {
  return Boolean(import.meta.env.VITE_AMAP_KEY)
}

function loadAmapLoader() {
  if (window.AMapLoader) {
    return Promise.resolve(window.AMapLoader)
  }

  if (!loaderPromise) {
    loaderPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = 'https://webapi.amap.com/loader.js'
      script.async = true
      script.onload = () => resolve(window.AMapLoader)
      script.onerror = () => reject(new Error('高德地图加载器加载失败'))
      document.head.appendChild(script)
    })
  }

  return loaderPromise
}

export async function loadAmap() {
  if (!hasAmapConfig()) {
    throw new Error('未配置 VITE_AMAP_KEY')
  }

  if (!amapPromise) {
    if (import.meta.env.VITE_AMAP_SECURITY_CODE) {
      window._AMapSecurityConfig = {
        securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE
      }
    }

    amapPromise = loadAmapLoader().then(loader => loader.load({
      key: import.meta.env.VITE_AMAP_KEY,
      version: '2.0',
      plugins: AMAP_PLUGINS
    }))
  }

  return amapPromise
}
