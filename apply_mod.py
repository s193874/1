#!/usr/bin/env python3
"""Apply the PlayTranslate Quick Bubble modification to an upstream checkout."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from datetime import datetime
from pathlib import Path

ACTIVITY_NAME = ".ui.FloatingIconStyleActivity"
ACTIVITY_XML = '''
        <!-- Added by PlayTranslate Quick Bubble mod -->
        <activity
            android:name=".ui.FloatingIconStyleActivity"
            android:exported="false" />
'''


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description="Apply Quick Bubble UI + bilingual note mode")
    parser.add_argument("repo", type=Path, help="Path to the PlayTranslate repository")
    parser.add_argument("--force", action="store_true", help="Apply even when upstream files differ")
    parser.add_argument("--dry-run", action="store_true", help="Check only; write nothing")
    args = parser.parse_args()

    bundle = Path(__file__).resolve().parent
    files_root = bundle / "files"
    checksums = json.loads((bundle / "upstream-checksums.json").read_text(encoding="utf-8"))
    repo = args.repo.expanduser().resolve()
    manifest = repo / "app/src/main/AndroidManifest.xml"
    if not manifest.is_file() or not (repo / "app/src/main/java/com/playtranslate").is_dir():
        print(f"错误：{repo} 看起来不是 PlayTranslate 仓库根目录。", file=sys.stderr)
        return 2

    mismatches: list[str] = []
    for rel, expected in checksums.items():
        target = repo / rel
        if not target.is_file():
            mismatches.append(f"缺少：{rel}")
        elif sha256(target) != expected:
            mismatches.append(f"内容不同：{rel}")
    if mismatches:
        print("检测到上游源码与本补丁制作时的版本不完全一致：")
        for item in mismatches:
            print(f"  - {item}")
        if not args.force:
            print("为避免覆盖错版本，已停止。确认后可加 --force。")
            return 3

    sources = sorted(p for p in files_root.rglob("*") if p.is_file())
    if args.dry_run:
        print(f"检查通过：将覆盖/新增 {len(sources)} 个文件，并登记一个 Activity。")
        return 0

    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup_root = repo / ".quickbubble-backup" / stamp
    created: list[str] = []
    overwritten: list[str] = []

    def backup_if_needed(target: Path, rel: str) -> None:
        if target.exists():
            backup = backup_root / rel
            backup.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(target, backup)
            overwritten.append(rel)
        else:
            created.append(rel)

    # Back up the manifest before text insertion.
    manifest_rel = manifest.relative_to(repo).as_posix()
    backup_if_needed(manifest, manifest_rel)

    for src in sources:
        rel = src.relative_to(files_root).as_posix()
        target = repo / rel
        backup_if_needed(target, rel)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, target)

    text = manifest.read_text(encoding="utf-8")
    if ACTIVITY_NAME not in text:
        marker = "</application>"
        if marker not in text:
            print("错误：AndroidManifest.xml 中没有 </application>，已保留备份。", file=sys.stderr)
            return 4
        text = text.replace(marker, ACTIVITY_XML + "\n    " + marker, 1)
        manifest.write_text(text, encoding="utf-8")

    record = {
        "created": sorted(set(created)),
        "overwritten": sorted(set(overwritten)),
        "repo": str(repo),
    }
    backup_root.mkdir(parents=True, exist_ok=True)
    (backup_root / "backup_manifest.json").write_text(
        json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    print("Quick Bubble 二改已套用。")
    print(f"备份：{backup_root}")
    print("Windows 构建：gradlew.bat assembleDebug")
    print("macOS/Linux 构建：./gradlew assembleDebug")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
