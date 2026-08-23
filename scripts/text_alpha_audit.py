#!/usr/bin/env python3
"""Flag text-draw call sites whose colour argument is a literal with an EMPTY alpha byte.

Why this exists
---------------
Until 1.21.5 every ``Font#drawInBatch`` / ``drawInBatch8xOutline`` ran the colour through a
private ``adjustColor``::

    (color & 0xFC000000) == 0 ? ARGB.opaque(color) : color

so the ``0xRRGGBB`` literals mods have always written rendered fully opaque. **Vanilla deleted
that method at 1.21.6.** From there the colour goes straight to the glyph quads, a missing alpha
byte means alpha 0, and the text is submitted, batched and drawn completely transparent --
nothing logged, nothing thrown. That is what blanked the Cave Compendium and the spelunkery
table on 1.21.6 -> 26.2.

The fix is ``ACColors.opaque(...)``: a no-op below 1.21.6 (the game would have done it anyway),
so it needs no ``//?`` gate. This script finds the call sites that still need it.

Usage:  python3 scripts/text_alpha_audit.py [--all]
        --all   also list call sites whose colour is a non-literal expression (unverifiable
                by inspection) instead of only the provably-broken literals.
"""
import os, re, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'java')

# (receiver-kind, method) -> zero-based indexes into the argument list AS WRITTEN.
# "wrapper" = ACClientCompat's Font-first overloads, which already run every colour through
# ACColors.opaque -- those sites are safe by construction and are never reported.
GUI_ARGS = {
    'drawString':          [4],   # (font, text, x, y, colour[, shadow])
    'drawCenteredString':  [4],
    'drawWordWrap':        [5],   # (font, text, x, y, width, colour)
    'drawScrollingString': [5],
}
FONT_ARGS = {
    'drawInBatch':          [3],     # (text, x, y, colour, shadow, matrix, buffer, mode, bg, light)
    'drawInBatch8xOutline': [3, 4],  # (text, x, y, colour, outlineColour, ...)
}
ALL_METHODS = set(GUI_ARGS) | set(FONT_ARGS)

# <receiver>.<method>( -- the receiver is what tells a safe wrapper call from a raw vanilla one
CALL = re.compile(r'(?:([A-Za-z_][\w.$()\[\]]*)\s*\.\s*)?\b(' + '|'.join(sorted(ALL_METHODS)) + r')\s*\(')
WRAPPER_RECEIVERS = ('ACClientCompat',)


def split_args(s):
    """Split a top-level argument list (the text between the call's parentheses)."""
    out, depth, cur, i, n = [], 0, [], 0, len(s)
    instr = inchr = False
    while i < n:
        c = s[i]
        if instr:
            if c == '\\':
                cur.append(c); i += 1
                if i < n: cur.append(s[i])
                i += 1; continue
            if c == '"': instr = False
        elif inchr:
            if c == '\\':
                cur.append(c); i += 1
                if i < n: cur.append(s[i])
                i += 1; continue
            if c == "'": inchr = False
        elif c == '"': instr = True
        elif c == "'": inchr = True
        elif c in '([{': depth += 1
        elif c in ')]}':
            if depth == 0: break
            depth -= 1
        elif c == ',' and depth == 0:
            out.append(''.join(cur)); cur = []; i += 1; continue
        cur.append(c); i += 1
    out.append(''.join(cur))
    return [a.strip() for a in out], i


LITERAL = re.compile(r'^-?(0[xX][0-9a-fA-F]+|\d+)[lL]?$')
DECL = re.compile(r'^(final\s+)?[A-Za-z_][\w.$<>,\s]*(\[\])?\s+[a-z_]\w*$')


def literal_value(expr):
    e = expr.replace('_', '').strip().rstrip('lL')
    if not LITERAL.match(e):
        return None
    neg = e.startswith('-')
    if neg:
        e = e[1:]
    v = int(e, 16) if e[:2].lower() == '0x' else int(e)
    return (-v) & 0xFFFFFFFF if neg else v


def scan(path, show_all):
    src = open(path, encoding='utf-8', errors='replace').read()
    lines = src.split('\n')
    # line offsets so we can report a line number from a character index
    starts, off = [], 0
    for ln in lines:
        starts.append(off); off += len(ln) + 1

    def line_of(idx):
        lo, hi = 0, len(starts) - 1
        while lo < hi:
            mid = (lo + hi + 1) // 2
            if starts[mid] <= idx: lo = mid
            else: hi = mid - 1
        return lo + 1

    hits = []
    for m in CALL.finditer(src):
        recv, name = m.group(1), m.group(2)
        if recv and recv.split('.')[-1] in WRAPPER_RECEIVERS:
            continue          # goes through ACColors.opaque already
        if recv and recv.split('.')[-1] in ALL_METHODS:
            continue          # matched the tail of a longer chain, not a receiver
        args, _ = split_args(src[m.end():])
        if not args:
            continue
        if all(DECL.match(a) for a in args):
            continue          # this is the wrapper's own declaration, not a call
        idxs = GUI_ARGS.get(name) or FONT_ARGS[name]
        for i in idxs:
            if i >= len(args):
                continue
            expr = args[i]
            v = literal_value(expr)
            if v is not None:
                if (v & 0xFC000000) == 0:
                    hits.append((line_of(m.start()), name, expr, 'ZERO-ALPHA literal'))
            elif show_all and 'opaque(' not in expr:
                hits.append((line_of(m.start()), name, expr, 'unverifiable expression'))
    return hits


def main():
    show_all = '--all' in sys.argv
    total = 0
    for dirpath, _, files in os.walk(ROOT):
        for f in sorted(files):
            if not f.endswith('.java'):
                continue
            p = os.path.join(dirpath, f)
            hits = scan(p, show_all)
            if not hits:
                continue
            rel = os.path.relpath(p, ROOT)
            for ln, name, expr, why in hits:
                print(f'{rel}:{ln}  {name}(... {expr} ...)   <- {why}')
                if why.startswith('ZERO'):
                    total += 1
    print()
    print(f'zero-alpha text colours: {total}')
    return 1 if total else 0


if __name__ == '__main__':
    sys.exit(main())
