#!/usr/bin/env bash
# Publica las capturas en la rama `screenshots` solo si hay cambios visuales relevantes.
set -euo pipefail

new_dir="${1:?Directorio con las capturas nuevas}"
source_sha="${2:?Commit de origen}"
branch="screenshots"
pub="$RUNNER_TEMP/screenshots-branch"

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

if git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
  git fetch -q origin "$branch"
  git worktree add -q -B "$branch" "$pub" FETCH_HEAD
else
  git worktree add -q --detach "$pub"
  (cd "$pub" && git checkout -q --orphan "$branch" && git rm -rfq . && git clean -fdxq)
fi

python3 -m pip install --quiet --disable-pip-version-check --break-system-packages pillow \
  || python3 -m pip install --quiet --disable-pip-version-check pillow
if python3 .github/scripts/screenshots_changed.py "$pub/capturas" "$new_dir"; then
  echo "No se publican capturas: no hay cambios visuales relevantes."
  exit 0
fi

rm -rf "$pub/capturas"
mkdir -p "$pub/capturas"
cp "$new_dir"/*.png "$pub/capturas/"
echo "$source_sha" > "$pub/SOURCE_COMMIT"
{
  echo "# Capturas de Despierta"
  echo
  echo "Generadas automáticamente en un emulador Android 14 (Pixel 6) con datos ficticios y hora fija."
  echo
  echo "Commit de origen: \`$source_sha\`"
  echo
  for image in "$pub"/capturas/*.png; do
    name="$(basename "$image" .png)"
    echo "## $name"
    echo
    echo "![$name](capturas/$name.png)"
    echo
  done
} > "$pub/README.md"

cd "$pub"
git add -A
git commit -q -m "Capturas de ${source_sha:0:12}"
git push -q origin "$branch"
echo "Capturas publicadas en la rama $branch."
