#!/usr/bin/env python3
"""Every ACSubmitBuffers must be flushed, or whatever drew into it is silently dropped.

From 1.21.9 a renderer does not write vertices -- it *submits* nodes to a SubmitNodeCollector,
and this tree keeps its pre-1.21.2 draw bodies by recording them into
`client/render/compat/ACSubmitBuffers` and handing the recording over in `flush()`. Miss the
flush and the geometry is recorded, dropped on the floor, and nothing at all is logged: the
draw simply does not appear. That is exactly how the cave compendium came to open as a blank
screen on 26.2 while every other node drew it (`CaveBookPipRenderer` built one and never
flushed it -- report #9b of the first player bug report).

Below 1.21.9 the class does not exist and nothing here can fire, so this is a source-level
check rather than a per-node one: one construction site, one flush of that same variable,
inside the same method.

    python3 scripts/submit_flush_audit.py          # exit 1 if any site is unflushed
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'src', 'main', 'java')
# How far after the construction a flush still counts. Generous: the legacy draw body sits
# between them and some of them are long.
WINDOW = 60


def audit():
    misses, total = [], 0
    for dirpath, _, filenames in os.walk(ROOT):
        for filename in sorted(filenames):
            if not filename.endswith('.java'):
                continue
            path = os.path.join(dirpath, filename)
            # The class itself defines flush(); it has no construction site to check.
            if filename == 'ACSubmitBuffers.java':
                continue
            lines = open(path).read().split('\n')
            for i, line in enumerate(lines):
                if not re.search(r'new\s+(?:[\w.]*\.)?ACSubmitBuffers\s*\(', line):
                    continue
                total += 1
                # The declaration may wrap, so look back a couple of lines for the variable.
                head = '\n'.join(lines[max(0, i - 2):i + 1])
                match = re.search(r'ACSubmitBuffers\s+(\w+)\s*=', head)
                var = match.group(1) if match else None
                window = '\n'.join(lines[i:i + WINDOW])
                if not var or not re.search(r'\b' + re.escape(var) + r'\.flush\(\)', window):
                    misses.append((os.path.relpath(path, os.path.dirname(ROOT)), i + 1, var))
    return misses, total


def main():
    misses, total = audit()
    for path, line, var in misses:
        print('MISS %s:%d  %s is never flushed' % (path, line, var or '<not assigned to a variable>'))
    print('%d ACSubmitBuffers construction sites, %d without a flush' % (total, len(misses)))
    return 1 if misses else 0


if __name__ == '__main__':
    sys.exit(main())
