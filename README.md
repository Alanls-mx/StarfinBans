# StarfinBans

StarfinBans is a Java 8 punishment plugin with Bukkit/Spigot and BungeeCord
entry points. A Maven build creates two distribution JARs from the current
source tree:

- `target/StarfinBans-<version>-Spigot.jar`
- `target/StarfinBans-<version>-Bungee.jar`

The JARs contain only the classes and descriptor for their platform. The server
API is provided by the running server and is not embedded. SQLite, MySQL and
HikariCP are packaged; HikariCP and MySQL Connector/J are relocated under the
StarfinBans namespace. The GitHub Actions build also publishes both files as a
workflow artifact, using names such as `StarfinBans-Spigot-1.0.0.jar`.
Built-in player messages use ASCII-safe text for legacy Minecraft fonts. The
message formatter also transliterates accented text and strips unsupported
Unicode symbols from existing `messages.yml` entries without replacing a
server's customized file.

## Build and tests

Use JDK 8 and Maven:

```text
mvn clean verify
python scripts/verify_distributions.py
```

The tests cover duration parsing for both platform adapters. The distribution
check inspects the generated JAR contents and Java class-file versions. These
checks do not replace testing against real Spigot, BungeeCord or MySQL servers.

## Installation and authorization

- **Spigot:** install the Spigot JAR and the compatible StarfinCore on each
  Bukkit server. StarfinBans requests authorization for product
  `starfin_bans`. PlaceholderAPI is optional. Vault is not required.
- **BungeeCord:** install the Bungee distributions of StarfinCore and
  StarfinBans on the proxy. StarfinBans requires Core, reads its live product
  authorization and independently verifies the signed grant before enabling
  punishment commands and listeners.

The two adapters are still separate code packages. They share a MySQL schema
but require separate Core installation identities for the proxy and backend.

## Storage

The default storage is SQLite, stored in `plugins/StarfinBans/database.db` on
Spigot and the Bungee plugin data directory on the proxy. SQLite files are
local to each installation; SQLite does not synchronize a network.

Set `storage-type: "mysql"` on every participating instance to use one shared
MySQL 8 database. Configure `database.host`, `port`, `database`, `user`,
`password`, `use-ssl`, `pool-size` and `connection-timeout` in each platform's
config. Create the database and a least-privilege user before starting the
plugins. Do not use a root account. MySQL connection or query failures are
reported as failures; this implementation does not silently switch storage.
Database read failures during login/chat checks are handled fail-closed: login
is refused or the chat message is blocked until punishment status can be
verified.

Both adapters use the same `punishments` table layout for the existing fields
(UUIDs, player/IP, type, reason, timestamps and active state). Initialization
creates missing tables and indexes and records schema version 1 without
dropping existing punishment rows. Take a database backup before upgrades.
There is not yet an administrator-triggered SQLite-to-MySQL migration command.

The current schema does not implement network/server scope or a persistent
cross-instance event queue. Shared MySQL makes reads and writes common, but
does not provide immediate network-wide side effects. Bungee remains the
recommended enforcement point for proxy-wide bans; avoid configuring duplicate
punishment enforcement on proxy and every backend until scoped synchronization
is implemented.

The `memory` storage is volatile and is only suitable for local development.

## Web integration and remaining platform limitations

The Bungee adapter currently sends outbound HTTP notifications for configured
create/expire actions. The Spigot web service is a placeholder, and neither
adapter implements authenticated inbound administrative operations or durable
event recovery. The existing StarfinWeb endpoint contract must be agreed and
implemented before describing this as bidirectional synchronization.

`/changereason` accepts the eight-character ID shown by `/history` or a full
UUID. It rejects ambiguous prefixes and preserves the punishment's other
fields. `/unwarn <player>` deactivates active warnings while preserving history.

## Backups and troubleshooting

Back up the SQLite database file or use a consistent MySQL backup before
changing storage settings or upgrading. A MySQL startup failure is logged with
the cause and must be resolved at the database/network/credential layer; do
not switch to SQLite for a shared network unless all instances are deliberately
reconfigured to the same authoritative storage.
