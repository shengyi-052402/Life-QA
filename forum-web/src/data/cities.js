/**
 * 全球主要城市数据
 * 格式: { name: string, nameZh: string, lat: number, lng: number, country: string }
 */
export const CITIES = [
  // 中国
  { name: 'Beijing', nameZh: '北京', lat: 39.9042, lng: 116.4074, country: '中国' },
  { name: 'Shanghai', nameZh: '上海', lat: 31.2304, lng: 121.4737, country: '中国' },
  { name: 'Guangzhou', nameZh: '广州', lat: 23.1291, lng: 113.2644, country: '中国' },
  { name: 'Shenzhen', nameZh: '深圳', lat: 22.5431, lng: 114.0579, country: '中国' },
  { name: 'Chengdu', nameZh: '成都', lat: 30.5728, lng: 104.0668, country: '中国' },
  { name: 'Hangzhou', nameZh: '杭州', lat: 30.2741, lng: 120.1551, country: '中国' },
  { name: 'Wuhan', nameZh: '武汉', lat: 30.5928, lng: 114.3055, country: '中国' },
  { name: "Xi'an", nameZh: '西安', lat: 34.3416, lng: 108.9398, country: '中国' },
  { name: 'Nanjing', nameZh: '南京', lat: 32.0603, lng: 118.7969, country: '中国' },
  { name: 'Chongqing', nameZh: '重庆', lat: 29.4316, lng: 106.9123, country: '中国' },
  { name: 'Tianjin', nameZh: '天津', lat: 39.3434, lng: 117.3616, country: '中国' },
  { name: 'Suzhou', nameZh: '苏州', lat: 31.2989, lng: 120.5853, country: '中国' },
  { name: 'Qingdao', nameZh: '青岛', lat: 36.0671, lng: 120.3826, country: '中国' },
  { name: 'Kunming', nameZh: '昆明', lat: 25.0461, lng: 102.7096, country: '中国' },
  { name: 'Harbin', nameZh: '哈尔滨', lat: 45.8038, lng: 126.5350, country: '中国' },

  // 亚洲
  { name: 'Tokyo', nameZh: '东京', lat: 35.6762, lng: 139.6503, country: '日本' },
  { name: 'Osaka', nameZh: '大阪', lat: 34.6937, lng: 135.5023, country: '日本' },
  { name: 'Seoul', nameZh: '首尔', lat: 37.5665, lng: 126.9780, country: '韩国' },
  { name: 'Singapore', nameZh: '新加坡', lat: 1.3521, lng: 103.8198, country: '新加坡' },
  { name: 'Bangkok', nameZh: '曼谷', lat: 13.7563, lng: 100.5018, country: '泰国' },
  { name: 'Kuala Lumpur', nameZh: '吉隆坡', lat: 3.1390, lng: 101.6869, country: '马来西亚' },
  { name: 'Jakarta', nameZh: '雅加达', lat: -6.2088, lng: 106.8456, country: '印度尼西亚' },
  { name: 'Manila', nameZh: '马尼拉', lat: 14.5995, lng: 120.9842, country: '菲律宾' },
  { name: 'Mumbai', nameZh: '孟买', lat: 19.0760, lng: 72.8777, country: '印度' },
  { name: 'New Delhi', nameZh: '新德里', lat: 28.6139, lng: 77.2090, country: '印度' },
  { name: 'Bangalore', nameZh: '班加罗尔', lat: 12.9716, lng: 77.5946, country: '印度' },
  { name: 'Taipei', nameZh: '台北', lat: 25.0330, lng: 121.5654, country: '中国台湾' },
  { name: 'Hong Kong', nameZh: '香港', lat: 22.3193, lng: 114.1694, country: '中国香港' },
  { name: 'Macau', nameZh: '澳门', lat: 22.1987, lng: 113.5439, country: '中国澳门' },
  { name: 'Hanoi', nameZh: '河内', lat: 21.0278, lng: 105.8342, country: '越南' },
  { name: 'Ho Chi Minh City', nameZh: '胡志明市', lat: 10.8231, lng: 106.6297, country: '越南' },
  { name: 'Dhaka', nameZh: '达卡', lat: 23.8103, lng: 90.4125, country: '孟加拉国' },
  { name: 'Karachi', nameZh: '卡拉奇', lat: 24.8607, lng: 67.0011, country: '巴基斯坦' },
  { name: 'Riyadh', nameZh: '利雅得', lat: 24.7136, lng: 46.6753, country: '沙特阿拉伯' },
  { name: 'Dubai', nameZh: '迪拜', lat: 25.2048, lng: 55.2708, country: '阿联酋' },
  { name: 'Istanbul', nameZh: '伊斯坦布尔', lat: 41.0082, lng: 28.9784, country: '土耳其' },
  { name: 'Tehran', nameZh: '德黑兰', lat: 35.6892, lng: 51.3890, country: '伊朗' },

  // 欧洲
  { name: 'London', nameZh: '伦敦', lat: 51.5074, lng: -0.1278, country: '英国' },
  { name: 'Paris', nameZh: '巴黎', lat: 48.8566, lng: 2.3522, country: '法国' },
  { name: 'Berlin', nameZh: '柏林', lat: 52.5200, lng: 13.4050, country: '德国' },
  { name: 'Madrid', nameZh: '马德里', lat: 40.4168, lng: -3.7038, country: '西班牙' },
  { name: 'Rome', nameZh: '罗马', lat: 41.9028, lng: 12.4964, country: '意大利' },
  { name: 'Milan', nameZh: '米兰', lat: 45.4654, lng: 9.1859, country: '意大利' },
  { name: 'Amsterdam', nameZh: '阿姆斯特丹', lat: 52.3676, lng: 4.9041, country: '荷兰' },
  { name: 'Brussels', nameZh: '布鲁塞尔', lat: 50.8503, lng: 4.3517, country: '比利时' },
  { name: 'Vienna', nameZh: '维也纳', lat: 48.2082, lng: 16.3738, country: '奥地利' },
  { name: 'Stockholm', nameZh: '斯德哥尔摩', lat: 59.3293, lng: 18.0686, country: '瑞典' },
  { name: 'Oslo', nameZh: '奥斯陆', lat: 59.9139, lng: 10.7522, country: '挪威' },
  { name: 'Copenhagen', nameZh: '哥本哈根', lat: 55.6761, lng: 12.5683, country: '丹麦' },
  { name: 'Helsinki', nameZh: '赫尔辛基', lat: 60.1699, lng: 24.9384, country: '芬兰' },
  { name: 'Zurich', nameZh: '苏黎世', lat: 47.3769, lng: 8.5417, country: '瑞士' },
  { name: 'Prague', nameZh: '布拉格', lat: 50.0755, lng: 14.4378, country: '捷克' },
  { name: 'Warsaw', nameZh: '华沙', lat: 52.2297, lng: 21.0122, country: '波兰' },
  { name: 'Moscow', nameZh: '莫斯科', lat: 55.7558, lng: 37.6173, country: '俄罗斯' },
  { name: 'St. Petersburg', nameZh: '圣彼得堡', lat: 59.9311, lng: 30.3609, country: '俄罗斯' },
  { name: 'Athens', nameZh: '雅典', lat: 37.9838, lng: 23.7275, country: '希腊' },
  { name: 'Lisbon', nameZh: '里斯本', lat: 38.7169, lng: -9.1395, country: '葡萄牙' },

  // 北美洲
  { name: 'New York', nameZh: '纽约', lat: 40.7128, lng: -74.0060, country: '美国' },
  { name: 'Los Angeles', nameZh: '洛杉矶', lat: 34.0522, lng: -118.2437, country: '美国' },
  { name: 'Chicago', nameZh: '芝加哥', lat: 41.8781, lng: -87.6298, country: '美国' },
  { name: 'San Francisco', nameZh: '旧金山', lat: 37.7749, lng: -122.4194, country: '美国' },
  { name: 'Seattle', nameZh: '西雅图', lat: 47.6062, lng: -122.3321, country: '美国' },
  { name: 'Boston', nameZh: '波士顿', lat: 42.3601, lng: -71.0589, country: '美国' },
  { name: 'Miami', nameZh: '迈阿密', lat: 25.7617, lng: -80.1918, country: '美国' },
  { name: 'Houston', nameZh: '休斯顿', lat: 29.7604, lng: -95.3698, country: '美国' },
  { name: 'Washington D.C.', nameZh: '华盛顿特区', lat: 38.9072, lng: -77.0369, country: '美国' },
  { name: 'Toronto', nameZh: '多伦多', lat: 43.6532, lng: -79.3832, country: '加拿大' },
  { name: 'Vancouver', nameZh: '温哥华', lat: 49.2827, lng: -123.1207, country: '加拿大' },
  { name: 'Montreal', nameZh: '蒙特利尔', lat: 45.5017, lng: -73.5673, country: '加拿大' },
  { name: 'Mexico City', nameZh: '墨西哥城', lat: 19.4326, lng: -99.1332, country: '墨西哥' },

  // 南美洲
  { name: 'São Paulo', nameZh: '圣保罗', lat: -23.5505, lng: -46.6333, country: '巴西' },
  { name: 'Rio de Janeiro', nameZh: '里约热内卢', lat: -22.9068, lng: -43.1729, country: '巴西' },
  { name: 'Buenos Aires', nameZh: '布宜诺斯艾利斯', lat: -34.6037, lng: -58.3816, country: '阿根廷' },
  { name: 'Santiago', nameZh: '圣地亚哥', lat: -33.4489, lng: -70.6693, country: '智利' },
  { name: 'Bogotá', nameZh: '波哥大', lat: 4.7110, lng: -74.0721, country: '哥伦比亚' },
  { name: 'Lima', nameZh: '利马', lat: -12.0464, lng: -77.0428, country: '秘鲁' },

  // 非洲
  { name: 'Cairo', nameZh: '开罗', lat: 30.0444, lng: 31.2357, country: '埃及' },
  { name: 'Lagos', nameZh: '拉各斯', lat: 6.5244, lng: 3.3792, country: '尼日利亚' },
  { name: 'Nairobi', nameZh: '内罗毕', lat: -1.2921, lng: 36.8219, country: '肯尼亚' },
  { name: 'Johannesburg', nameZh: '约翰内斯堡', lat: -26.2041, lng: 28.0473, country: '南非' },
  { name: 'Cape Town', nameZh: '开普敦', lat: -33.9249, lng: 18.4241, country: '南非' },
  { name: 'Casablanca', nameZh: '卡萨布兰卡', lat: 33.5731, lng: -7.5898, country: '摩洛哥' },
  { name: 'Addis Ababa', nameZh: '亚的斯亚贝巴', lat: 9.1450, lng: 40.4897, country: '埃塞俄比亚' },

  // 大洋洲
  { name: 'Sydney', nameZh: '悉尼', lat: -33.8688, lng: 151.2093, country: '澳大利亚' },
  { name: 'Melbourne', nameZh: '墨尔本', lat: -37.8136, lng: 144.9631, country: '澳大利亚' },
  { name: 'Brisbane', nameZh: '布里斯班', lat: -27.4698, lng: 153.0251, country: '澳大利亚' },
  { name: 'Auckland', nameZh: '奥克兰', lat: -36.8509, lng: 174.7645, country: '新西兰' },
]

/**
 * 搜索城市（支持中文名和英文名）
 * @param {string} query 搜索关键字
 * @returns {Array} 匹配的城市列表
 */
export function searchCities(query) {
  if (!query || !query.trim()) return []
  const q = query.trim().toLowerCase()
  return CITIES.filter(city =>
    city.name.toLowerCase().includes(q) ||
    city.nameZh.includes(q) ||
    city.country.includes(q)
  ).slice(0, 10) // 最多返回 10 条
}
