"""Writes a version tag's release notes from the pull requests merged since the previous tag.

    python tools/release/changelog.py v1.8.0 out/

A beta tag (v1.8.0-beta.1, any tag with a "-") lists what changed since the previous tag of either kind; a production
tag, since the previous production tag, so it covers its betas. Pull requests labelled skip-changelog are left out.
Needs git history with tags, and the gh CLI signed in (GH_TOKEN in CI).

Writes out/release-notes.md for the GitHub release, and out/play-notes.txt for Google Play unless a
fastlane/metadata/android/*/changelogs/default.txt was edited since the previous tag: those are then uploaded as written.
"""

import json
import subprocess
import sys
from pathlib import Path

PLAY_LIMIT = 500
SKIP_LABEL = "skip-changelog"
CHANGELOGS = "fastlane/metadata/android/*/changelogs/*"


def git(*args: str) -> str:
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout.strip()


def previous_tag(tag: str) -> str | None:
    args = ["describe", "--tags", "--abbrev=0", "--match", "v*"]
    if "-" not in tag:
        args += ["--exclude", "v*-*"]
    try:
        return git(*args, f"{tag}^")
    except subprocess.CalledProcessError:
        return None  # The first tag.


def merged_pulls(repo: str, commits: list[str]) -> list[dict]:
    """The merged pull requests these commits came from, oldest first."""
    pulls = {}
    for sha in reversed(commits):
        out = subprocess.run(["gh", "api", f"repos/{repo}/commits/{sha}/pulls"], check=True, capture_output=True, text=True).stdout
        for pull in json.loads(out):
            if pull["merged_at"] and pull["number"] not in pulls:
                pulls[pull["number"]] = pull
    return [p for p in pulls.values() if SKIP_LABEL not in {label["name"] for label in p["labels"]}]


def play_notes(titles: list[str]) -> str:
    lines = [f"• {t}" for t in titles] or ["• Bug fixes and improvements"]
    for kept in range(len(lines), 0, -1):
        rest = len(lines) - kept
        text = "\n".join(lines[:kept] + ([f"• And {rest} more"] if rest else []))
        if len(text) <= PLAY_LIMIT:
            return text
    return lines[0][: PLAY_LIMIT - 1] + "…"


def main() -> int:
    tag, out = sys.argv[1], Path(sys.argv[2])
    repo = git("config", "--get", "remote.origin.url").removesuffix(".git").split("github.com")[-1].strip("/:")
    prev = previous_tag(tag)
    commits = git("rev-list", f"{prev}..{tag}" if prev else tag).split()
    pulls = merged_pulls(repo, commits)

    lines = [f"- {p['title']} (#{p['number']})" for p in pulls] or ["No pull requests merged."]
    notes = "## What's changed\n\n" + "\n".join(lines) + "\n"
    if prev:
        notes += f"\n**Full changelog**: https://github.com/{repo}/compare/{prev}...{tag}\n"
    out.mkdir(parents=True, exist_ok=True)
    (out / "release-notes.md").write_text(notes, encoding="utf-8")

    edited = prev and git("diff", "--name-only", prev, tag, "--", CHANGELOGS)
    if edited:
        print(f"Changelogs edited since {prev}, Google Play gets them as committed:\n{edited}")
    else:
        (out / "play-notes.txt").write_text(play_notes([p["title"] for p in pulls]) + "\n", encoding="utf-8")
    print(f"Pull requests since {prev or 'the start'}: {len(pulls)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
