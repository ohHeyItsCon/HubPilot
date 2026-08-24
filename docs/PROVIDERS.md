# Server Providers

Providers tell HubPilot how a managed server's power should be handled. Provider settings and credentials are kept separate so normal config can be shared without leaking API keys.

HubPilot currently includes Always-On, Crafty Controller, Pterodactyl, and Generic HTTP.

## Always-On

**Provider ID:** `always-online`  
**Provider type:** `always-on`

Always-On means HubPilot leaves that server's power to something else.

Use it for a hub that stays online, a 24/7 backend, a paid host that controls startup itself, or any server managed by Docker, systemd, another panel, or another process manager.

No controller URL or API key is needed. If the server is offline, HubPilot cannot start it.

## Crafty Controller setup

**Provider type:** `crafty`

Crafty is the provider with live beta testing behind it. HubPilot uses the Crafty Controller v2 API with Bearer-token authentication.

Official reference: [Crafty Controller API v2](https://docs.craftycontrol.com/pages/developer-guide/api-reference/v2/)

### 1. Create a Crafty API key

Create an API key for the Crafty user HubPilot will use. Crafty API keys are user-scoped, so a dedicated user with access only to the servers HubPilot needs is the safer setup.

For the known-working HubPilot setup, use a **Full Access** API key for that user. Crafty has had an API issue where a `COMMANDS`-only key can return HTTP 400 for start/stop actions even though the permission sounds like it should be enough.

Keep the key private. When it is entered through `/hp setup`, HubPilot stores it in `secrets.yml`.

### 2. Enter the Crafty panel URL

Use the URL that opens the Crafty panel itself:

```text
https://crafty.example.com
```

or:

```text
https://192.168.1.50:8443
```

Do **not** add `/api/v2`. HubPilot adds that path itself.

If Crafty uses a self-signed certificate, HubPilot has an `allow-insecure-tls` option. Leave it off unless the certificate cannot be validated normally and you trust that connection.

### 3. Save the key in HubPilot

The easiest option is `/hp setup`. Pick Crafty, enter the panel URL, then enter the API key when setup asks for it.

Manual equivalent:

```yaml
# providers.yml
primary-provider: crafty

providers:
  crafty:
    type: crafty
    enabled: true
    base-url: "https://crafty.example.com"
    secret: crafty-token
    allow-insecure-tls: false
    connect-timeout-seconds: 10
```

```yaml
# secrets.yml
secrets:
  crafty-token: "YOUR_CRAFTY_API_KEY"
```

You can use an environment variable instead:

```yaml
secrets:
  crafty-token: "${HUBPILOT_CRAFTY_TOKEN}"
```

### 4. Map servers to Crafty UUIDs

Crafty uses UUIDs as server IDs. HubPilot uses that UUID for Crafty start and stop requests.

If Crafty is the primary provider, `/hp discover` is the easiest way to pull in the live mappings. If you configure a server manually, set its Crafty UUID as `startup.provider-server-id`.

```yaml
startup:
  provider: crafty
  provider-server-id: "CRAFTY-SERVER-UUID"
```

HubPilot sends Crafty actions to:

```text
POST /api/v2/servers/<UUID>/action/start_server
POST /api/v2/servers/<UUID>/action/stop_server
```

### 5. Test it

The Crafty provider test calls the live `/api/v2/servers` inventory with the saved Bearer token. Once that passes, `/hp discover` should show Crafty-backed Velocity servers that can be added.

If the connection test passes but a server still will not start, check the [FAQ](FAQ.md).

## Pterodactyl setup

**Provider type:** `pterodactyl`

HubPilot uses the Pterodactyl **Client API** for power actions.

Pterodactyl reference: [pterodactyl/panel](https://github.com/pterodactyl/panel)

### 1. Create a Client API key

Sign into the Pterodactyl account that can access the servers HubPilot should control. Open **Account Settings > API Credentials** and create a Client API key.

Modern Client API keys normally start with:

```text
ptlc_
```

Do not use an Application API key that starts with `ptla_`. HubPilot calls the Client API, and Application keys do not authenticate to that endpoint.

If one key needs to control several servers, create it from an account that has access to those servers.

### 2. Enter the panel URL

Use the panel root URL:

```text
https://panel.example.com
```

Do **not** add `/api/client`. HubPilot adds it.

### 3. Save the key in HubPilot

Use `/hp setup`, choose Pterodactyl, enter the panel URL, then enter the `ptlc_...` Client API key.

Manual equivalent:

```yaml
# providers.yml
primary-provider: pterodactyl

providers:
  pterodactyl:
    type: pterodactyl
    enabled: true
    base-url: "https://panel.example.com"
    secret: pterodactyl-token
    connect-timeout-seconds: 10
```

```yaml
# secrets.yml
secrets:
  pterodactyl-token: "ptlc_YOUR_CLIENT_API_KEY"
```

### 4. Set each server's Pterodactyl identifier

HubPilot needs the Pterodactyl server **identifier**, not the display name.

If the panel URL looks like this:

```text
https://panel.example.com/server/1234abcd
```

then the provider server ID is:

```text
1234abcd
```

Configure the HubPilot server with:

```yaml
startup:
  provider: pterodactyl
  provider-server-id: "1234abcd"
```

HubPilot sends power requests to:

```text
POST /api/client/servers/<identifier>/power
```

with either `start` or `stop` as the signal.

### 5. Test it

The Pterodactyl provider test calls `/api/client` with the saved Client API key. A successful test confirms the panel URL and token work before HubPilot tries a server-specific power action.

Pterodactyl has controlled validation, but it has not had the same live beta coverage as Crafty yet.

## Generic HTTP setup

**Provider type:** `generic-http`

Generic HTTP is for hosts, panels, scripts, or webhooks that expose usable HTTP endpoints but do not have a dedicated HubPilot provider.

There is no single API-key setup for Generic HTTP because every service can be different. Check that service's API or webhook docs and find:

1. the endpoint that starts a server;
2. the endpoint that stops a server;
3. an optional status/test endpoint;
4. the HTTP method each endpoint expects;
5. the authentication format;
6. the server ID the service expects.

### URL and server ID

Generic HTTP URLs can contain `{server}`. HubPilot replaces it with that server's `startup.provider-server-id`.

Example:

```yaml
startup:
  provider: generic-http
  provider-server-id: "my-server-123"
```

with:

```text
https://host.example/api/servers/{server}/start
```

becomes:

```text
https://host.example/api/servers/my-server-123/start
```

### Authentication types

HubPilot supports:

- `none`
- `bearer`
- `x-api-key`
- `basic`

`bearer` sends:

```text
Authorization: Bearer <secret>
```

`x-api-key` sends:

```text
X-API-Key: <secret>
```

`basic` expects the secret in `username:password` form and sends a normal HTTP Basic Authorization header.

Custom headers can be added with `header.<Header-Name>` entries. Environment references such as `${TOKEN_NAME}` also work in custom header values.

### Example Generic HTTP provider

```yaml
# providers.yml
primary-provider: generic-http

providers:
  generic-http:
    type: generic-http
    enabled: true

    start-method: POST
    start-url: "https://host.example/api/servers/{server}/start"

    stop-method: POST
    stop-url: "https://host.example/api/servers/{server}/stop"

    status-method: GET
    status-url: "https://host.example/api/servers/{server}/status"
    test-server-id: "server-used-for-provider-test"

    secret: generic-http-token
    auth-type: bearer
    connect-timeout-seconds: 10
```

```yaml
# secrets.yml
secrets:
  generic-http-token: "YOUR_HOST_API_KEY"
```

If the API needs a body, HubPilot also supports `start-body` and `stop-body`. `{server}` works there too.

```yaml
start-method: POST
start-url: "https://host.example/api/power"
start-body: '{"server":"{server}","action":"start"}'

stop-method: POST
stop-url: "https://host.example/api/power"
stop-body: '{"server":"{server}","action":"stop"}'
```

If the service uses a custom header instead of one of the built-in auth types, use `auth-type: none` and add the header it expects:

```yaml
auth-type: none
header.X-Custom-Token: "${MY_HOST_TOKEN}"
```

### Testing Generic HTTP

If `status-url` is configured, HubPilot uses it for the provider test and replaces `{server}` with `test-server-id`.

If the service has no simple status endpoint, `status-url` can be left blank. Start and stop can still work, but the provider test cannot prove the remote power endpoint works before the first real request.

For Generic HTTP, the remote service's docs are the source of truth for the URL, method, body, and authentication format.

## If a server does not start

Check:

- server name and backend/provider mapping
- `startup.provider-server-id`
- port registered in Velocity
- address Velocity uses to reach the backend
- provider assigned to the server
- panel/controller URL
- API credential and permissions
- startup and ping timeout values
- whether the controller server was renamed or recreated

The full troubleshooting list is in the [FAQ](FAQ.md).

## Secrets

Never commit `secrets.yml` or post real API keys in an issue.

When possible, use a dedicated controller account with only the access HubPilot actually needs. Environment-variable references can also keep the credential out of the file itself.
