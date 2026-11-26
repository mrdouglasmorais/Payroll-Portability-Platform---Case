import AxeBuilder from '@axe-core/playwright'
import { expect, type Page, test } from '@playwright/test'

/**
 * UI do operador contra o stack Compose ativo.
 * Preferir agenda seedada da Central para ben-ui / conv-1 on a clean volume.
 *
 * Accessibility: axe (wcag2a/wcag2aa) + keyboard smoke across wizard steps.
 * This is a regression harness — not a claim of full WCAG 2.2 AA certification.
 */

async function assertNoAxeViolations(page: Page, context: string) {
  const accessibility = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa'])
    .analyze()
  expect(
    accessibility.violations,
    `${context}: ${JSON.stringify(
      accessibility.violations.map((v) => ({ id: v.id, impact: v.impact, help: v.help })),
      null,
      2,
    )}`,
  ).toEqual([])
}

async function authorizeToAgenda(page: Page) {
  await page.getByLabel('ID do beneficiário').fill('ben-ui')
  await page.getByLabel('ID do convênio').fill('conv-1')
  await page.getByRole('button', { name: 'Autorizar e carregar agenda' }).click()
  await expect(page.getByRole('heading', { name: '2. Agenda' })).toBeVisible({
    timeout: 30_000,
  })
}

async function selectFirstParcelAndContinue(page: Page) {
  const firstCheckbox = page.getByRole('checkbox').first()
  await expect(firstCheckbox).toBeVisible({ timeout: 30_000 })
  await firstCheckbox.check()
  await page.getByRole('button', { name: 'Continuar' }).click()
  await expect(page.getByRole('heading', { name: '3. Destino' })).toBeVisible()
}

async function fillDestinationAndReview(page: Page) {
  await page.getByLabel('Credor cessionário').fill('creditor-playwright')
  await page.getByLabel('Banco').fill('001')
  await page.getByLabel('Agência').fill('1234')
  await page.getByLabel('Conta').fill('77777-0')
  await page.getByRole('button', { name: 'Revisar' }).click()
  await expect(page.getByRole('heading', { name: '4. Revisão e envio' })).toBeVisible()
  await expect(page.getByText(/\*{4}77-0/)).toBeVisible()
}

async function submitAndWaitTerminal(page: Page) {
  await page.getByRole('button', { name: 'Enviar portabilidade' }).click()
  await expect(page.getByRole('heading', { name: '5. Linha do tempo da operação' })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.locator('[data-status]')).toBeVisible({ timeout: 30_000 })
  await expect(page.locator('[data-status="CONFIRMADA"]')).toHaveText('Confirmada', {
    timeout: 90_000,
  })
  await expect(page.getByText(/consultando API local/)).toHaveCount(0)
  await expect(page.getByRole('heading', { name: 'Eventos' })).toBeVisible()
}

test('operador autoriza, seleciona parcela, envia e chega a status terminal', async ({
  page,
}) => {
  await page.setViewportSize({ width: 1280, height: 800 })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Console operacional' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Fluxo de portabilidade' })).toBeVisible()

  await authorizeToAgenda(page)
  await selectFirstParcelAndContinue(page)
  await fillDestinationAndReview(page)
  await submitAndWaitTerminal(page)
})

for (const viewport of [
  { name: 'desktop', width: 1280, height: 800 },
  { name: 'mobile', width: 390, height: 844 },
] as const) {
  test(`a11y smoke (${viewport.name}): teclado + axe nas etapas e erro`, async ({ page }) => {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    await page.goto('/')

    // 1. Autorização — keyboard + axe
    const beneficiary = page.getByLabel('ID do beneficiário')
    await beneficiary.focus()
    await expect(beneficiary).toBeFocused()
    await beneficiary.fill('ben-ui')
    await page.keyboard.press('Tab')
    await expect(page.getByLabel('ID do convênio')).toBeFocused()
    await page.keyboard.type('conv-1')
    await page.keyboard.press('Tab')
    await expect(page.getByRole('button', { name: 'Autorizar e carregar agenda' })).toBeFocused()
    await assertNoAxeViolations(page, `autorização (${viewport.name})`)

    await page.getByRole('button', { name: 'Autorizar e carregar agenda' }).click()
    await expect(page.getByRole('heading', { name: '2. Agenda' })).toBeVisible({
      timeout: 30_000,
    })

    // 2. Agenda / seleção — erro sem parcela + axe
    await assertNoAxeViolations(page, `agenda (${viewport.name})`)
    await page.getByRole('button', { name: 'Continuar' }).click()
    await expect(page.getByRole('alert')).toContainText('Selecione ao menos uma parcela')
    await assertNoAxeViolations(page, `erro seleção (${viewport.name})`)

    const firstCheckbox = page.getByRole('checkbox').first()
    await expect(firstCheckbox).toBeVisible({ timeout: 30_000 })
    await firstCheckbox.focus()
    await expect(firstCheckbox).toBeFocused()
    await firstCheckbox.check()
    await page.getByRole('button', { name: 'Continuar' }).click()
    await expect(page.getByRole('heading', { name: '3. Destino' })).toBeVisible()

    // 3. Conta de recebimento — keyboard + axe
    await assertNoAxeViolations(page, `destino (${viewport.name})`)
    const creditor = page.getByLabel('Credor cessionário')
    await creditor.focus()
    await expect(creditor).toBeFocused()
    await creditor.fill('creditor-a11y')
    await page.keyboard.press('Tab')
    await expect(page.getByLabel('Banco')).toBeFocused()
    await page.keyboard.type('001')
    await page.keyboard.press('Tab')
    await expect(page.getByLabel('Agência')).toBeFocused()
    await page.keyboard.type('1234')
    await page.keyboard.press('Tab')
    await expect(page.getByLabel('Conta')).toBeFocused()
    await page.keyboard.type('88888-0')
    await page.getByRole('button', { name: 'Revisar' }).click()
    await expect(page.getByRole('heading', { name: '4. Revisão e envio' })).toBeVisible()

    // 4. Revisão
    await assertNoAxeViolations(page, `revisão (${viewport.name})`)
    await expect(page.getByText(/\*{4}88-0/)).toBeVisible()
    await page.getByRole('button', { name: 'Enviar portabilidade' }).click()

    // 5. Status + auditoria (eventos)
    await expect(page.getByRole('heading', { name: '5. Linha do tempo da operação' })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('[data-status="CONFIRMADA"]')).toHaveText('Confirmada', {
      timeout: 90_000,
    })
    await expect(page.getByRole('heading', { name: 'Eventos' })).toBeVisible()
    await expect(page.locator('.timeline li').first()).toBeVisible()
    await assertNoAxeViolations(page, `status+auditoria (${viewport.name})`)
  })
}
