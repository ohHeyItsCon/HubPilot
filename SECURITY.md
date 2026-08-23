# Security

HubPilot can store credentials for server-management APIs. Treat those credentials the same way as an admin password.

Never post any of the following in a public issue, screenshot, log, or config dump:

- `secrets.yml`
- provider API keys or tokens
- authorization headers
- passwords
- session cookies
- private panel URLs that expose sensitive infrastructure

Check logs and configuration files for sensitive values before posting them.

## Reporting a security problem

Use GitHub private vulnerability reporting or security advisories if they are available for this repository.

If private reporting is unavailable, open a short public issue asking for a private contact method. Do not include exploit details or credentials in that issue.
