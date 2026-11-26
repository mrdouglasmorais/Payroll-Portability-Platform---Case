import { expect, test } from '@playwright/test'

/**
 * UI do operador contra o stack Compose ativo.
 * Preferir agenda seedada da Central para ben-ui / conv-1.
 */
test('operador autoriza, seleciona parcela, envia e chega a status terminal', async ({
  page,
}) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Console operacional' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Fluxo de portabilidade' })).toBeVisible()

  await page.getByLabel('ID do beneficiário').fill('ben-ui')
  await page.getByLabel('ID do convênio').fill('conv-1')
  await page.getByRole('button', { name: 'Autorizar e carregar agenda' }).click()

  await expect(page.getByText(/Autorização/)).toBeVisible({ timeout: 30_000 })
  await expect(page.getByText('parcela-ui-1')).toBeVisible({ timeout: 30_000 })
  const firstCheckbox = page.getByRole('checkbox').first()
  await expect(firstCheckbox).toBeVisible()
  await firstCheckbox.check()
  await page.getByRole('button', { name: 'Continuar' }).click()

  await page.getByLabel('Credor cessionário').fill('creditor-playwright')
  await page.getByLabel('Banco').fill('001')
  await page.getByLabel('Agência').fill('1234')
  await page.getByLabel('Conta').fill('77777-0')
  await page.getByRole('button', { name: 'Revisar' }).click()

  await expect(page.getByText(/\*{4}77-0/)).toBeVisible()
  await page.getByRole('button', { name: 'Enviar portabilidade' }).click()

  await expect(page.getByText(/Operação/)).toBeVisible({ timeout: 30_000 })
  await expect(page.getByText('Confirmada')).toBeVisible({ timeout: 90_000 })
  await expect(page.getByText(/consultando API local/)).toHaveCount(0)
})
