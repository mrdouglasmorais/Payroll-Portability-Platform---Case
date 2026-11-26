import { expect, test } from '@playwright/test'

/**
 * Operator UI against a live Compose stack.
 * Prefers seeded Central agenda for ben-001 / conv-1.
 */
test('operator can authorize, select installment, submit and reach terminal status', async ({
  page,
}) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Operational console' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Portability flow' })).toBeVisible()

  await page.getByLabel('Beneficiary id').fill('ben-ui')
  await page.getByLabel('Agreement id').fill('conv-1')
  await page.getByRole('button', { name: 'Authorize and load agenda' }).click()

  await expect(page.getByText(/Authorization/)).toBeVisible({ timeout: 30_000 })
  await expect(page.getByText('parcela-ui-1')).toBeVisible({ timeout: 30_000 })
  const firstCheckbox = page.getByRole('checkbox').first()
  await expect(firstCheckbox).toBeVisible()
  await firstCheckbox.check()
  await page.getByRole('button', { name: 'Continue' }).click()

  await page.getByLabel('Assignee creditor').fill('creditor-playwright')
  await page.getByLabel('Bank').fill('001')
  await page.getByLabel('Agency').fill('1234')
  await page.getByLabel('Account').fill('77777-0')
  await page.getByRole('button', { name: 'Review' }).click()

  await expect(page.getByText(/\*{4}77-0/)).toBeVisible()
  await page.getByRole('button', { name: 'Submit portability' }).click()

  await expect(page.getByText(/Operation/)).toBeVisible({ timeout: 30_000 })
  await expect(page.getByText('CONFIRMADA')).toBeVisible({ timeout: 90_000 })
  await expect(page.getByText(/polling local API/)).toHaveCount(0)
})

