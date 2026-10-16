import pathlib
import sys
import zipfile
import xml.etree.ElementTree as ET


ROOT = pathlib.Path(__file__).resolve().parents[1]
TARGET = ROOT / "target"
POM = ET.parse(ROOT / "pom.xml").getroot()
VERSION = POM.find("{http://maven.apache.org/POM/4.0.0}version").text


def verify_legacy_text_resources():
    for name in ("messages.yml", "plugin.yml", "bungee.yml"):
        path = ROOT / "src" / "main" / "resources" / name
        if path.is_file() and any(byte >= 128 for byte in path.read_bytes()):
            raise AssertionError(name + " contains non-ASCII text unsupported by legacy Minecraft fonts")


def verify(name, platform):
    path = TARGET / ("StarfinBans-" + VERSION + "-" + platform + ".jar")
    if not path.is_file():
        raise AssertionError("Missing distribution: " + str(path))

    with zipfile.ZipFile(path) as archive:
        entries = set(archive.namelist())
        if ("plugin.yml" in entries) != (platform == "Spigot"):
            raise AssertionError(platform + " JAR has incorrect plugin.yml presence")
        if ("bungee.yml" in entries) != (platform == "Bungee"):
            raise AssertionError(platform + " JAR has incorrect bungee.yml presence")

        if platform == "Spigot":
            required = "com/starfinplugins/bans/StarfinBansPlugin.class"
            wrong_platform_class = lambda entry: (
                entry.startswith("br/com/starfinplugins/bans/")
                and not entry.startswith("br/com/starfinplugins/bans/security/")
            )
        else:
            required = "br/com/starfinplugins/bans/bungee/StarfinBansBungeePlugin.class"
            wrong_platform_class = lambda entry: (
                entry.startswith("com/starfinplugins/bans/")
                and not entry.startswith("com/starfinplugins/bans/libs/")
            )
        if required not in entries:
            raise AssertionError(platform + " JAR contains incorrect platform classes")
        if platform == "Bungee":
            descriptor = archive.read("bungee.yml").decode("utf-8")
            if "- StarfinCore" not in descriptor:
                raise AssertionError("Bungee JAR does not require StarfinCore")
            if "br/com/starfinplugins/bans/bungee/command/UnwarnCommand.class" not in entries:
                raise AssertionError("Bungee JAR is missing unwarn")
        if any(
            entry.endswith(".class") and wrong_platform_class(entry)
            for entry in entries
        ):
            raise AssertionError(platform + " JAR includes classes from the other platform")

        if "org/sqlite/JDBC.class" not in entries:
            raise AssertionError(platform + " JAR is missing the SQLite driver")
        if "com/starfinplugins/bans/libs/hikari/HikariDataSource.class" not in entries:
            raise AssertionError(platform + " JAR is missing isolated HikariCP")
        if "com/starfinplugins/bans/libs/mysql/cj/jdbc/Driver.class" not in entries:
            raise AssertionError(platform + " JAR is missing isolated MySQL Connector/J")
        service = "META-INF/services/java.sql.Driver"
        if service not in entries:
            raise AssertionError(platform + " JAR is missing JDBC driver service registration")
        service_content = archive.read(service).decode("utf-8")
        if "org.sqlite.JDBC" not in service_content:
            raise AssertionError(platform + " JAR does not register SQLite JDBC")
        if "com.starfinplugins.bans.libs.mysql.cj.jdbc.Driver" not in service_content:
            raise AssertionError(platform + " JAR does not register relocated MySQL JDBC")

        platform_apis = (
            "org/bukkit/Bukkit.class",
            "net/md_5/bungee/api/ProxyServer.class",
        )
        if any(api in entries for api in platform_apis):
            raise AssertionError(platform + " JAR embeds a platform API")

        for entry in entries:
            if not entry.endswith(".class") or entry.startswith("META-INF/versions/"):
                continue
            class_bytes = archive.read(entry)
            major = int.from_bytes(class_bytes[6:8], byteorder="big")
            if major > 52:
                raise AssertionError(
                    platform + " JAR contains non-Java-8 class " + entry + " (major " + str(major) + ")"
                )

    print("Verified " + path.name)


try:
    verify_legacy_text_resources()
    verify("StarfinBans", "Spigot")
    verify("StarfinBans", "Bungee")
except (AssertionError, OSError, zipfile.BadZipFile) as error:
    print(str(error), file=sys.stderr)
    sys.exit(1)
