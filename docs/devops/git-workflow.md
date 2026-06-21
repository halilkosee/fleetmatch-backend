# Git Workflow

Recommended branch model:

- `main`: production-ready code
- `develop`: shared integration branch
- `test`: QA/staging validation branch
- `feature/*`: feature work
- `fix/*`: bug fixes
- `hotfix/*`: urgent production fixes

## Pull Request Rules

Open pull requests into `develop` for normal work.

CI runs:

```bash
./mvnw clean verify
```

Pushes to `develop` and `main` also build the Docker image. Deployment is not automated yet.

## Commit Rules

Use clear product or engineering messages. Do not include tool names in commit messages.

Examples:

```text
Add backend environment profiles
Fix message visibility for archived conversations
Enforce subscription load limits
```
