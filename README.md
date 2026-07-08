# DevOps Pipeline Demo — Java + Jenkins + Docker + AWS EC2

A minimal, free-tier-friendly CI/CD demo: a Spring Boot "Hello World" app that
Jenkins builds, tests, containerizes, and deploys to an AWS EC2 instance.

## Architecture

```
GitHub repo
     │  webhook / poll
     ▼
Jenkins (running on EC2 #1, t2.micro/t3.micro)
     │
     ├─ Checkout
     ├─ Build (mvn compile)
     ├─ Test (mvn test + JUnit report)
     ├─ Package (mvn package -> jar)
     ├─ Docker Build (multi-stage Dockerfile)
     ├─ Docker Push (Docker Hub)
     ├─ Deploy (SSH -> docker run on target host)
     └─ Smoke Test (curl the deployed endpoint)
```

To stay safely inside the AWS free tier, Jenkins and the deployed app both run
on **the same** t2.micro/t3.micro instance (Jenkins on port 8080, the app on
port 8081). Running two separate instances would split your 750 free hours/month
across both, so one box is the simplest, cheapest way to demo this.

> Free tier notes (AWS, 2026): 750 hrs/month covers one instance running 24/7.
> Accounts created before July 15, 2025 get 12 months of t2.micro/t3.micro.
> Newer accounts get a 6-month credit-based Free Plan with a broader instance
> list — check the EC2 console's "Free tier eligible" label before you launch.

## 1. Launch the EC2 instance

1. AWS Console -> EC2 -> Launch Instance
2. AMI: Amazon Linux 2023
3. Instance type: `t2.micro` (or `t3.micro` if t2.micro isn't offered in your region)
4. Key pair: create/download one (you'll need it for SSH + Jenkins deploy)
5. Security group inbound rules:
   - SSH (22) — your IP only
   - Custom TCP 8080 — your IP (Jenkins UI)
   - Custom TCP 8081 — 0.0.0.0/0 (the demo app, so you can hit it from anywhere)
6. Storage: default 8-30GB gp3 (free tier covers up to 30GB)
7. Launch, then SSH in:
   ```
   ssh -i your-key.pem ec2-user@<EC2_PUBLIC_IP>
   ```

## 2. Install Docker + Jenkins on the instance

```bash
# Docker
sudo yum update -y
sudo yum install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

# Jenkins (needs Java 17)
sudo yum install -y java-17-amazon-corretto
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.repo.key
sudo yum install -y jenkins
sudo usermod -aG docker jenkins
sudo systemctl enable --now jenkins

# Maven (Jenkins agent needs it if not using a container-based agent)
sudo yum install -y maven
```

Reboot or re-login so the `docker` group membership takes effect:
```bash
sudo reboot
```

## 3. Unlock Jenkins

1. Visit `http://<EC2_PUBLIC_IP>:8080`
2. Get the initial admin password:
   ```bash
   sudo cat /var/lib/jenkins/secrets/initialAdminPassword
   ```
3. Install suggested plugins, then also install:
   - **Docker Pipeline**
   - **SSH Agent**
   - **Pipeline: Stage View** (nice for demo visuals)

## 4. Add Jenkins credentials

Manage Jenkins -> Credentials -> add:

| ID | Type | Purpose |
|---|---|---|
| `dockerhub-creds` | Username/Password | Docker Hub login for pushing images |
| `ec2-ssh-key` | SSH Username with private key | Same key pair, so Jenkins can SSH to deploy |

Since Jenkins and the deploy target are the same box in this demo, `ec2-ssh-key`
can point back to `localhost` — or, if you'd rather demo a *separate* deploy
target, launch a second free-tier instance and point `DEPLOY_HOST` at it.

## 5. Create the pipeline job

1. New Item -> Pipeline -> name it `devops-demo`
2. Pipeline -> Definition: "Pipeline script from SCM"
3. SCM: Git, point at your repo containing this project (with the `Jenkinsfile`)
4. Before running, edit the `Jenkinsfile` environment block:
   - `DOCKERHUB_USER` -> your Docker Hub username
   - `DEPLOY_HOST` -> `ec2-user@<EC2_PUBLIC_IP>`
   - the `Smoke Test` stage's URL -> same `<EC2_PUBLIC_IP>`
5. Save, then **Build Now**

You'll see all 7 stages light up green in the Stage View: Checkout -> Build ->
Test -> Package -> Docker Build -> Docker Push -> Deploy -> Smoke Test.

## 6. Verify

```bash
curl http://<EC2_PUBLIC_IP>:8081/
# Hello from the Jenkins -> Docker -> AWS EC2 pipeline demo!

curl http://<EC2_PUBLIC_IP>:8081/version
# v1.0.0
```

Push a change (e.g. edit the string in `HelloController.java`, bump the
version), commit, and re-run the pipeline (or wire up a GitHub webhook) to
show the full loop live.

## Optional stretch goal: swap EC2 deploy for real ECS

If you want to show ECS specifically instead of a plain `docker run`:
1. Push images to **ECR** instead of Docker Hub (`aws ecr` login + push).
2. Create an ECS cluster with an EC2 capacity provider using a single
   t2.micro/t3.micro instance (ECS-optimized AMI) — the ECS control plane
   itself is free; you're still just paying/free-tiering the EC2 instance
   underneath it.
3. Replace the `Deploy to EC2` stage with:
   ```
   aws ecs update-service --cluster demo-cluster --service devops-demo-svc --force-new-deployment
   ```
This adds real moving parts (task definitions, ECR repo, ECS service) — good
for a more "enterprise-y" demo, but the plain EC2 + docker run version above is
faster to set up and easier to narrate live.

## Cleanup (avoid surprise charges)

- Terminate the EC2 instance when you're done demoing.
- Release any Elastic IP you allocated (they're billed even when idle).
- Delete unused EBS volumes/snapshots.
