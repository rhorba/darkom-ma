import { test, expect, APIRequestContext } from '@playwright/test';

const API_BASE_URL = process.env.API_BASE_URL ?? 'http://localhost:8081';
const ADMIN_EMAIL = process.env.ADMIN_EMAIL ?? '';
const ADMIN_PASSWORD = 'supersecretpw';
const PASSWORD = 'supersecretpw';
const STAMP = Date.now();

async function registerUser(
  request: APIRequestContext,
  email: string,
  fullName: string,
  role: string
): Promise<void> {
  await request.post(`${API_BASE_URL}/api/v1/auth/register`, {
    data: { email, password: PASSWORD, fullName, phone: '0600000000', role }
  });
}

test.describe.configure({ mode: 'serial' });

test('darkom.ma critical user journeys: landlord, tenant, admin', async ({ page, request }) => {
  page.on('response', (response) => {
    if (response.url().includes('/api/') && response.status() >= 400) {
      console.log(`[API ${response.status()}] ${response.request().method()} ${response.url()}`);
    }
  });
  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      console.log(`[console.error] ${msg.text()}`);
    }
  });

  const landlordEmail = `landlord-${STAMP}@e2e.test`;
  const tenantEmail = `tenant-${STAMP}@e2e.test`;

  await registerUser(request, landlordEmail, 'Rachid Landlord', 'LANDLORD');
  await registerUser(request, tenantEmail, 'Sara Tenant', 'TENANT');

  // ---- Landlord: create property, unit, lease ----
  await page.goto('/login');
  await page.getByLabel('Email').fill(landlordEmail);
  await page.getByLabel('Mot de passe').fill(PASSWORD);
  await page.getByRole('button', { name: 'Se connecter' }).click();
  await expect(page).toHaveURL(/properties/);

  await page.getByRole('button', { name: 'Nouvelle propriété' }).click();
  await page.getByLabel('Nom').fill('Villa Zaytouna');
  await page.getByLabel('Adresse').fill('12 Rue des Oliviers');
  await page.getByLabel('Ville').fill('Rabat');
  await page.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(page.getByText('Villa Zaytouna')).toBeVisible();

  await page.getByText('Villa Zaytouna').click();
  await expect(page).toHaveURL(/properties\/.+/);

  await page.getByRole('button', { name: 'Nouveau lot' }).click();
  await page.getByLabel('Libellé').fill('Apt 1');
  await page.getByLabel('Loyer mensuel (MAD)').fill('3500');
  await page.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(page.getByText('Apt 1')).toBeVisible();

  await page.getByRole('button', { name: 'Créer un bail' }).click();
  await page.getByLabel('Email du locataire').fill(tenantEmail);
  await page.getByLabel('Date de début').fill('2026-01-01');
  await page.getByLabel('Date de fin').fill('2026-12-31');
  await page.getByLabel('Loyer mensuel (MAD)').fill('3500');
  await page.getByRole('button', { name: 'Créer le bail' }).click();
  await expect(page.getByText('Occupé')).toBeVisible({ timeout: 10_000 });

  await page.getByRole('button', { name: 'Déconnexion' }).click();
  await expect(page).toHaveURL(/login/);

  // ---- Tenant: view lease, pay rent via mock CMI ----
  await page.getByLabel('Email').fill(tenantEmail);
  await page.getByLabel('Mot de passe').fill(PASSWORD);
  await page.getByRole('button', { name: 'Se connecter' }).click();
  await expect(page).toHaveURL(/my-lease/, { timeout: 10_000 });
  await expect(page.getByText('Apt 1')).toBeVisible();
  await expect(page.getByText('Villa Zaytouna')).toBeVisible();

  await page.getByRole('button', { name: 'Payer maintenant' }).click();
  await expect(page.getByText('Simulateur de paiement CMI')).toBeVisible({ timeout: 10_000 });
  await page.getByRole('button', { name: 'Simuler un paiement reussi' }).click();
  await expect(page).toHaveURL(/my-lease\?payment=success/);
  await expect(page.getByText('PAID')).toBeVisible();

  // ---- Tenant: submit a maintenance request ----
  await page.goto('/my-maintenance');
  await page.getByLabel('Description').fill('Le robinet de la cuisine fuit.');
  await page.getByRole('button', { name: 'Envoyer la demande' }).click();
  await expect(page.getByText('Le robinet de la cuisine fuit.')).toBeVisible();
  await expect(page.getByText('OPEN')).toBeVisible();

  await page.getByRole('button', { name: 'Déconnexion' }).click();
  await expect(page).toHaveURL(/login/, { timeout: 10_000 });

  // ---- Landlord: update the maintenance request status ----
  await page.getByLabel('Email').fill(landlordEmail);
  await page.getByLabel('Mot de passe').fill(PASSWORD);
  await page.getByRole('button', { name: 'Se connecter' }).click();
  await expect(page).toHaveURL(/properties/, { timeout: 10_000 });

  await page.goto('/maintenance');
  await expect(page.getByText('Le robinet de la cuisine fuit.')).toBeVisible();
  await page.locator('mat-select').first().click();
  await page.getByRole('option', { name: 'IN_PROGRESS' }).click();
  await expect(page.getByText('Statut mis à jour')).toBeVisible();

  await page.getByRole('button', { name: 'Déconnexion' }).click();
  await expect(page).toHaveURL(/login/, { timeout: 10_000 });

  // ---- Admin: list users, deactivate one ----
  if (ADMIN_EMAIL) {
    await page.getByLabel('Email').fill(ADMIN_EMAIL);
    await page.getByLabel('Mot de passe').fill(ADMIN_PASSWORD);
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page).toHaveURL(/admin\/users/, { timeout: 10_000 });

    await page.goto('/admin/users');
    const tenantRow = page.locator('tr', { hasText: tenantEmail });
    await expect(tenantRow).toBeVisible();
    await tenantRow.getByRole('button', { name: 'Désactiver' }).click();
    await expect(page.getByText('Compte désactivé')).toBeVisible();
  }
});
