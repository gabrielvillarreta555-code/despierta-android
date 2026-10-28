#!/usr/bin/env bash
# Publica las capturas de validación en la rama `validacion-capturas` (siempre, sin filtro):
# cada ejecución es un commit, así se conserva el historial de comparaciones.
set -euo pipefail

dir="${1:?Directorio con las capturas}"
source_sha="${2:?Commit candidato}"
base_sha="${3:?Commit base}"
config="${4:-todas}"
branch="validacion-capturas-$config"
pub="$RUNNER_TEMP/validacion-branch-$config"

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

if git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
  git fetch -q origin "$branch"
  git worktree add -q -B "$branch" "$pub" FETCH_HEAD
else
  git worktree add -q --detach "$pub"
  (cd "$pub" && git checkout -q --orphan "$branch" && git rm -rfq . && git clean -fdxq)
fi

rm -rf "$pub/capturas" "$pub/registros"
mkdir -p "$pub/capturas"
cp "$dir"/*.png "$pub/capturas/" 2>/dev/null || true
mkdir -p "$pub/registros" && cp "$dir"/*.txt "$pub/registros/" 2>/dev/null || true
printf 'candidata %s\nbase %s\n' "$source_sha" "$base_sha" > "$pub/COMMITS"
cd "$pub"
git add -A
git commit -q --allow-empty -m "Validación de ${source_sha:0:12} contra ${base_sha:0:12}"
git push -q origin "$branch"
