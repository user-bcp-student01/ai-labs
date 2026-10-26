#!/usr/bin/env python3
"""Run setup, development or tests from the monorepo root."""
import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def run(command, component):
    print(f'[{component}] {" ".join(map(str, command))}', flush=True)
    return subprocess.run(list(map(str, command)), cwd=ROOT / component).returncode


def python_api():
    folder = ROOT / 'api' / '.venv'
    return folder / ('Scripts/python.exe' if os.name == 'nt' else 'bin/python')


def execute(action, component):
    npm = shutil.which('npm')
    if component == 'auth':
        maven = shutil.which('mvn')
        if not maven:
            print('auth requiere JDK 21 y Maven 3.9+. También puedes usar Compose.', file=sys.stderr)
            return 1
        return run([maven, {'setup': '--version', 'start': 'quarkus:dev', 'test': 'verify'}[action]], 'auth')
    if component == 'api':
        executable = python_api()
        if action == 'setup':
            if sys.version_info < (3, 11) or sys.version_info >= (3, 14):
                print('Ejecuta setup api con Python 3.11–3.13; recomendado 3.13.', file=sys.stderr)
                return 1
            if not executable.exists():
                if executable.parent.parent.exists():
                    print('api/.venv existe pero no contiene Python. Revísalo sin reemplazarlo.', file=sys.stderr)
                    return 1
                result = run([sys.executable, '-m', 'venv', '.venv'], 'api')
                if result:
                    return result
            return run([executable, '-m', 'pip', 'install', '-r', 'requirements.txt', '-r', 'requirements-dev.txt'], 'api')
        if not executable.exists():
            print('Primero ejecuta: python tools/lab.py setup api', file=sys.stderr)
            return 1
        if action == 'start':
            return run([executable, '-m', 'uvicorn', 'app.main:app', '--host', '127.0.0.1', '--port', '8000', '--reload'], 'api')
        return run([executable, '-m', 'pytest', '--cov=app', '--cov-report=term-missing', '--cov-report=html', '--cov-fail-under=85'], 'api')
    if not npm:
        print('web requiere Node.js 22.12+ y npm.', file=sys.stderr)
        return 1
    commands = {'setup': [[npm, 'ci' if (ROOT / 'web/package-lock.json').exists() else 'install']],
                'start': [[npm, 'start']],
                'test': [[npm, 'test'], [npm, 'run', 'test:coverage'], [npm, 'run', 'build']]}
    for command in commands[action]:
        result = run(command, 'web')
        if result:
            return result
    return 0


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['setup', 'start', 'test'])
    parser.add_argument('component', choices=['auth', 'api', 'web'], nargs='?')
    args = parser.parse_args()
    if args.action == 'start' and not args.component:
        parser.error('Para los tres servicios usa docker compose up --build. Para uno, indica auth, api o web.')
    results = [execute(args.action, part) for part in ([args.component] if args.component else ['auth', 'api', 'web'])]
    sys.exit(1 if any(results) else 0)
