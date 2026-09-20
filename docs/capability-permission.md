# Capability and permission specification

Capabilities are a closed vocabulary evaluated by the firewall **independently** of skill text.

```
filesystem.read
filesystem.write
network.read
network.write
shell.execute
process.spawn
credential.read
browser.control
database.read
database.write
cloud.read
cloud.write
production.deploy
```

## Decisions

`ALLOW` | `DENY` | `REQUIRE_USER_APPROVAL`

Default policy (see `config/sandbox-policy.yaml` / trust+security):

| Capability | Default |
| --- | --- |
| filesystem.read | ALLOW for documentation skills after gate |
| filesystem.write | DENY |
| network.* | DENY unless declared **and** permitted |
| shell.execute / process.spawn | DENY |
| credential.read | DENY |
| production.deploy / cloud.write | DENY (CRITICAL; explicit authorization) |

## Self-grant prohibition

`request_capability` is the only mutation path. It records an audit event and consults policy + optional `approver`. Skill manifest fields cannot add to `effectivePermissions`.

Composed skills inherit the **strictest** constraint among components.
