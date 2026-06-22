#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Build, collect, and optionally upload Night Veil APKs.

Examples:
  python pushApk.py
  python pushApk.py --variant release --clean
  python pushApk.py --skip-upload
  python pushApk.py --api-key your_key_here
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import mimetypes
import os
import pathlib
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
import uuid
from typing import Dict, Iterable, Optional, Tuple


ROOT = pathlib.Path(__file__).resolve().parent
APP_BUILD_GRADLE = ROOT / "app" / "build.gradle"
LOCAL_PROPERTIES = ROOT / "local.properties"
GRADLE_PROPERTIES = ROOT / "gradle.properties"
DIST_DIR = ROOT / "dist" / "apk"

PGYER_COS_TOKEN_URL = "https://www.pgyer.com/apiv2/app/getCOSToken"
DEFAULT_COMMIT_COUNT = 5


def print_step(message: str) -> None:
    print(f"\n=== {message} ===")


def run_command(
    command: Iterable[str],
    cwd: pathlib.Path = ROOT,
    check: bool = True,
) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        list(command),
        cwd=str(cwd),
        text=True,
        capture_output=True,
        encoding="utf-8",
        errors="replace",
    )
    if check and result.returncode != 0:
        raise RuntimeError(
            f"Command failed: {' '.join(command)}\n"
            f"stdout:\n{result.stdout}\n"
            f"stderr:\n{result.stderr}"
        )
    return result


def load_properties(path: pathlib.Path) -> Dict[str, str]:
    data: Dict[str, str] = {}
    if not path.exists():
        return data
    for raw_line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        data[key.strip()] = value.strip()
    return data


def resolve_api_key(cli_value: Optional[str]) -> Optional[str]:
    if cli_value:
        return cli_value.strip()

    env_value = os.environ.get("PGYER_API_KEY")
    if env_value:
        return env_value.strip()

    for props_path in (LOCAL_PROPERTIES, GRADLE_PROPERTIES):
        props = load_properties(props_path)
        for key in ("pgyer.apiKey", "PGYER_API_KEY"):
            value = props.get(key)
            if value:
                return value.strip()
    return None


def parse_version_info() -> Tuple[str, str]:
    content = APP_BUILD_GRADLE.read_text(encoding="utf-8", errors="replace")
    version_name_match = re.search(r'versionName\s+"([^"]+)"', content)
    version_code_match = re.search(r"versionCode\s+(\d+)", content)
    if not version_name_match or not version_code_match:
        raise RuntimeError("Could not parse versionName/versionCode from app/build.gradle")
    return version_name_match.group(1), version_code_match.group(1)


def parse_project_name() -> str:
    settings_file = ROOT / "settings.gradle"
    if settings_file.exists():
        content = settings_file.read_text(encoding="utf-8", errors="replace")
        match = re.search(r'rootProject\.name\s*=\s*"([^"]+)"', content)
        if match:
            return match.group(1).strip()
    return ROOT.name


def get_branch_name() -> str:
    jenkins_branch = os.environ.get("GIT_BRANCH")
    if jenkins_branch:
        return jenkins_branch.split("/")[-1]
    result = run_command(["git", "rev-parse", "--abbrev-ref", "HEAD"], check=False)
    branch_name = result.stdout.strip()
    return branch_name or "HEAD"


def get_git_commit_log(count: int) -> str:
    result = run_command(
        ["git", "log", f"-{count}", "--no-merges", "--pretty=format:%s"],
        check=False,
    )
    lines = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    return "\n".join(lines)


def build_update_description(
    variant: str,
    version_name: str,
    version_code: str,
    commit_count: int,
    extra_desc: Optional[str],
) -> str:
    branch_name = get_branch_name()
    commit_log = get_git_commit_log(commit_count)
    parts = [f"[{branch_name}] {variant.capitalize()}-{version_name}-{version_code}"]
    if extra_desc:
        parts.append(extra_desc.strip())
    if commit_log:
        parts.append(commit_log)
    return "\n".join(parts)


def get_gradle_command() -> list[str]:
    if os.name == "nt":
        return [str(ROOT / "gradlew.bat")]
    return [str(ROOT / "gradlew")]


def build_variant(variant: str, clean: bool) -> None:
    gradle = get_gradle_command()
    tasks = []
    if clean:
        tasks.append("clean")
    tasks.append(f"assemble{variant.capitalize()}")
    print_step(f"Running Gradle tasks: {' '.join(tasks)}")
    result = subprocess.run(
        gradle + tasks,
        cwd=str(ROOT),
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if result.returncode != 0:
        raise RuntimeError("Gradle build failed")


def find_apk(variant: str) -> pathlib.Path:
    apk_root = ROOT / "app" / "build" / "outputs" / "apk"
    if not apk_root.exists():
        raise RuntimeError("APK output directory does not exist yet")

    variant_lower = variant.lower()
    candidates = sorted(
        (
            path for path in apk_root.rglob("*.apk")
            if variant_lower in path.parts or variant_lower in path.name.lower()
        ),
        key=lambda p: p.stat().st_mtime,
        reverse=True,
    )
    if not candidates:
        raise RuntimeError(f"Could not find APK for variant: {variant}")
    return candidates[0]


def sanitize_name(name: str) -> str:
    return re.sub(r"[^A-Za-z0-9._-]+", "-", name).strip("-")


def build_release_apk_name(
    project_name: str,
    variant: str,
    version_name: str,
    version_code: str,
) -> str:
    timestamp = dt.datetime.now().strftime("%Y%m%d_%H%M")
    return (
        f"{sanitize_name(project_name)}_v{sanitize_name(version_name)}_"
        f"{version_code}_{sanitize_name(variant.lower())}_{timestamp}.apk"
    )


def prepare_distribution_apk(
    source_apk: pathlib.Path,
    project_name: str,
    variant: str,
    version_name: str,
    version_code: str,
) -> pathlib.Path:
    target_dir = DIST_DIR / variant.lower()
    target_dir.mkdir(parents=True, exist_ok=True)
    target_name = build_release_apk_name(project_name, variant, version_name, version_code)
    target_apk = target_dir / target_name
    shutil.copy2(source_apk, target_apk)
    return target_apk


def open_folder(path: pathlib.Path) -> None:
    try:
        if os.name == "nt":
            os.startfile(str(path))  # type: ignore[attr-defined]
            return
        if sys.platform == "darwin":
            subprocess.Popen(["open", str(path)])
            return
        subprocess.Popen(["xdg-open", str(path)])
    except Exception as exc:
        print(f"Could not open folder automatically: {path}\nReason: {exc}")


def encode_multipart_formdata(
    fields: Dict[str, str],
    file_field: Optional[Tuple[str, pathlib.Path]] = None,
) -> Tuple[bytes, str]:
    boundary = f"----NightVeil{uuid.uuid4().hex}"
    body = bytearray()

    for name, value in fields.items():
        body.extend(f"--{boundary}\r\n".encode("utf-8"))
        body.extend(
            f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode("utf-8")
        )
        body.extend(str(value).encode("utf-8"))
        body.extend(b"\r\n")

    if file_field:
        field_name, file_path = file_field
        mime_type = mimetypes.guess_type(file_path.name)[0] or "application/octet-stream"
        body.extend(f"--{boundary}\r\n".encode("utf-8"))
        body.extend(
            (
                f'Content-Disposition: form-data; name="{field_name}"; '
                f'filename="{file_path.name}"\r\n'
            ).encode("utf-8")
        )
        body.extend(f"Content-Type: {mime_type}\r\n\r\n".encode("utf-8"))
        body.extend(file_path.read_bytes())
        body.extend(b"\r\n")

    body.extend(f"--{boundary}--\r\n".encode("utf-8"))
    return bytes(body), f"multipart/form-data; boundary={boundary}"


def http_post_json(url: str, fields: Dict[str, str]) -> dict:
    body, content_type = encode_multipart_formdata(fields)
    request = urllib.request.Request(
        url=url,
        data=body,
        method="POST",
        headers={
            "Connection": "Keep-Alive",
            "Charset": "UTF-8",
            "Content-Type": content_type,
            "User-Agent": "NightVeil-PushApk/1.0",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            raw = response.read().decode("utf-8", errors="replace")
            return json.loads(raw)
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Failed to request Pgyer upload token: HTTP {exc.code}\n{detail}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Failed to request Pgyer upload token: {exc}") from exc


def http_post_file(url: str, fields: Dict[str, str], file_path: pathlib.Path) -> None:
    body, content_type = encode_multipart_formdata(fields, ("file", file_path))
    request = urllib.request.Request(
        url=url,
        data=body,
        method="POST",
        headers={
            "Connection": "Keep-Alive",
            "Charset": "UTF-8",
            "Content-Type": content_type,
            "User-Agent": "NightVeil-PushApk/1.0",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=300) as response:
            _ = response.read()
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"APK upload failed: HTTP {exc.code}\n{detail}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"APK upload failed: {exc}") from exc


def request_pgyer_token(api_key: str, update_description: str) -> dict:
    print_step("Requesting Pgyer upload token")
    result = http_post_json(
        PGYER_COS_TOKEN_URL,
        {
            "_api_key": api_key,
            "buildType": "android",
            "buildUpdateDescription": update_description,
        },
    )
    if result.get("code") != 0:
        raise RuntimeError(f"Pgyer returned an error: {json.dumps(result, ensure_ascii=False)}")
    return result


def upload_to_pgyer(api_key: str, apk_path: pathlib.Path, update_description: str) -> None:
    token_info = request_pgyer_token(api_key, update_description)
    data = token_info.get("data") or {}
    params = data.get("params") or {}
    endpoint = data.get("endpoint")
    if not endpoint or not params:
        raise RuntimeError(f"Pgyer upload token payload is incomplete: {json.dumps(token_info, ensure_ascii=False)}")

    print_step(f"Uploading APK to Pgyer: {apk_path.name}")
    upload_fields = {
        "key": str(params.get("key", "")),
        "signature": str(params.get("signature", "")),
        "x-cos-security-token": str(params.get("x-cos-security-token", "")),
    }
    http_post_file(endpoint, upload_fields, apk_path)
    print("Upload complete.")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build and optionally upload Night Veil APKs")
    parser.add_argument("--variant", default="release", help="Build variant, default: release")
    parser.add_argument("--api-key", help="Pgyer API key")
    parser.add_argument("--clean", action="store_true", help="Run clean before assemble")
    parser.add_argument("--skip-build", action="store_true", help="Skip Gradle build and reuse latest APK")
    parser.add_argument("--skip-upload", action="store_true", help="Build only, do not upload to Pgyer")
    parser.add_argument(
        "--commit-count",
        type=int,
        default=DEFAULT_COMMIT_COUNT,
        help=f"Recent commit count to include in changelog, default: {DEFAULT_COMMIT_COUNT}",
    )
    parser.add_argument("--desc", help="Extra text appended to the upload description")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    variant = args.variant.strip().lower()
    project_name = parse_project_name()
    version_name, version_code = parse_version_info()
    update_description = build_update_description(
        variant=variant,
        version_name=version_name,
        version_code=version_code,
        commit_count=args.commit_count,
        extra_desc=args.desc,
    )

    print_step("Package parameters")
    print(f"Project: {project_name}")
    print(f"Variant: {variant}")
    print(f"Version: {version_name} ({version_code})")
    print(f"Update description:\n{update_description}")

    if not args.skip_build:
        build_variant(variant, args.clean)

    apk_path = find_apk(variant)
    print_step("Resolved APK")
    print(f"APK: {apk_path}")
    print(f"Size: {apk_path.stat().st_size / 1024 / 1024:.2f} MB")

    dist_apk_path = prepare_distribution_apk(
        source_apk=apk_path,
        project_name=project_name,
        variant=variant,
        version_name=version_name,
        version_code=version_code,
    )
    print_step("Distribution APK")
    print(f"Output folder: {dist_apk_path.parent}")
    print(f"Output file: {dist_apk_path.name}")

    if args.skip_upload:
        print_step("Upload skipped by flag")
        open_folder(dist_apk_path.parent)
        return 0

    api_key = resolve_api_key(args.api_key)
    if not api_key:
        raise RuntimeError(
            "Pgyer API key not found. Use --api-key, set PGYER_API_KEY, "
            "or configure pgyer.apiKey in local.properties / gradle.properties."
        )

    upload_to_pgyer(api_key, dist_apk_path, update_description)
    open_folder(dist_apk_path.parent)
    print_step("Done")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("\nCancelled.", file=sys.stderr)
        raise SystemExit(130)
    except Exception as exc:
        print(f"\nFailed: {exc}", file=sys.stderr)
        raise SystemExit(1)
