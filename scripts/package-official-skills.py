#!/usr/bin/env python3
"""Package the standalone skills-only plugin; no runtime build or network access."""
import argparse
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

ROOT = Path(__file__).resolve().parent.parent
PACKAGE = ROOT / "plugins" / "universal-official-skills"
FILES = (
    "plugin.json",
    "skills/find-official-skills/SKILL.md",
    "skills/find-official-skills/references/sources.json",
)


def package(output: Path, source: Path = PACKAGE, license_path: Path = ROOT / "LICENSE"):
    entries = []
    for relative in FILES:
        path = source / relative
        if not path.is_file() or path.is_symlink() or not path.resolve().is_relative_to(source.resolve()):
            raise ValueError(f"Missing or unsafe package resource: {relative}")
        entries.append((relative, path.read_bytes()))
    entries.append(("LICENSE", license_path.read_bytes()))
    output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(output, "w", compression=ZIP_DEFLATED) as archive:
        for relative, content in entries:
            info = ZipInfo(f"universal-official-skills/{relative}", date_time=(2026, 10, 4, 0, 0, 0))
            info.compress_type = ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, content)
    return output


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "dist/universal-official-skills.zip")
    args = parser.parse_args()
    print(package(args.output.resolve()))
