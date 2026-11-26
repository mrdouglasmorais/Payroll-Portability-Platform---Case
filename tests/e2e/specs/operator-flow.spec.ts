import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'

/**
 * UI do operador contra o stack Compose ativo.
 * Preferir agenda seedada da Central para ben-ui / conv-1 on a clean volume.
 */
test('operador autoriza, seleciona parcela, envia e chega a status terminal', async ({
  page,
}) => {
  await page.setViewportSize({ width: 1280, height: 800 })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Console operacional' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Fluxo de portabilidade' })).toBeVisible()

  await page.getByLabel('ID do beneficiário').fill('ben-ui')
  await page.getByLabel('ID do convênio').fill('conv-1')
  await page.getByRole('button', { name: 'Autorizar e carregar agenda' }).click()

  await expect(page.getByRole('heading', { name: '2. Agenda' })).toBeVisible({
    timeout: 30_000,
  })
  const firstCheckbox = page.getByRole('checkbox').first()
  await expect(firstCheckbox).toBeVisible({ timeout: 30_000 })
  await firstCheckbox.check()
  await page.getByRole('button', { name: 'Continuar' }).click()

  await page.getByLabel('Credor cessionário').fill('creditor-playwright')
  await page.getByLabel('Banco').fill('001')
  await page.getByLabel('Agência').fill('1234')
  await page.getByLabel('Conta').fill('77777-0')
  await page.getByRole('button', { name: 'Revisar' }).click()

  await expect(page.getByText(/\*{4}77-0/)).toBeVisible()
  await page.getByRole('button', { name: 'Enviar portabilidade' }).click()

  await expect(page.locator('[data-status]')).toBeVisible({ timeout: 30_000 })
  await expect(page.locator('[data-status="CONFIRMADA"]')).toHaveText('Confirmada', {
    timeout: 90_000,
  })
  await expect(page.getByText(/consultando API local/)).toHaveCount(0)
})

test('navegação por teclado no consentimento e axe no primeiro viewport', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/')

  const beneficiary = page.getByLabel('ID do beneficiário')
  await beneficiary.focus()
  await expect(beneficiary).toBeFocused()
  await beneficiary.fill('ben-ui')
  await page.keyboard.press('Tab')
  await expect(page.getByLabel('ID do convênio')).toBeFocused()
  await page.keyboard.type('conv-1')
  await page.keyboard.press('Tab')
  await expect(page.getByRole('button', { name: 'Autorizar e carregar agenda' })).toBeFocused()

  const accessibility = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa'])
    .analyze()
  expect(
    accessibility.violations,
    JSON.stringify(
      accessibility.violations.map((v) => ({ id: v.id, impact: v.impact, help: v.help })),
      null,
      2,
    ),
  ).toEqual([])
})
