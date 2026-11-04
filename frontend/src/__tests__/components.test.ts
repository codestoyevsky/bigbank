import { createTestingPinia } from '@pinia/testing'
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AdBoard from '../components/AdBoard.vue'
import AdCard from '../components/AdCard.vue'
import ShopPanel from '../components/ShopPanel.vue'
import StatsBar from '../components/StatsBar.vue'
import { useGame } from '../stores/game'
import { ad, game, shop } from './fixtures'

function pinia(state: Partial<ReturnType<typeof useGame>['$state']> = {}) {
  return createTestingPinia({ createSpy: vi.fn, initialState: { game: { state: game(), ads: [], shop: [], ...state } } })
}

describe('AdCard', () => {
  it('shows the advisor assessment and emits solve', async () => {
    const wrapper = mount(AdCard, { props: { ad: ad({ recommended: true }), disabled: false, busy: false } })

    expect(wrapper.text()).toContain('Advisor\'s pick')
    expect(wrapper.text()).toContain('97%')
    expect(wrapper.text()).toContain('48.5')
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('solve')?.[0]).toEqual([expect.objectContaining({ adId: 'a1' })])
  })

  it('shows the advisor warning when there is one', () => {
    const wrapper = mount(AdCard, { props: { ad: ad({ warning: 'Careful' }), disabled: false, busy: false } })
    expect(wrapper.get('[data-test="warning"]').text()).toContain('Careful')
  })

  it('disables solving while busy', () => {
    const wrapper = mount(AdCard, { props: { ad: ad(), disabled: true, busy: true } })
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
    expect(wrapper.get('button').text()).toBe('Solving…')
  })
})

describe('AdBoard', () => {
  it('lists ads and solves the advisor pick in one click', async () => {
    const recommended = ad({ adId: 'best', recommended: true })
    const wrapper = mount(AdBoard, { global: { plugins: [pinia({ ads: [ad(), recommended] })] } })

    expect(wrapper.findAll('[data-test="ad"]')).toHaveLength(2)
    await wrapper.get('[data-test="solve-recommended"]').trigger('click')
    expect(useGame().solve).toHaveBeenCalledWith(expect.objectContaining({ adId: 'best' }))
  })

  it('tells the player when filters hide everything', async () => {
    const wrapper = mount(AdBoard, { global: { plugins: [pinia({ ads: [ad({ risk: 'DEADLY' })] })] } })
    await wrapper.get('[data-test="max-risk"]').setValue('SAFE')
    expect(wrapper.text()).toContain('No ads match your filters.')
  })
})

describe('ShopPanel', () => {
  it('disables items the player cannot afford and suggests a potion when low on lives', () => {
    const wrapper = mount(ShopPanel, {
      global: { plugins: [pinia({ state: game({ gold: 60, lives: 1 }), shop })] },
    })
    const [potion, claws] = wrapper.findAll('[data-test="shop-item"]')

    expect(potion!.get('button').attributes('disabled')).toBeUndefined()
    expect(potion!.text()).toContain('Low on lives')
    expect(claws!.get('button').attributes('disabled')).toBeDefined()
  })
})

describe('StatsBar', () => {
  it('shows stats and warns on low lives', () => {
    const wrapper = mount(StatsBar, { global: { plugins: [pinia({ state: game({ score: 1234, lives: 1 }) })] } })

    expect(wrapper.get('[data-test="stat-score"]').text()).toContain('1,234')
    expect(wrapper.get('[data-test="stat-lives"]').classes()).toContain('warn')
  })
})
