import AxeBuilder from '@axe-core/playwright'
import { expect, type Page, test } from '@playwright/test'

/**
 * UI do operador contra o stack Compose ativo.
 * Preferir agenda seedada da Central para ben-ui / conv-1 on a clean volume.
 *
 * Accessibility: axe (wcag2a/wcag2aa) + keyboard smoke across wizard steps.
 * This is a regression harness — not a claim of full WCAG 2.2 AA certification.
 *
 * Consent step defaults to demo IDs (ben-ui / conv-1); advanced ID inputs are
 * behind "Alterar dados". Prefer exclusive parcela-ui-* / lab-seed OPEN rows.
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

async function openAdvancedIds(page: Page) {
  const toggle = page.getByRole('button', { name: 'Alterar dados' })
  if (await toggle.isVisible()) {
    await toggle.click()
  }
  await expect(page.getByLabel('ID do beneficiário')).toBeVisible()
}

async function authorizeToAgenda(page: Page) {
  // Demo panel already holds ben-ui / conv-1 — authorize without exposing fields.
  await page.getByRole('button', { name: 'Autorizar e consultar agenda' }).click()
  await expect(page.getByRole('heading', { name: 'Parcelas elegíveis' })).toBeVisible({
    timeout: 30_000,
  })
}

async function selectOpenParcelAndContinue(page: Page) {
  // Prefer lab-seed extras: canonical parcela-ui-1..4 may still appear OPEN on
  // Central after platform CONSUMED (dual-DB lab drift). Fall back to any enabled box.
  const extra = page.getByRole('checkbox', { name: /Selecionar parcela-ui-extra-/ })
  const checkbox =
    (await extra.count()) > 0
      ? extra.first()
      : page.getByRole('checkbox', { disabled: false }).first()
  await expect(checkbox).toBeVisible({ timeout: 30_000 })
  await checkbox.check()
  await page.getByRole('button', { name: 'Continuar' }).click()
  await expect(page.getByRole('heading', { name: 'Conta de recebimento' })).toBeVisible()
}

async function fillDestinationAndReview(page: Page, account = '77777-0') {
  await page.getByLabel('Credor cessionário').fill('creditor-playwright')
  await page.getByLabel('Banco').fill('001')
  await page.getByLabel('Agência').fill('1234')
  await page.getByLabel('Conta').fill(account)
  await page.getByRole('button', { name: 'Revisar' }).click()
  await expect(page.getByRole('heading', { name: 'Revisão e envio' })).toBeVisible()
  // Masked account appears in review + summary sidebar — assert first match.
  await expect(page.getByText(new RegExp(`\\*{4}${account.slice(-4)}`)).first()).toBeVisible()
}

async function submitAndWaitTerminal(page: Page) {
  await page.getByRole('button', { name: 'Enviar portabilidade' }).click()
  await expect(page.getByRole('heading', { name: 'Resultado e auditoria' })).toBeVisible({
    timeout: 30_000,
  })
  await expect(page.locator('[data-status]')).toBeVisible({ timeout: 30_000 })
  await expect(page.locator('[data-status="CONFIRMADA"]')).toHaveText('Confirmada', {
    timeout: 90_000,
  })
  await expect(page.getByText(/consultando API/)).toHaveCount(0)
  await expect(page.getByRole('heading', { name: 'Eventos' })).toBeVisible()
}

test('operador autoriza, seleciona parcela, envia e chega a status terminal', async ({
  page,
}) => {
  await page.setViewportSize({ width: 1280, height: 800 })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Nova operação' })).toBeVisible()
  await expect(page.getByRole('region', { name: 'Fluxo de portabilidade' })).toBeVisible()

  await authorizeToAgenda(page)
  await selectOpenParcelAndContinue(page)
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

    // 1. Autorização — open advanced IDs for keyboard focus order + fill + axe
    await openAdvancedIds(page)
    const beneficiary = page.getByLabel('ID do beneficiário')
    await beneficiary.focus()
    await expect(beneficiary).toBeFocused()
    await beneficiary.fill('ben-ui')
    await page.keyboard.press('Tab')
    const agreement = page.getByLabel('ID do convênio')
    await expect(agreement).toBeFocused()
    await agreement.fill('conv-1')
    await page.keyboard.press('Tab')
    await expect(page.getByRole('button', { name: 'Autorizar e consultar agenda' })).toBeFocused()
    await assertNoAxeViolations(page, `autorização (${viewport.name})`)

    await page.getByRole('button', { name: 'Autorizar e consultar agenda' }).click()
    await expect(page.getByRole('heading', { name: 'Parcelas elegíveis' })).toBeVisible({
      timeout: 30_000,
    })

    // 2. Agenda / seleção — erro sem parcela + axe
    await assertNoAxeViolations(page, `agenda (${viewport.name})`)
    await page.getByRole('button', { name: 'Continuar' }).click()
    await expect(page.getByRole('alert')).toContainText('Selecione ao menos uma parcela')
    await assertNoAxeViolations(page, `erro seleção (${viewport.name})`)

    const extra = page.getByRole('checkbox', { name: /Selecionar parcela-ui-extra-/ })
    const checkbox =
      (await extra.count()) > 0
        ? extra.first()
        : page.getByRole('checkbox', { disabled: false }).first()
    await expect(checkbox).toBeVisible({ timeout: 30_000 })
    await checkbox.focus()
    await expect(checkbox).toBeFocused()
    await checkbox.check()
    await page.getByRole('button', { name: 'Continuar' }).click()
    await expect(page.getByRole('heading', { name: 'Conta de recebimento' })).toBeVisible()

    // 3. Conta de recebimento — keyboard focus + fill + axe
    await assertNoAxeViolations(page, `destino (${viewport.name})`)
    const creditor = page.getByLabel('Credor cessionário')
    await creditor.focus()
    await expect(creditor).toBeFocused()
    await creditor.fill('creditor-a11y')
    await page.keyboard.press('Tab')
    const bank = page.getByLabel('Banco')
    await expect(bank).toBeFocused()
    await bank.fill('001')
    await page.keyboard.press('Tab')
    const agency = page.getByLabel('Agência')
    await expect(agency).toBeFocused()
    await agency.fill('1234')
    await page.keyboard.press('Tab')
    const account = page.getByLabel('Conta')
    await expect(account).toBeFocused()
    await account.fill('88888-0')
    await page.getByRole('button', { name: 'Revisar' }).click()
    await expect(page.getByRole('heading', { name: 'Revisão e envio' })).toBeVisible()

    // 4. Revisão
    await assertNoAxeViolations(page, `revisão (${viewport.name})`)
    await expect(page.getByText(/\*{4}88-0/).first()).toBeVisible()
    await page.getByRole('button', { name: 'Enviar portabilidade' }).click()

    // 5. Status + auditoria (eventos)
    await expect(page.getByRole('heading', { name: 'Resultado e auditoria' })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.locator('[data-status="CONFIRMADA"]')).toHaveText('Confirmada', {
      timeout: 90_000,
    })
    await expect(page.getByRole('heading', { name: 'Eventos' })).toBeVisible()
    await expect(page.locator('.timeline li').first()).toBeVisible({ timeout: 30_000 })
    await assertNoAxeViolations(page, `status+auditoria (${viewport.name})`)
  })
}
