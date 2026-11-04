#!/usr/bin/env python3
"""Busca un elemento en un volcado de uiautomator (stdin) y escribe el centro "x y".

Uso: ui-find.py text|desc|textci|class VALOR [índice]
"""
import re
import sys
import xml.etree.ElementTree as ET

mode, value = sys.argv[1], sys.argv[2]
index = int(sys.argv[3]) if len(sys.argv) > 3 else 0
raw = sys.stdin.read()
start = raw.find("<?xml")
if start < 0:
    sys.exit(1)
root = ET.fromstring(raw[start:raw.rfind(">") + 1])
matches = []
for node in root.iter("node"):
    text, desc, cls = node.get("text", ""), node.get("content-desc", ""), node.get("class", "")
    ok = (
        (mode == "text" and text == value)
        or (mode == "textci" and text.strip().lower() == value.lower())
        or (mode == "desc" and desc == value)
        or (mode == "class" and cls == value)
    )
    if ok:
        b = re.findall(r"\d+", node.get("bounds", ""))
        if len(b) == 4:
            x1, y1, x2, y2 = map(int, b)
            matches.append(((x1 + x2) // 2, (y1 + y2) // 2))
if len(matches) <= index:
    sys.exit(1)
print(*matches[index])
