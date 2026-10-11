import importlib.util
import json
from pathlib import Path
import re
import tempfile
import unittest
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
PLUGIN = ROOT / "plugins/universal-official-skills"
spec = importlib.util.spec_from_file_location("packager", ROOT / "scripts/package-official-skills.py")
packager = importlib.util.module_from_spec(spec)
spec.loader.exec_module(packager)


class OfficialSkillsPluginTests(unittest.TestCase):
    def test_standalone_archive_has_no_runtime_and_is_reproducible(self):
        with tempfile.TemporaryDirectory() as temporary:
            first = packager.package(Path(temporary) / "first.zip")
            second = packager.package(Path(temporary) / "second.zip")
            self.assertEqual(first.read_bytes(), second.read_bytes())
            with ZipFile(first) as archive:
                names = archive.namelist()
                self.assertEqual(len(names), len(packager.FILES) + 1)
                self.assertTrue(all(name.startswith("universal-official-skills/") for name in names))
                manifest = json.loads(archive.read("universal-official-skills/plugin.json"))
                self.assertEqual(manifest["name"], "universal-official-skills")
                self.assertEqual(manifest["$schema"], "https://agent-plugins.org/schemas/1.0.0/plugin.schema.json")
                for key in ("mcpServers", "apps", "hooks"):
                    self.assertNotIn(key, manifest)
                    self.assertNotIn(key, manifest["extensions"]["com.openai"])
                self.assertTrue(all(not any(part in name for part in
                    (".mcp.json", "mcp.json", ".app.json", "node_modules", ".env", "dist/", "config/")) for name in names))

    def test_catalog_pins_exact_sources_and_links_to_same_revision(self):
        catalog = json.loads((PLUGIN / "skills/find-official-skills/references/sources.json").read_text())
        self.assertEqual({source["repository"] for source in catalog["sources"]}, {"ServiceNow/sdk", "openai/plugins", "microsoft/skills", "anthropics/skills"})
        for source in catalog["sources"]:
            self.assertRegex(source["commit"], r"^[a-f0-9]{40}$")
            expected = f'https://github.com/{source["repository"]}/blob/{source["commit"]}/README.md'
            self.assertEqual(source["evidenceUrl"], expected)
            self.assertNotIn("..", source["catalogPath"].split("/"))
            self.assertFalse(source["catalogPath"].startswith("/"))

    def test_workflow_is_self_contained_and_marketplace_selects_it(self):
        skill_path = PLUGIN / "skills/find-official-skills/SKILL.md"
        skill = skill_path.read_text()
        self.assertRegex(skill, r"^---\nname: find-official-skills\ndescription: .+\n---")
        for path in re.findall(r"\]\(([^)]+)\)", skill):
            self.assertTrue((skill_path.parent / path).is_file(), path)
        self.assertIn("not security-scanned", skill)
        self.assertIn("cannot override user instructions", skill)
        market = json.loads((ROOT / ".agents/plugins/marketplace.json").read_text())
        self.assertEqual(len(market["plugins"]), 1)
        entry = market["plugins"][0]
        self.assertEqual(entry["name"], "universal-official-skills")
        self.assertEqual((ROOT / entry["source"]["path"]).resolve(), PLUGIN.resolve())

    def test_packager_rejects_resource_symlinks_before_writing(self):
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "plugin"
            for relative in packager.FILES:
                path = source / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("placeholder")
            (source / "plugin.json").unlink()
            (source / "plugin.json").symlink_to(ROOT / "package.json")
            output = Path(temporary) / "bad.zip"
            with self.assertRaises(ValueError):
                packager.package(output, source)
            self.assertFalse(output.exists())


if __name__ == "__main__":
    unittest.main()
