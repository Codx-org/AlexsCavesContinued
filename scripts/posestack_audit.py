#!/usr/bin/env python3
"""Find a ``popPose()`` that can underflow its ``PoseStack``.

Why this exists
---------------
``PoseStack``'s constructor seeds it with ONE base pose.  Up to MC 1.21.4 the
stack was a Deque and ``popPose()`` was a bare ``removeLast()``, so popping that
base pose silently emptied a one-element deque and nothing ever complained.  From
**1.21.5** ``popPose()`` opens with ``if (lastIndex == 0) throw new
NoSuchElementException();`` -- so the identical code is an instant hard crash on
**33 of the 58 nodes**, thrown from deep inside the render dispatch where the
stack trace names the vanilla method rather than the mod's.

That is what killed the Teletor renderer: ``TeletorModel#translateToHead`` built
a fresh ``PoseStack``, never pushed, read ``last().pose()``, and then popped.

What it checks
--------------
For every ``PoseStack x = new PoseStack()`` in ``src/main/java``, it walks the
enclosing block keeping a running depth and reports any ``x.popPose()`` reached
while the depth is 0.  Locally-constructed stacks only -- a stack received as a
parameter legitimately has depth its caller pushed, and this says nothing about
those.

Usage
-----
    python3 scripts/posestack_audit.py        # exit 1 on any underflow
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src/main/java")
NEW = re.compile(r'(\w+)\s*=\s*new PoseStack\(\)')


def scan(path):
    lines = open(path, encoding="utf-8", errors="replace").read().split("\n")
    out = []
    for i, line in enumerate(lines):
        m = NEW.search(line)
        if not m:
            continue
        var = m.group(1)
        push = re.compile(r'\b' + re.escape(var) + r'\.pushPose\(')
        pop = re.compile(r'\b' + re.escape(var) + r'\.popPose\(')
        depth, brace = 0, 0
        for j in range(i, len(lines)):
            t = lines[j]
            if push.search(t):
                depth += 1
            if pop.search(t):
                depth -= 1
                if depth < 0:
                    out.append((j + 1, var, t.strip()))
                    break
            brace += t.count("{") - t.count("}")
            if j > i and brace < 0:
                break
    return out


def main():
    problems = 0
    stacks = 0
    for dp, _, fns in os.walk(ROOT):
        for fn in sorted(fns):
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dp, fn)
            src = open(path, encoding="utf-8", errors="replace").read()
            stacks += len(NEW.findall(src))
            for lineno, var, text in scan(path):
                rel = os.path.relpath(path, ROOT)
                print(f"UNDERFLOW {rel}:{lineno}  {var}  |  {text}")
                problems += 1
    print(f"\n{stacks} locally-constructed PoseStack(s); {problems} underflow(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
