# Server Providers

A provider tells HubPilot how a managed server's power should be handled. Provider settings and provider secrets are stored separately so the normal configuration can be shared without exposing API keys.

HubPilot currently includes Always-On, Crafty Controller, Pterodactyl, and Generic HTTP.

## Always-On

**Provider ID:** `always-online`  
**Provider type:** `always-on`

Always-On leaves server power management outside HubPilot.

Use it when the hub stays online all the time, a backend runs 24/7, or another system already handles startup and shutdown.

No controller URL or API key is needed. If an Always-On server is offline, HubPilot cannot turn it on.

## Crafty Controller setup

**Provider type:** `crafty`

Crafty is the provider with live beta testing behind it. HubPilot uses the Crafty Controller v2 API with Bearer-token authentication.

Official reference: [Crafty Controller API v2](https://docs.craftycontrol.com/pages/developer-guide/api-reference/v2/)

### 1. Create a Crafty API key

Create an API key for the Crafty user HubPilot will use. API keys in current Crafty releases are user-scoped, so the safer setup is a dedicated Crafty user that only has access to the Minecraft servers HubPilot needs to control.

For the known-working HubPilot setup, use a **Full Access** API key for that Crafty user. Crafty currently has a documented API issue where a `COMMANDS`-only key can return HTTP 400 for start/stop actions even though that permission looks like it should be enough.

Keep the key private. HubPilot stores it in `secrets.yml` when it is entered through `/hp setup`.

### 2. Enter the Crafty panel URL

Use the URL that reaches the Crafty web panel itself, for example:

```text
https://crafty.example.com
```

or:

```text
https://192.168.1.50:8443
```

Do **not** add `/api/v2` to the URL. HubPilot adds the API path itself.

If Crafty is using a self-signed certificate, HubPilot has an `allow-insecure-tls` option. Keep it off unless the certificate cannot be validated normally and the connection is trusted.

### 3. Save the key in HubPilot

The easiest route is `/hp setup`. Select Crafty, enter the panel URL, then enter the API key when setup asks for it.

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

An environment variable can be used instead:

```yaml
secrets:
  crafty-token: "${HUBPILOT_CRAFTY_TOKEN}"
```

### 4. Map servers to Crafty UUIDs

Crafty has used UUIDs as server IDs since the 4.3 server-ID change. HubPilot uses that UUID for Crafty start and stop requests.

When Crafty is the primary provider, `/hp discover` is the easiest way to bring in the correct live mappings. If a server is configured manually, its HubPilot server file needs the Crafty UUID as `startup.provider-server-id`.

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

HubPilot's Crafty provider test calls the live `/api/v2/servers` inventory using the saved Bearer token. After that passes, `/hp discover` should show the Crafty-backed Velocity servers that are eligible to be added.

If the connection works but a server still will not start, check the [FAQ](FAQ.md).

## Pterodactyl setup

**Provider type:** `pterodactyl`

HubPilot uses the Pterodactyl **Client API** for power operations.

Pterodactyl reference: [pterodactyl/panel](https://github.com/pterodactyl/panel)

### 1. Create a Client API key

Sign into the Pterodactyl account that has access to the servers HubPilot should control, then open **Account Settings > API Credentials** and create a Client API key.

Modern Client API keys normally start with:

```text
ptlc_
```

Do not use an Application API key that starts with `ptla_`. HubPilot calls the Client API, and Pterodactyl Application keys do not authenticate to the Client API.

If one key needs to control multiple servers, create the key from an account that has access to those servers. An administrator's Client API key can access the Client API for servers available to that admin account.

### 2. Enter the panel URL

Use the panel root URL:

```text
https://panel.example.com
```

Do **not** add `/api/client`. HubPilot adds that path itself.

### 3. Save the key in HubPilot

Use `/hp setup`, select Pterodactyl, enter the panel URL, then enter the `ptlc_...` Client API key.

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

The identifier is the short value used in the server URL. For example:

```text
https://panel.example.com/server/1234abcd
```

uses:

```text
1234abcd
```

as the provider server ID.

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

with either `start` or `stop` as the requested signal.

### 5. Test it

HubPilot's Pterodactyl provider test calls `/api/client` using the saved Client API key. A successful test confirms the panel URL and token work before any server-specific power request is attempted.

Pterodactyl support has controlled validation but does not yet have the same live beta coverage as Crafty.

## Generic HTTP setup

**Provider type:** `generic-http`

Generic HTTP is for hosts, panels, scripts, or webhooks that can expose HTTP endpoints but do not have a dedicated HubPilot adapter.

There is no single API-key creation process for Generic HTTP because every host can do it differently. Start with the API or webhook documentation for the service being connected and find these pieces:

1. the endpoint used to start a server;
2. the endpoint used to stop a server;
3. an optional endpoint that can be called to test/status-check the server;
4. the HTTP method each endpoint expects;
5. the authentication format required by the service;
6. the server ID that the service expects.

### URL and server ID

Generic HTTP URLs may contain `{server}`. HubPilot replaces it with that server's `startup.provider-server-id`.

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

`basic` expects the secret value in `username:password` form and sends a normal HTTP Basic Authorization header.

Custom headers can also be added with `header.<Header-Name>` entries in the provider config. Environment references such as `${TOKEN_NAME}` are supported in custom header values.

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

If the API needs a request body, HubPilot also supports `start-body` and `stop-body`. `{server}` can be used in those values too.

For example:

```yaml
start-method: POST
start-url: "https://host.example/api/power"
start-body: '{"server":"{server}","action":"start"}'

stop-method: POST
stop-url: "https://host.example/api/power"
stop-body: '{"server":"{server}","action":"stop"}'
```

If the service uses a custom header instead of the built-in auth types, use `auth-type: none` and add the required header:

```yaml
auth-type: none
header.X-Custom-Token: "${MY_HOST_TOKEN}"
```

### Testing Generic HTTP

If `status-url` is configured, HubPilot uses it for the provider connection test and replaces `{server}` with `test-server-id`.

If the host does not offer a simple status endpoint, `status-url` can be left blank. In that case the provider can still be used for start and stop, but the provider test cannot prove the remote power endpoint works before the first real request.

Because Generic HTTP depends entirely on the remote service, check that service's documentation for the exact URL, method, body, and authentication format.

## If a server does not start

Check:

- the server name and backend/provider mapping
- `startup.provider-server-id`
- the port registered in Velocity
- the address Velocity uses to reach the backend
- the provider assigned to the server
- panel/controller URL
- API credential and permissions
- startup and ping timeout values
- whether the controller server was renamed or recreated

The full troubleshooting list is in the [FAQ](FAQ.md).

## Secrets

Never commit `secrets.yml` or post real API keys in a GitHub issue.

Where possible, use a dedicated controller account with only the access HubPilot actually needs. Environment-variable references can also keep the credential out of the file itself.
