"""Validación de Python, sin ejecutar herramientas de otras plataformas."""

import argparse
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
UV = [shutil.which("uv")] if shutil.which("uv") else [sys.executable, "-m", "uv"]


def is_python_branch(branch):
    return branch == "python-cli" or branch.rsplit("/", 1)[-1] == "python"


def run(args, cwd=ROOT):
    print("+", " ".join(map(str, args)), flush=True)
    subprocess.run(list(map(str, args)), cwd=cwd, check=True)


def suite():
    project = ROOT / "python"
    if not (project / "src").is_dir() or not (project / "tests").is_dir():
        raise ValueError(
            "Faltan python/src y python/tests; integra el código Python primero"
        )
    out = ROOT / ".artifacts" / "python"
    out.mkdir(parents=True, exist_ok=True)
    run(UV + ["sync", "--locked", "--project", project])
    uv = UV + ["run", "--locked", "--project", project]
    run(uv + ["ruff", "format", "--check", "."], project)
    run(uv + ["ruff", "check", "."], project)
    run(
        uv
        + [
            "pytest",
            "--cov=src",
            "--cov-fail-under=80",
            "--cov-report=term-missing",
            f"--cov-report=xml:{out / 'coverage.xml'}",
            f"--cov-report=html:{out / 'html'}",
            f"--junitxml={out / 'junit.xml'}",
        ],
        project,
    )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("stage", choices=("pre-commit", "pre-push", "ci", "test"))
    parser.add_argument("--branch")
    args, _ = parser.parse_known_args()
    if args.stage in ("ci", "test"):
        suite()
        return
    branch = (
        args.branch
        or subprocess.check_output(
            ["git", "branch", "--show-current"], cwd=ROOT, text=True
        ).strip()
    )
    if args.stage == "pre-push":
        # Git pasa refspecs por stdin: se valida la rama enviada, incluso
        # cuando el checkout actual pertenece a otra plataforma.
        refs = [line.split() for line in sys.stdin if line.strip()]
        if not any(
            ref[0].startswith("refs/heads/")
            and is_python_branch(ref[0].removeprefix("refs/heads/"))
            and ref[1].strip("0")
            for ref in refs
        ):
            return
    elif not is_python_branch(branch):
        print(f"Rama {branch}: sin revisiones Python automáticas")
        return
    suite()


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        sys.exit(1)
