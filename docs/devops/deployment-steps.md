# Deployment Steps

This project currently supports manual DEV and PROD deployments. No cloud provider, Kubernetes, Jenkins, monitoring, or automated deployment has been added.

## DEV Deployment

1. Copy the repository to the DEV server.
2. Create `.env.dev` from `.env.example`.
3. Set `SPRING_PROFILES_ACTIVE=dev`.
4. Set `DB_NAME=fleetmatch_dev`.
5. Set secure values for `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `REDIS_PASSWORD`.
6. Start the stack:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d --build
```

7. Configure Nginx with `deploy/nginx/dev.conf`.
8. Point `api-dev.easyfleetmatch.com` DNS to the DEV server.
9. Confirm health:

```text
http://api-dev.easyfleetmatch.com/api/health
```

## PROD Deployment

1. Copy the repository to the PROD server.
2. Create `.env.prod` from `.env.example`.
3. Set `SPRING_PROFILES_ACTIVE=prod`.
4. Set `DB_NAME=fleetmatch_prod`.
5. Set strong production values for all secrets.
6. Start the stack:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

7. Configure Nginx with `deploy/nginx/prod.conf`.
8. Point `api.easyfleetmatch.com` DNS to the PROD server.
9. Confirm health:

```text
http://api.easyfleetmatch.com/api/health
```

## Deployment Checklist

- Environment file exists on the target server.
- DEV and PROD use separate PostgreSQL databases.
- JWT secret is unique per environment.
- Redis password is set outside LOCAL.
- Nginx config matches the target domain.
- DNS points to the correct server.
- TLS certificates are installed before public production use.
- `mvn clean verify` passes in CI before deployment.
