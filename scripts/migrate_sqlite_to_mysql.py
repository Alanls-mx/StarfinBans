"""Copy StarfinBans punishment history from SQLite to an existing MySQL schema.

Dry-run is the default. Applying is idempotent by punishment UUID and aborts on
conflicting IDs. Credentials are read from the plugin config, never printed.
"""
import argparse
import json
import sqlite3
from pathlib import Path

import pymysql
import yaml

COLUMNS = (
    "id", "target", "target_name", "ip", "staff", "staff_name", "type",
    "reason", "created_at", "expires", "active",
)


def rows_from_sqlite(path):
    source = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
    source.row_factory = sqlite3.Row
    try:
        columns = {row[1] for row in source.execute("PRAGMA table_info(punishments)")}
        if not set(COLUMNS).issubset(columns):
            raise ValueError("SQLite source is missing punishment columns")
        return [tuple(row[column] for column in COLUMNS)
                for row in source.execute("SELECT * FROM punishments ORDER BY id")]
    finally:
        source.close()


def backup_sqlite(source_path, destination):
    with sqlite3.connect(f"file:{source_path}?mode=ro", uri=True) as source:
        with sqlite3.connect(destination) as target:
            source.backup(target)


def same_row(left, right):
    return tuple(left) == tuple(right)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sqlite", type=Path, required=True)
    parser.add_argument("--config", type=Path, required=True, help="StarfinBans config.yml with database settings")
    parser.add_argument("--apply", action="store_true", help="write missing rows to MySQL")
    parser.add_argument("--backup-dir", type=Path, help="new directory for backups; required with --apply")
    args = parser.parse_args()
    if args.apply and args.backup_dir is None:
        parser.error("--backup-dir is required with --apply")
    source_rows = rows_from_sqlite(args.sqlite.resolve())
    config = yaml.safe_load(args.config.read_text(encoding="utf-8"))["database"]
    connection = pymysql.connect(
        host=config["host"], port=int(config.get("port", 3306)),
        user=config["user"], password=config["password"], database=config["database"],
        connect_timeout=10, read_timeout=30, write_timeout=30, autocommit=False,
    )
    try:
        with connection.cursor() as cursor:
            cursor.execute("START TRANSACTION WITH CONSISTENT SNAPSHOT")
            cursor.execute("SHOW CREATE TABLE punishments")
            schema = cursor.fetchone()[1]
            cursor.execute("SELECT " + ", ".join(COLUMNS) + " FROM punishments ORDER BY id")
            existing_rows = cursor.fetchall()
            existing = {row[0]: row for row in existing_rows}
            missing = []
            for row in source_rows:
                prior = existing.get(row[0])
                if prior is None:
                    missing.append(row)
                elif not same_row(row, prior):
                    raise ValueError("Conflicting punishment ID: " + row[0])
            print(f"SQLite rows: {len(source_rows)}; MySQL rows: {len(existing_rows)}; missing: {len(missing)}")
            if not args.apply:
                connection.rollback()
                print("Dry-run only; no rows changed.")
                return
            backup_dir = args.backup_dir.resolve()
            backup_dir.mkdir(parents=True, exist_ok=False)
            backup_sqlite(args.sqlite.resolve(), backup_dir / "source.sqlite.db")
            (backup_dir / "mysql-punishments.schema.sql").write_text(schema + ";\n", encoding="utf-8")
            with (backup_dir / "mysql-punishments.jsonl").open("w", encoding="utf-8") as output:
                for row in existing_rows:
                    output.write(json.dumps(dict(zip(COLUMNS, row)), ensure_ascii=False, default=str) + "\n")
            insert = ("INSERT INTO punishments (" + ", ".join(COLUMNS) + ") VALUES (" +
                      ", ".join(["%s"] * len(COLUMNS)) + ")")
            for row in missing:
                cursor.execute(insert, row)
            connection.commit()
            print(f"Copied {len(missing)} row(s). Backups: {backup_dir}")
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


if __name__ == "__main__":
    main()
