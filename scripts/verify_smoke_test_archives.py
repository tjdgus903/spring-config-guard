"""Check the actual plugin/sample archives before CI offers them for installation."""

import io
from pathlib import Path
import xml.etree.ElementTree as ET
import zipfile


ROOT = Path(__file__).resolve().parents[1]
PLUGIN_ID = "io.github.tjdgus903.springconfigguard"
PLUGIN_VERSION = "0.1.0"
PLUGIN_LOGOS = ("META-INF/pluginIcon.svg", "META-INF/pluginIcon_dark.svg")
PLUGIN_HOMEPAGE = "https://github.com/tjdgus903/spring-config-guard"
VENDOR_HOMEPAGE = "https://github.com/tjdgus903"
PACKAGED_LICENSE = "META-INF/LICENSE"


def check_integrity(archive):
    corrupt = archive.testzip()
    if corrupt is not None:
        raise ValueError(f"Corrupt ZIP entry: {corrupt}")


def verify_plugin_logo(jar, name):
    if name not in jar.namelist():
        raise ValueError(f"The packaged plugin logo is missing: {name}")
    root = ET.fromstring(jar.read(name))
    if root.tag.rsplit("}", 1)[-1] != "svg":
        raise ValueError(f"The packaged plugin logo is not SVG: {name}")
    if root.get("viewBox") != "0 0 40 40":
        raise ValueError(f"The packaged plugin logo must use a 40x40 view box: {name}")
    for element in root.iter():
        for value in element.attrib.values():
            normalized = value.strip().lower()
            if normalized.startswith(("http:", "https:", "//")) or "url(http" in normalized:
                raise ValueError(f"The packaged plugin logo contains an external reference: {name}")


def verify_plugin():
    plugins = list((ROOT / "build/distributions").glob("*.zip"))
    if len(plugins) != 1:
        raise ValueError(f"Expected one installable plugin ZIP, found {len(plugins)}")
    descriptors = 0
    with zipfile.ZipFile(plugins[0]) as archive:
        check_integrity(archive)
        for name in archive.namelist():
            if not name.endswith(".jar"):
                continue
            with zipfile.ZipFile(io.BytesIO(archive.read(name))) as jar:
                check_integrity(jar)
                test_only_packages = ("com/intellij/driver/", "com/intellij/ide/starter/", "org/junit/", "org/kodein/", "kotlin/")
                if any(p.startswith(test_only_packages) or "ConfigKeyMappingUiTest" in p for p in jar.namelist()):
                    raise ValueError(f"IDE test code or dependencies leaked into the Java plugin: {name}")
                if "META-INF/plugin.xml" not in jar.namelist():
                    continue
                descriptor = ET.fromstring(jar.read("META-INF/plugin.xml"))
                if descriptor.findtext("id") != PLUGIN_ID:
                    continue
                descriptors += 1
                if descriptor.findtext("version") != PLUGIN_VERSION:
                    raise ValueError(f"Expected plugin version {PLUGIN_VERSION}")
                if descriptor.get("url") != PLUGIN_HOMEPAGE:
                    raise ValueError("The packaged plugin descriptor has an unexpected homepage URL")
                vendor = descriptor.find("vendor")
                if vendor is None or vendor.get("url") != VENDOR_HOMEPAGE:
                    raise ValueError("The packaged plugin descriptor has an unexpected vendor URL")
                description = "".join(descriptor.find("description").itertext()).strip()
                if "Project source and configuration are not uploaded" not in description:
                    raise ValueError("The packaged plugin description is missing the local-only privacy statement")
                change_notes = descriptor.find("change-notes")
                if change_notes is None or "0.1.0" not in "".join(change_notes.itertext()):
                    raise ValueError("The packaged plugin descriptor is missing 0.1.0 change notes")
                for logo in PLUGIN_LOGOS:
                    verify_plugin_logo(jar, logo)
                if PACKAGED_LICENSE not in jar.namelist():
                    raise ValueError("The packaged Apache 2.0 license is missing")
                if jar.read(PACKAGED_LICENSE) != (ROOT / "LICENSE").read_bytes():
                    raise ValueError("The packaged license differs from the repository LICENSE")
                actions = {a.get("id"): a.get("class") for a in descriptor.findall("./actions/action")}
                action_class = actions.get("SpringConfigGuard.AnalyzeConfigKeyMappings")
                if action_class is None or action_class.replace(".", "/") + ".class" not in jar.namelist():
                    raise ValueError("The packaged key mapping action or its compiled class is missing")
                if any("SmokeTestSampleTest" in p or p.endswith("DemoClient.class") for p in jar.namelist()):
                    raise ValueError("Test/sample classes leaked into the plugin")
    if descriptors != 1:
        raise ValueError(f"Expected one Spring Config Guard descriptor, found {descriptors}")
    print(f"Verified installable plugin: {plugins[0].name}")


def verify_sample():
    source = ROOT / "samples/config-mapping"
    expected = {
        "config-mapping/" + path.relative_to(source).as_posix(): path
        for path in (source / "src/main").rglob("*") if path.is_file()
    }
    for name in ("README.md", "build.gradle.kts", "settings.gradle.kts"):
        expected["config-mapping/" + name] = source / name
    for name in ("gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"):
        expected["config-mapping/" + name] = ROOT / name
    sample = ROOT / "build/smoke-test/spring-config-guard-sample.zip"
    with zipfile.ZipFile(sample) as archive:
        check_integrity(archive)
        actual = {entry.filename for entry in archive.infolist() if not entry.is_dir()}
        if actual != expected.keys():
            raise ValueError(f"Sample ZIP mismatch: missing={expected.keys() - actual}, extra={actual - expected.keys()}")
        for name, path in expected.items():
            if archive.read(name) != path.read_bytes():
                raise ValueError(f"Packaged sample differs from its source: {name}")
        if not (archive.getinfo("config-mapping/gradlew").external_attr >> 16) & 0o111:
            raise ValueError("The sample Unix Wrapper is not executable")
    print(f"Verified sample: {len(expected)} files match source; Unix Wrapper is executable")


if __name__ == "__main__":
    verify_plugin()
    verify_sample()
