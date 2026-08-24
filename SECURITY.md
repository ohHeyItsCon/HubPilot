# Security

HubPilot can store credentials for server-management APIs. Treat them like admin passwords.

Never put any of these in a public issue, screenshot, log, or config dump:

- `secrets.yml`
- provider API keys or tokens
- authorization headers
- passwords
- session cookies
- private panel URLs that expose sensitive infrastructure

Check logs and config files before posting them. Redact anything private first.

## Reporting a security problem

Use GitHub private vulnerability reporting or security advisories if they are available for this repo.

If private reporting is not available, open a short public issue asking for a private contact method. Do not include exploit details, credentials, or other sensitive information in the public issue.
