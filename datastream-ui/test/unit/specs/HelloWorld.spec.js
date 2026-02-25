import { mount } from '@vue/test-utils'
import HelloWorld from '@/views/components/HelloWorld'

describe('HelloWorld.vue', () => {
  it('should render correct contents', () => {
    const wrapper = mount(HelloWorld)
    expect(wrapper.find('.hello h1').text()).toBe('Welcome to Your Vue.js App')
  })
})
