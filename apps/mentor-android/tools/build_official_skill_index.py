#!/usr/bin/env python3
"""Builds app/src/main/assets/official_skills_index.json from live GitHub data.

Only repositories owned by allowlisted, GitHub-verified organisations are indexed. For each
repository the script checks: owner is an Organization, org `is_verified` is true, the repo is
not a fork, not archived, and owned by that org. Every SKILL.md is fetched from
raw.githubusercontent.com (HTTPS) and every skill folder URL on github.com must return 200.
Nothing is written by hand: names/descriptions come from each SKILL.md front matter.

Uses the `gh` CLI for API calls when it is available (higher rate limit), otherwise
unauthenticated HTTPS requests. Run: python3 tools/build_official_skill_index.py
"""
import concurrent.futures as cf
import datetime as dt
import json
import pathlib
import shutil
import subprocess
import sys
import urllib.request

# Repositories that are the company's own public skill catalogue.
SOURCE_REPOS = [
    "anthropics/skills",
    "openai/skills",
    "google/skills",
    "microsoft/skills",
    "vercel-labs/agent-skills",
    "huggingface/skills",
    "supabase/agent-skills",
    "getsentry/skills",
    "expo/skills",
]

OUT = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/official_skills_index.json"


def api(path):
    if shutil.which("gh"):
        out = subprocess.run(["gh", "api", path], check=True, capture_output=True, text=True).stdout
        return json.loads(out)
    req = urllib.request.Request("https://api.github.com/" + path, headers={"Accept": "application/vnd.github+json"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def get(url, method="GET"):
    req = urllib.request.Request(url, method=method, headers={"User-Agent": "lumina-skill-index"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.status, (r.read().decode("utf-8", "replace") if method == "GET" else "")


def front_matter(text):
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        return {}
    out, key = {}, None
    for line in lines[1:]:
        if line.strip() == "---":
            break
        if line[:1] in (" ", "\t") and key:
            out[key] = (out[key] + " " + line.strip()).strip()
            continue
        if ":" in line:
            key, val = line.split(":", 1)
            key = key.strip()
            val = val.strip()
            if val in ("|", ">", "|-", ">-"):
                val = ""
            out[key] = val.strip('"').strip("'")
    return out


def main():
    orgs, repos, skills, rejected = {}, [], [], []
    for full in SOURCE_REPOS:
        owner, name = full.split("/")
        if owner not in orgs:
            o = api(f"orgs/{owner}")
            orgs[owner] = {
                "login": o["login"], "name": o.get("name") or o["login"], "type": o.get("type"),
                "isVerified": bool(o.get("is_verified")), "blog": o.get("blog") or "",
                "htmlUrl": o["html_url"],
            }
        org = orgs[owner]
        r = api(f"repos/{full}")
        problems = []
        if r["owner"]["type"] != "Organization": problems.append("owner is not an organization")
        if r["owner"]["login"].lower() != owner.lower(): problems.append("owner mismatch")
        if not org["isVerified"]: problems.append("organization not verified")
        if r["fork"]: problems.append("fork")
        if r["archived"]: problems.append("archived")
        if r.get("private"): problems.append("private")
        if problems:
            rejected.append({"repo": full, "reasons": problems})
            continue
        branch = r["default_branch"]
        repos.append({
            "fullName": r["full_name"], "owner": r["owner"]["login"], "ownerType": r["owner"]["type"],
            "htmlUrl": r["html_url"], "description": r.get("description") or "",
            "defaultBranch": branch, "license": (r.get("license") or {}).get("spdx_id") or "",
            "fork": r["fork"], "archived": r["archived"], "pushedAt": r["pushed_at"],
        })
        tree = api(f"repos/{full}/git/trees/{branch}?recursive=1")
        paths = sorted(x["path"] for x in tree["tree"] if x["type"] == "blob" and x["path"].endswith("SKILL.md"))

        def one(path):
            folder = path.rsplit("/", 1)[0] if "/" in path else ""
            raw = f"https://raw.githubusercontent.com/{full}/{branch}/{path}"
            page = f"https://github.com/{full}/tree/{branch}/{folder}" if folder else r["html_url"]
            status, text = get(raw)
            page_status, _ = get(page, "HEAD")
            fm = front_matter(text)
            return {
                "repo": full, "path": folder, "name": fm.get("name") or folder.rsplit("/", 1)[-1],
                "description": (fm.get("description") or "")[:600], "license": fm.get("license", "")[:120],
                "skillUrl": page, "rawUrl": raw, "rawStatus": status, "pageStatus": page_status,
            }

        with cf.ThreadPoolExecutor(6) as ex:
            found = list(ex.map(one, paths))
        seen = set()
        for s in found:
            if s["rawStatus"] != 200 or s["pageStatus"] != 200 or not s["description"]:
                rejected.append({"repo": full, "path": s["path"], "reasons": ["unreachable or no description"]})
                continue
            key = s["name"].lower()
            if key in seen:
                continue
            seen.add(key)
            del s["rawStatus"], s["pageStatus"]
            skills.append(s)
        print(full, len(paths), "SKILL.md ->", sum(1 for s in skills if s["repo"] == full), file=sys.stderr)
    data = {
        "schema": 1,
        "generatedAt": dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "organizations": sorted(orgs.values(), key=lambda o: o["login"]),
        "repositories": repos,
        "skills": skills,
    }
    OUT.write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n")
    print(json.dumps({"skills": len(skills), "repos": len(repos), "rejected": rejected}, indent=1), file=sys.stderr)


if __name__ == "__main__":
    main()
