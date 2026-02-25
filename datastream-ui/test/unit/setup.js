// Vue 3 测试设置
import { config } from '@vue/test-utils'

// 全局配置
config.global.stubs = {
  // 可以在这里添加全局的组件存根
}

// 设置全局属性
config.global.mocks = {
  $t: (key) => key,
  $route: {
    path: '/',
    name: 'home'
  },
  $router: {
    push: jest.fn(),
    replace: jest.fn()
  }
}
