# ThunderChat Storage

ThunderChat is designed to work with or without MySQL.

## MySQL mode

When `storage.type: mysql` is configured, ThunderChat attempts to connect to the configured MySQL/MariaDB server during startup.

If the connection succeeds:

- HikariCP is used for pooled JDBC connections.
- The configured database is created automatically when the MySQL account has permission.
- ThunderChat tables are created automatically.
- Persistent state uses the shared MySQL database.
- Network packets for empty backends can use the persistent MySQL queue.

A proxy network should normally use **one shared ThunderChat database** for all Paper backends. Each backend only changes `network.server-name`.

## Automatic fallback

MySQL is **not a hard dependency for running the plugin**.

If MySQL is configured but cannot be reached, ThunderChat detects the failure and prints a clearly visible red console warning. When `storage.fallback: yaml` is enabled (the default), managers automatically use their existing local YAML persistence paths instead.

The plugin continues to start and normal chat/moderation features remain available.

Example warning:

```text
[ThunderChat] WARNING: MySQL is unavailable.
[ThunderChat] Falling back to local YAML storage. The plugin will continue running without MySQL.
```

The fallback is intentionally automatic. Server owners do not need to install MySQL just to start ThunderChat during development or on a standalone server.

## Configuration

```yaml
storage:
  type: mysql
  fallback: yaml
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "thunderchat"
    username: "root"
    password: "change-me"
    pool-size: 4
```

### `storage.type`

- `mysql` — try MySQL first.
- `yaml` — intentionally use local YAML storage without attempting MySQL.

### `storage.fallback`

- `yaml` — if MySQL fails, continue using local YAML storage.
- Any other value — do not enable the automatic YAML fallback. The plugin still runs, but persistent storage that depends on the storage layer will be unavailable until MySQL works.

## Important network limitation

YAML fallback is local to each Paper backend. It cannot replace the shared database for cross-server state.

In particular, when MySQL is unavailable:

- local chat still works;
- local filters still work;
- local mutes/ignores/spy settings can persist through YAML;
- normal live proxy plugin messaging can still operate where a carrier player is available;
- the persistent empty-backend network queue is unavailable;
- shared cross-server persistence cannot be guaranteed.

Once MySQL is restored, restart ThunderChat (or restart the backend) so the MySQL storage layer is initialized again.

## Recommended deployments

### Single Paper server

MySQL is optional. YAML fallback is perfectly valid for development or a small standalone server.

### Velocity/Bungee network

Use one shared MySQL database for all backends when possible:

```text
Velocity
   ├── lobby
   ├── survival
   ├── skyblock
   └── bedwars
          │
          └── MySQL: thunderchat
```

Do **not** create one ThunderChat database per gamemode unless you intentionally want isolated installations.

## Performance

The normal MySQL path is:

```text
Gameplay
  ↓
in-memory cache
  ↓
dirty state
  ↓
batched async JDBC
  ↓
HikariCP
  ↓
shared MySQL
```

The gameplay path does not synchronously write MySQL. The YAML fallback preserves the plugin's existing local-file behavior when a shared database is unavailable.

## Troubleshooting

If the red MySQL warning appears:

1. Check that MySQL/MariaDB is running.
2. Check the configured host and port.
3. Check the username and password.
4. Check that the account can connect from the Paper server.
5. If automatic database creation is desired, give the account permission to create the configured database.
6. If this is only a development/standalone server, you can leave `storage.fallback: yaml` enabled and continue running without MySQL.

The warning is deliberately loud so a production network does not silently run without shared persistence.
