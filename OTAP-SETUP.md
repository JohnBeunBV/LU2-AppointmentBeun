# OTAP Omgeving Opzetten

## Overzicht

Dit project implementeert een **OTAP-omgeving** (Ontwikkeling, Test, Acceptatie, Productie) met:

- Eén `Dockerfile` met `BUILD_ENV` arg (vervangt drie losse bestanden)
- Docker Compose per omgeving (dev, test, acceptance, prod)
- GitHub Actions CI/CD workflows per omgeving
- GitHub Environments met protection rules en secrets per omgeving

---

## Docker structuur

```
.docker/Dockerfile               ← één Dockerfile voor alle omgevingen
docker-compose.dev.yml           → Ontwikkeling (port 8080, debug 5005)
docker-compose.test.yml          → Test          (port 8081)
docker-compose.acceptance.yml    → Acceptatie    (port 8082)
docker-compose.prod.yml          → Productie     (poort intern)
```

De `BUILD_ENV` build arg bepaalt het gedrag van de image:

| BUILD_ENV    | JVM heap       | Debug agent | Non-root user | Log level |
|--------------|----------------|-------------|---------------|-----------|
| development  | 512m / 256m    | Ja (5005)   | Nee           | DEBUG     |
| test         | 1024m / 512m   | Nee         | Nee           | DEBUG     |
| acceptance   | 1024m / 512m   | Nee         | Nee           | INFO      |
| production   | 2048m / 1024m  | Nee         | Ja (openmrs)  | ERROR     |

### Lokaal draaien

```bash
# Ontwikkeling (met debug port)
docker-compose -f docker-compose.dev.yml up -d

# Test
docker-compose -f docker-compose.test.yml --env-file .env.test up -d

# Acceptatie
docker-compose -f docker-compose.acceptance.yml --env-file .env.acceptance up -d

# Productie (alleen op server)
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### Lokaal bouwen per omgeving

```bash
docker build -f .docker/Dockerfile --build-arg BUILD_ENV=development -t openmrs:dev .
docker build -f .docker/Dockerfile --build-arg BUILD_ENV=test        -t openmrs:test .
docker build -f .docker/Dockerfile --build-arg BUILD_ENV=acceptance  -t openmrs:acc .
docker build -f .docker/Dockerfile --build-arg BUILD_ENV=production  -t openmrs:prod .
```

---

## Branch strategie

```
main          →  Productie   (manual deploy, 2 reviewers vereist)
  ↑ PR
release/*     →  Acceptatie  (auto deploy bij push, 1 reviewer vereist)
  ↑ PR
develop       →  Test        (auto deploy bij push)
  ↑ PR
feature/*     →  Alleen build & test, geen deploy
```

---

## GitHub: Environments aanmaken

Ga naar: **repo → Settings → Environments → New environment**

### 1. Environment: `test`

**Protection rules:**
- Deployment branches: `develop` only

**Secrets:**

| Secret | Waarde |
|--------|--------|
| `TEST_SERVER_HOST` | IP of hostname van de test server |
| `TEST_SERVER_USER` | SSH gebruiker (bijv. `deploy`) |
| `TEST_SSH_KEY` | Inhoud van de SSH private key |
| `SLACK_WEBHOOK` | Slack webhook URL |

---

### 2. Environment: `acceptance`

**Protection rules:**
- Required reviewers: minimaal 1 persoon
- Deployment branches: `release/*` only

**Secrets:**

| Secret | Waarde |
|--------|--------|
| `ACC_SERVER_HOST` | IP of hostname van de acceptatie server |
| `ACC_SERVER_USER` | SSH gebruiker (bijv. `deploy`) |
| `ACC_SSH_KEY` | Inhoud van de SSH private key |
| `SLACK_WEBHOOK` | Slack webhook URL |

---

### 3. Environment: `production`

**Protection rules:**
- Required reviewers: minimaal 2 personen
- Deployment branches: `main` only
- Wait timer: 5 minuten (optioneel, geeft tijd om te annuleren)

**Secrets:**

| Secret | Waarde |
|--------|--------|
| `PROD_SERVER_HOST` | IP of hostname van de productie server |
| `PROD_SERVER_USER` | SSH gebruiker (bijv. `deploy`) |
| `PROD_SSH_KEY` | Inhoud van de SSH private key |
| `DB_ROOT_PASSWORD` | MySQL root wachtwoord (sterk!) |
| `DB_NAME` | `openmrs_prod` |
| `DB_USER` | `openmrs` |
| `DB_PASSWORD` | MySQL wachtwoord voor openmrs user (sterk!) |
| `OPENMRS_ADMIN_USER` | `admin` |
| `OPENMRS_ADMIN_PASSWORD` | OpenMRS admin wachtwoord (sterk!) |
| `SLACK_WEBHOOK` | Slack webhook URL |

**Optioneel (SonarQube):**

| Secret | Waarde |
|--------|--------|
| `SONAR_HOST_URL` | URL van SonarQube instantie |
| `SONAR_LOGIN` | SonarQube token |

---

## GitHub: Branch protection rules

Ga naar: **Settings → Branches → Add branch protection rule**

### Branch: `main`

- [x] Require a pull request before merging
- [x] Require approvals: **2**
- [x] Dismiss stale pull request approvals when new commits are pushed
- [x] Require status checks to pass before merging
  - Status check: `build` (van `Build & Test` workflow)
- [x] Require branches to be up to date before merging
- [x] Restrict who can push to matching branches (alleen team leads / release managers)
- [x] Do not allow bypassing the above settings

### Branch: `develop`

- [x] Require a pull request before merging
- [x] Require approvals: **1**
- [x] Require status checks to pass before merging
  - Status check: `build`
- [x] Require branches to be up to date before merging

### Branch: `release/*`

- [x] Require a pull request before merging
- [x] Require approvals: **1**
- [x] Require status checks to pass before merging
  - Status check: `build`

---

## SSH keys aanmaken

Voer dit uit op je lokale machine (één keer per server):

```bash
# Test server
ssh-keygen -t ed25519 -C "github-deploy-test" -f github_deploy_test -N ""

# Acceptatie server
ssh-keygen -t ed25519 -C "github-deploy-acceptance" -f github_deploy_acceptance -N ""

# Productie server
ssh-keygen -t ed25519 -C "github-deploy-prod" -f github_deploy_prod -N ""
```

Kopieer de public key naar elke server:

```bash
ssh-copy-id -i github_deploy_test.pub deploy@<TEST_SERVER_HOST>
ssh-copy-id -i github_deploy_acceptance.pub deploy@<ACC_SERVER_HOST>
ssh-copy-id -i github_deploy_prod.pub deploy@<PROD_SERVER_HOST>
```

Voeg de **private** key toe aan het GitHub Environment als `TEST_SSH_KEY` / `ACC_SSH_KEY` / `PROD_SSH_KEY` (inhoud van het bestand zonder `.pub`).

---

## Server-side setup

Voer de volgende stappen uit op **elke deployment server** (test, acceptatie, productie).

### 1. Docker installeren (Ubuntu 22.04)

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
```

### 2. Deploy gebruiker aanmaken

```bash
sudo useradd -m -s /bin/bash deploy
sudo usermod -aG docker deploy
sudo mkdir -p /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
# Plak hier de public key van de deploy SSH key
sudo nano /home/deploy/.ssh/authorized_keys
sudo chmod 600 /home/deploy/.ssh/authorized_keys
sudo chown -R deploy:deploy /home/deploy/.ssh
```

### 3. Mapstructuur aanmaken

```bash
# Op de test server
sudo mkdir -p /opt/openmrs/test
sudo chown deploy:deploy /opt/openmrs/test

# Op de acceptatie server
sudo mkdir -p /opt/openmrs/acceptance
sudo chown deploy:deploy /opt/openmrs/acceptance

# Op de productie server
sudo mkdir -p /opt/openmrs/prod /backups
sudo chown deploy:deploy /opt/openmrs/prod /backups
```

### 4. Docker Compose bestanden op de server zetten

De workflows deployen via SSH. De server heeft de compose files nodig. Eerste keer: kopieer ze handmatig of via git clone.

```bash
# Op de test server (als deploy user)
cd /opt/openmrs/test
scp user@lokaal:<pad>/docker-compose.test.yml .

# Op de acceptatie server
cd /opt/openmrs/acceptance
scp user@lokaal:<pad>/docker-compose.acceptance.yml .

# Op de productie server
cd /opt/openmrs/prod
scp user@lokaal:<pad>/docker-compose.prod.yml .
```

### 5. .env bestanden aanmaken op de servers

Maak per server een `.env` bestand met de werkelijke waarden. **Dit bestand nooit committen.**

**Test server** — `/opt/openmrs/test/.env`

```env
DB_ROOT_PASSWORD=<sterk_wachtwoord>
DB_NAME=openmrs_test
DB_USER=openmrs
DB_PASSWORD=<sterk_wachtwoord>
OPENMRS_ADMIN_USER=admin
OPENMRS_ADMIN_PASSWORD=<sterk_wachtwoord>
```

**Acceptatie server** — `/opt/openmrs/acceptance/.env`

```env
DB_ROOT_PASSWORD=<sterk_wachtwoord>
DB_NAME=openmrs_acceptance
DB_USER=openmrs
DB_PASSWORD=<sterk_wachtwoord>
OPENMRS_ADMIN_USER=admin
OPENMRS_ADMIN_PASSWORD=<sterk_wachtwoord>
```

**Productie server** — `/opt/openmrs/prod/.env`

```env
DB_ROOT_PASSWORD=<sterk_wachtwoord>
DB_NAME=openmrs_prod
DB_USER=openmrs
DB_PASSWORD=<sterk_wachtwoord>
OPENMRS_ADMIN_USER=admin
OPENMRS_ADMIN_PASSWORD=<sterk_wachtwoord>
```

Stel de rechten in zodat alleen `deploy` het bestand kan lezen:

```bash
chmod 600 /opt/openmrs/<omgeving>/.env
```

### 6. Container registry login op de server

De workflows pushen images naar GHCR. De server moet die kunnen pullen. Genereer een GitHub Personal Access Token (PAT) met `read:packages` scope en log in:

```bash
# Op elke server, als deploy user
echo "<PAT>" | docker login ghcr.io -u <github_username> --password-stdin
```

Dit slaat de credentials op in `~/.docker/config.json` van de deploy user.

### 7. Eerste handmatige deploy (smoke test)

Controleer dat alles werkt vóór de eerste CI deploy:

```bash
# Op de test server
cd /opt/openmrs/test
docker compose -f docker-compose.test.yml --env-file .env pull
docker compose -f docker-compose.test.yml --env-file .env up -d
docker compose -f docker-compose.test.yml logs -f openmrs
```

---

## Workflows overzicht

| Workflow | Trigger | Environment | Approval |
|----------|---------|-------------|----------|
| `build-test.yml` | Push / PR op alle branches | — | Geen |
| `deploy-test.yml` | Push naar `develop` | `test` | Geen |
| `deploy-acceptance.yml` | Push naar `release/*` of handmatig | `acceptance` | 1 reviewer |
| `deploy-prod.yml` | Handmatig (`workflow_dispatch`) | `production` | 2 reviewers |

### Productie deployen

1. Ga naar **Actions → Deploy to Production**
2. Klik **Run workflow**
3. Voer het versienummer in: `v1.2.3`
4. Wacht op goedkeuring van 2 reviewers
5. Deploy start automatisch

---

## Checklist

### GitHub

- [ ] Environment `test` aangemaakt met branch filter `develop`
- [ ] Environment `acceptance` aangemaakt met 1 required reviewer en branch filter `release/*`
- [ ] Environment `production` aangemaakt met 2 required reviewers en branch filter `main`
- [ ] Alle secrets toegevoegd per environment (zie tabellen hierboven)
- [ ] Branch protection ingesteld op `main` (2 reviewers, status checks)
- [ ] Branch protection ingesteld op `develop` (1 reviewer, status checks)
- [ ] Branch protection ingesteld op `release/*` (1 reviewer, status checks)

### Servers

- [ ] Docker geïnstalleerd op test server
- [ ] Docker geïnstalleerd op acceptatie server
- [ ] Docker geïnstalleerd op productie server
- [ ] `deploy` gebruiker aangemaakt op alle servers
- [ ] SSH public keys toegevoegd aan `authorized_keys` op alle servers
- [ ] Mapstructuur aangemaakt (`/opt/openmrs/<omgeving>`)
- [ ] Docker Compose bestanden op servers gezet
- [ ] `.env` bestanden aangemaakt met correcte waarden
- [ ] Container registry login gedaan op alle servers
- [ ] Eerste handmatige deploy getest op test server

### Optioneel

- [ ] Slack webhook aangemaakt en toegevoegd als secret
- [ ] SonarQube secrets toegevoegd voor code quality
- [ ] Runbook gedocumenteerd voor noodgevallen en rollback

---

## Troubleshooting

**Deployment mislukt: "SSH connection refused"**

```bash
# Test de verbinding lokaal
ssh -i github_deploy_key deploy@<SERVER_HOST>
# Controleer of de public key in authorized_keys staat
cat ~/.ssh/authorized_keys
```

**Container kan image niet pullen (GHCR)**

```bash
# Op de server als deploy user
docker login ghcr.io
# Of controleer of de PAT nog geldig is
```

**Test omgeving vastgelopen**

```bash
docker compose -f docker-compose.test.yml down -v
docker compose -f docker-compose.test.yml up -d
```

**Productie rollback**

```bash
# Op de productie server
cd /opt/openmrs/prod
# Zet de image tag terug naar een vorige versie in .env of direct:
docker compose -f docker-compose.prod.yml down
docker pull ghcr.io/<repo>:v<vorige_versie>
# Pas de image tag aan in docker-compose.prod.yml en start opnieuw
docker compose -f docker-compose.prod.yml up -d
```

Of via GitHub: **Deployments → klik op vorige succesvolle deploy → Re-run**

---

**Aangemaakt:** 2026-06-02  
**Bijgewerkt:** 2026-06-04  
**Status:** KLAAR VOOR IMPLEMENTATIE
