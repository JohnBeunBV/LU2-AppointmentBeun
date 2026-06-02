# OTAP Omgeving Opzetten

## 📋 Overzicht

Dit project implementeert een **OTAP-omgeving** (Ontwikkeling, Test, Acceptatie, Productie) met:

- ✅ Docker Compose per omgeving (dev, test, prod)
- ✅ GitHub Actions CI/CD workflows
- ✅ GitHub Environments met protection rules
- ✅ Automated deployment

---

## 🐳 Docker Omgevingen

### Structuur

```
docker-compose.dev.yml   → Local development (port 8080)
docker-compose.test.yml  → Automated testing (port 8081)
docker-compose.prod.yml  → Production (internal only)
```

### 1. Development (Local)

```bash
# Setup
docker-compose -f docker-compose.dev.yml up -d

# Logs
docker-compose -f docker-compose.dev.yml logs -f openmrs

# Shutdown
docker-compose -f docker-compose.dev.yml down
```

**Properties:**

- Hot reload support
- Debug port 5005 exposed
- Full logging
- Direct database access

### 2. Test (Automated)

```bash
# Setup
docker-compose -f docker-compose.test.yml --env-file .env.test up -d

# Run tests
docker-compose -f docker-compose.test.yml exec openmrs mvn test

# Cleanup
docker-compose -f docker-compose.test.yml down -v
```

**Properties:**

- Compiled module
- Optimized JVM settings
- Test database isolation
- Build in CI only

### 3. Production (Secure)

```bash
# Setup (on production server)
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Check status
docker-compose -f docker-compose.prod.yml ps
```

**Properties:**

- No exposed database ports
- Multi-stage builds
- Non-root user
- Volume backups
- Minimal logs

---

## 🔐 GitHub Environments Setup

### Step 1: Create Environments

Go to: **Settings → Environments → New Environment**

Create 3 environments:

1. **development** (no protection)
2. **test** (optional reviewers)
3. **production** (required reviewers + branch protection)

### Step 2: Development Environment

**Name:** `development`  
**Branch protection:** None  
**Secrets:** (optional)

```
DEV_SERVER_HOST=dev.example.com
DEV_SERVER_USER=deploy
DEV_SSH_KEY=(private key)
SLACK_WEBHOOK=https://hooks.slack.com/...
```

### Step 3: Test Environment

**Name:** `test`  
**Branch protection:** Require main branch only

**Secrets:**

```
TEST_SERVER_HOST=test.example.com
TEST_SERVER_USER=deploy
TEST_SSH_KEY=(private key)
TEST_DB_PASSWORD=(test db password)
SLACK_WEBHOOK=...
```

### Step 4: Production Environment ⚠️

**Name:** `production`  
**Branch protection:**

- ✅ Require main branch only
- ✅ Require status checks to pass
- ✅ Require code reviews (min 2)
- ✅ Dismiss stale reviews
- ✅ Restrict who can push to main

**Secrets (CRITICAL):**

```
PROD_SERVER_HOST=prod.example.com
PROD_SERVER_USER=deploy
PROD_SSH_KEY=(private key)
DB_ROOT_PASSWORD=(SECURE! Use GitHub secret)
DB_NAME=openmrs_prod
DB_USER=openmrs
DB_PASSWORD=(SECURE!)
OPENMRS_ADMIN_USER=admin
OPENMRS_ADMIN_PASSWORD=(SECURE!)
SLACK_WEBHOOK=...
```

---

## 🚀 Deployment Workflows

### 1. Automatic: Build & Test (on all branches)

```
push/PR → build → unit tests → docker build
```

**File:** `.github/workflows/build-test.yml`

### 2. Automatic: Deploy to Test (develop branch)

```
push develop → build → test deployment → health checks
```

**File:** `.github/workflows/deploy-test.yml`

### 3. Manual: Deploy to Production (main branch)

```
workflow_dispatch (requires approval) → backup → deploy → health checks
```

**File:** `.github/workflows/deploy-prod.yml`

**How to deploy:**

1. Go to: **Actions → Deploy to Production**
2. Click: **Run workflow**
3. Enter version: `v1.2.3`
4. Wait for approval (requires 2 reviewers)
5. Deploy starts

---

## 📝 Required GitHub Secrets

### For All Environments

```
GITHUB_TOKEN       (automatic, no setup needed)
SLACK_WEBHOOK      (Slack notification URL)
```

### For Test

```
TEST_SERVER_HOST   (server hostname)
TEST_SERVER_USER   (SSH user)
TEST_SSH_KEY       (SSH private key for deploy)
```

### For Production ⚠️

```
PROD_SERVER_HOST   (server hostname)
PROD_SERVER_USER   (SSH user)
PROD_SSH_KEY       (SSH private key for deploy)
DB_ROOT_PASSWORD   (MySQL root password)
DB_PASSWORD        (OpenMRS database password)
OPENMRS_ADMIN_PASSWORD (OpenMRS admin password)
SONAR_HOST_URL     (optional, for code quality)
SONAR_LOGIN        (optional, for code quality)
```

---

## 🔑 SSH Key Setup

### Generate SSH keys (on local machine)

```bash
ssh-keygen -t ed25519 -C "github-deploy" -f github_deploy_key -N ""
```

### Add public key to servers

```bash
# On test server
echo "$(cat github_deploy_key.pub)" >> ~/.ssh/authorized_keys

# On production server
echo "$(cat github_deploy_key.pub)" >> ~/.ssh/authorized_keys
```

### Add private key to GitHub

1. Go to **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Name: `TEST_SSH_KEY` / `PROD_SSH_KEY`
4. Value: (paste contents of `github_deploy_key`)

---

## 🧪 Local Testing

### Test build locally

```bash
# Build dev image
docker build -f .docker/Dockerfile.dev -t openmrs:dev .

# Build test image
docker build -f .docker/Dockerfile.test -t openmrs:test .

# Run tests
docker-compose -f docker-compose.test.yml up -d
docker-compose -f docker-compose.test.yml exec openmrs mvn test
```

### Simulate production deploy

```bash
# Build prod image
docker build -f .docker/Dockerfile.prod -t openmrs:prod .

# Test with local compose
docker-compose -f docker-compose.prod.yml up
```

---

## 📊 Branch Strategy

```
main (production)
  ↑ PR (requires 2 reviewers)

develop (test/acceptatie)
  ↑ PR (requires 1 review)

feature/* (development)
  → push → auto build & test
```

---

## ✅ Checklist

- [ ] Create GitHub Environments (dev, test, prod)
- [ ] Add branch protection rules to `main`
- [ ] Configure required reviewers for prod
- [ ] Generate SSH keys for deploy
- [ ] Add SSH public keys to servers
- [ ] Add SSH private keys to GitHub secrets
- [ ] Add database passwords to GitHub secrets
- [ ] Test build workflow locally
- [ ] Test deploy workflow (test environment first)
- [ ] Set up Slack notifications
- [ ] Document server setup
- [ ] Create runbook for emergency rollback

---

## 🆘 Troubleshooting

### Deployment fails: "SSH connection refused"

- Check SSH key is added to server `~/.ssh/authorized_keys`
- Check server IP/hostname in GitHub secret
- Test locally: `ssh -i github_deploy_key user@host`

### Test environment stuck

- Manual cleanup: `docker-compose -f docker-compose.test.yml down -v`
- Check logs: `docker-compose -f docker-compose.test.yml logs openmrs`

### Production rollback

- Go to **Deployments**
- Click **Reactivate** on previous successful deployment
- OR manually run older commit to `main` branch

---

**Created:** 2026-06-02  
**Status:** READY FOR IMPLEMENTATION  
**Next Step:** Configure GitHub Environments & secrets
