#!/usr/bin/env python3
"""Compara dos carpetas de capturas PNG.

Sale con 0 si no hay cambios visuales relevantes y con 1 si los hay (o si cambia la lista de
pantallas). Una captura cuenta como cambiada si más del 0,2 % de sus píxeles difiere de forma
apreciable; así el ruido mínimo de renderizado no genera publicaciones nuevas.
"""
import os
import sys

from PIL import Image, ImageChops

PIXEL_THRESHOLD = 24      # diferencia mínima por canal para contar un píxel
RATIO_THRESHOLD = 0.002   # proporción de píxeles distintos para considerar un cambio


def pngs(folder):
    if not os.path.isdir(folder):
        return set()
    return {name for name in os.listdir(folder) if name.endswith(".png")}


def main(old_dir, new_dir):
    old, new = pngs(old_dir), pngs(new_dir)
    changes = []
    for name in sorted(new - old):
        changes.append(f"nueva: {name}")
    for name in sorted(old - new):
        changes.append(f"retirada: {name}")
    for name in sorted(old & new):
        a = Image.open(os.path.join(old_dir, name)).convert("RGB")
        b = Image.open(os.path.join(new_dir, name)).convert("RGB")
        if a.size != b.size:
            changes.append(f"tamaño distinto: {name}")
            continue
        mask = ImageChops.difference(a, b).convert("L").point(lambda v: 255 if v > PIXEL_THRESHOLD else 0)
        ratio = mask.histogram()[255] / (a.size[0] * a.size[1])
        if ratio > RATIO_THRESHOLD:
            changes.append(f"cambiada ({ratio:.2%}): {name}")
    if changes:
        print("Cambios visuales:\n  " + "\n  ".join(changes))
        return 1
    print("Sin cambios visuales relevantes.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
