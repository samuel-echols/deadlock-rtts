# AWS Setup Guide

This guide uses the **AWS Console** (web UI) to provision the infrastructure for Deadlock Meta History.

**Architecture:** EC2 (app) + RDS (Postgres) + ECR (Docker images) + S3 (backups). No load balancer or domain required — the EC2 public IP is used directly over port 80.

**Order matters:** launch EC2 first, then RDS — so you can use the "Connect to EC2" wizard that wires the security groups automatically.

---

## 1. Create an ECR repository

1. Search **ECR** in the top bar → **Elastic Container Registry**
2. Click **Create repository**
3. Name: `deadlock-meta-history`
4. Leave everything else default → **Create repository**

Copy the **URI** shown in the repository list — e.g. `123456789012.dkr.ecr.us-east-1.amazonaws.com/deadlock-meta-history`. You'll need it later as `ECR_IMAGE`.

---

## 2. Launch an EC2 instance

### 2a. Create a key pair

1. Go to **EC2** → **Key Pairs** (left sidebar, under Network & Security)
2. Click **Create key pair**
3. Name: `deadlock-ec2-key`, format: `.pem`
4. Click **Create key pair** — the `.pem` file downloads automatically
5. Move it somewhere safe: `mv ~/Downloads/deadlock-ec2-key.pem ~/.ssh/` then `chmod 400 ~/.ssh/deadlock-ec2-key.pem`

You'll need the contents of this file for the `EC2_SSH_KEY` GitHub secret later.

### 2b. Launch the instance

1. Go to **EC2** → **Instances** → **Launch instances**
2. **Name:** `deadlock-meta-history`
3. **AMI:** Search for `Amazon Linux 2023` → select the first result (Amazon Linux 2023 AMI, 64-bit x86)
4. **Instance type:** `t3.small`
5. **Key pair:** select `deadlock-ec2-key`
6. **Network settings** → **Edit**:
   - VPC: default
   - Auto-assign public IP: **Enable**
   - **Create security group**, name it `deadlock-ec2-sg`
   - Add rule: **HTTP**, port 80, source `0.0.0.0/0`
   - Add rule: **SSH**, port 22, source **My IP** (console fills this in automatically)
7. Leave storage as default (8 GB gp3 is fine)
8. Click **Launch instance**

Once running, click the instance → copy the **Public IPv4 address**. This is your `EC2_HOST`.

---

## 3. Create an RDS PostgreSQL instance

### 3a. Create a DB subnet group

1. Go to **RDS** → **Subnet groups** (left sidebar) → **Create DB subnet group**
2. Name: `deadlock-subnet-group`
3. VPC: select your **default VPC**
4. Availability Zones: select **all** listed AZs
5. Subnets: select **all** listed subnets
6. Click **Create**

### 3b. Create the RDS instance

1. Go to **RDS** → **Databases** → **Create database**
2. **Standard create**
3. Engine: **PostgreSQL**, version: **16** (latest 16.x)
4. Template: **Free tier** (uses db.t3.micro) or **Dev/Test** for db.t4g.micro
5. **DB instance identifier:** `deadlock-meta-history`
6. **Master username:** `deadlock`
7. **Master password:** choose a strong password and save it — this is your `DB_PASSWORD`
8. **DB instance class:** `db.t4g.micro` (Burstable, under Free tier change to db.t3.micro if preferred)
9. **Storage:** 20 GB, gp3
10. **Connectivity:**
    - Select **Connect to an EC2 compute resource**
    - Choose the `deadlock-meta-history` EC2 instance you just created
    - This automatically creates and wires the security groups — no manual SG work needed
    - VPC: default
    - DB subnet group: `deadlock-subnet-group`
    - Public access: **No**
11. **Database authentication:** Password authentication
12. **Additional configuration** → **Initial database name:** `deadlock`
13. Backup retention: 7 days (default)
14. Click **Create database**

This takes ~5 minutes. Once the status shows **Available**, click the database → copy the **Endpoint** under Connectivity & security. This is your `DB_HOST`.

---

## 4. Bootstrap the EC2 instance

SSH into the instance:

```bash
ssh -i ~/.ssh/deadlock-ec2-key.pem ec2-user@YOUR_EC2_IP
```

Install Docker and Docker Compose:

```bash
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
# Log out and back in for the group change to take effect
exit
ssh -i ~/.ssh/deadlock-ec2-key.pem ec2-user@YOUR_EC2_IP

# Docker Compose plugin
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Verify
docker --version && docker compose version
```

Create the app directory, then from your **local machine** copy the prod files:

```bash
# Run these on your local machine
scp -i ~/.ssh/deadlock-ec2-key.pem docker-compose.prod.yml \
  ec2-user@YOUR_EC2_IP:/home/ec2-user/app/
scp -i ~/.ssh/deadlock-ec2-key.pem scripts/backup.sh scripts/deploy.sh \
  ec2-user@YOUR_EC2_IP:/home/ec2-user/app/scripts/
ssh ec2-user@YOUR_EC2_IP "mkdir -p ~/app/scripts && chmod +x ~/app/scripts/*.sh"
```

---

## 5. Attach an IAM role to EC2 for ECR access

The EC2 instance needs permission to pull Docker images from ECR.

1. Go to **IAM** → **Roles** → **Create role**
2. Trusted entity: **AWS service** → **EC2** → Next
3. Search and attach the policy: `AmazonEC2ContainerRegistryReadOnly` → Next
4. Role name: `deadlock-ec2-role` → **Create role**

Now attach it to your EC2 instance:

1. Go to **EC2** → **Instances** → select `deadlock-meta-history`
2. **Actions** → **Security** → **Modify IAM role**
3. Select `deadlock-ec2-role` → **Update IAM role**

---

## 6. Create an IAM user for GitHub Actions

GitHub Actions needs credentials to push Docker images to ECR.

1. Go to **IAM** → **Users** → **Create user**
2. Username: `deadlock-ci` → Next
3. **Attach policies directly** → search and select `AmazonEC2ContainerRegistryPowerUser` → Next → **Create user**
4. Click the `deadlock-ci` user → **Security credentials** tab
5. Scroll to **Access keys** → **Create access key**
6. Use case: **Application running outside AWS** → Next → **Create access key**
7. **Copy both the Access Key ID and Secret Access Key now** — the secret is only shown once

---

## 7. Set up GitHub Actions secrets

Create a GitHub repository (if you haven't already), then go to **Settings → Secrets and variables → Actions → New repository secret** and add each of these:

| Secret | Value |
|---|---|
| `AWS_ACCESS_KEY_ID` | Access Key ID from step 6 |
| `AWS_SECRET_ACCESS_KEY` | Secret Access Key from step 6 |
| `EC2_HOST` | EC2 public IP from step 2b |
| `EC2_SSH_KEY` | Full contents of `deadlock-ec2-key.pem` (including `-----BEGIN...` and `-----END...` lines) |
| `DB_HOST` | RDS endpoint from step 3b |
| `DB_NAME` | `deadlock` |
| `DB_USER` | `deadlock` |
| `DB_PASSWORD` | The password you set in step 3b |

---

## 8. First deploy

Push to `main` and the GitHub Actions workflow will:
1. Run tests
2. Build the Docker image (including the React frontend)
3. Push it to ECR
4. SSH into EC2 and start the app with Docker Compose

The app will be available at `http://YOUR_EC2_IP`.

Check it's healthy:

```bash
curl http://YOUR_EC2_IP/actuator/health
```

### Manual first deploy (optional — before pushing to main)

```bash
# On your local machine
ECR_URI=123456789012.dkr.ecr.us-east-1.amazonaws.com/deadlock-meta-history

aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

docker build -t $ECR_URI:latest .
docker push $ECR_URI:latest

# SSH into EC2 and start the app
ssh -i ~/.ssh/deadlock-ec2-key.pem ec2-user@YOUR_EC2_IP
cd ~/app
export ECR_IMAGE=123456789012.dkr.ecr.us-east-1.amazonaws.com/deadlock-meta-history:latest
export DB_HOST=your-rds-endpoint.rds.amazonaws.com
export DB_NAME=deadlock
export DB_USER=deadlock
export DB_PASSWORD=YOUR_STRONG_PASSWORD
docker compose -f docker-compose.prod.yml up -d
```

---

## 9. Set up automated backups (optional)

### Create an S3 bucket

1. Go to **S3** → **Create bucket**
2. Name: `deadlock-meta-history-backups` (must be globally unique — add your account ID suffix if needed)
3. Region: `us-east-1`
4. Block all public access: **on** (default)
5. Click **Create bucket**

### Grant EC2 access to S3

1. Go to **IAM** → **Roles** → `deadlock-ec2-role` → **Add permissions** → **Create inline policy**
2. Switch to the **JSON** editor and paste:
```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["s3:PutObject", "s3:ListBucket", "s3:DeleteObject"],
    "Resource": [
      "arn:aws:s3:::deadlock-meta-history-backups",
      "arn:aws:s3:::deadlock-meta-history-backups/*"
    ]
  }]
}
```
3. Name the policy `S3BackupWrite` → **Create policy**

### Schedule the backup on EC2

SSH into EC2 and install `pg_dump`, then set up the cron job:

```bash
sudo dnf install -y postgresql16

crontab -e
```

Add this line (replace the placeholder values):
```
0 3 * * * DB_HOST=your-rds-endpoint.rds.amazonaws.com DB_NAME=deadlock DB_USER=deadlock PGPASSWORD=YOUR_PASSWORD S3_BUCKET=deadlock-meta-history-backups /home/ec2-user/app/scripts/backup.sh >> /var/log/deadlock-backup.log 2>&1
```

---

## Cost estimate (us-east-1, as of 2026)

| Resource | Type | Approx monthly cost |
|---|---|---|
| EC2 | t3.small (on-demand) | ~$15 |
| RDS | db.t4g.micro, 20 GB gp3 | ~$15 |
| ECR | <1 GB storage | ~$0.10 |
| S3 | <1 GB backups | ~$0.02 |
| Data transfer | Minimal for a personal project | ~$1 |
| **Total** | | **~$31/month** |

To reduce cost: use `db.t3.micro` + `t3.micro` (~$16/mo total) or stop instances when not in use.
