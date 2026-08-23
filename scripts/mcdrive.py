#!/usr/bin/env python3
"""Drive the dev client: find its window, focus it, type chat commands, click.

Keyboard goes through **ydotool** (uinput), not XTEST.  XTEST *keyboard* injection is
silently dead on this box -- the events reach the X server and the game never sees one,
so every `key`/`chat` call looks like it worked and does nothing.  `ydotoold` must be
running and the user must be in the `input` group.  Window lookup, focus and the pointer
stay on Xlib/XTEST, which do work.  (The pointer still never reaches Minecraft itself --
see docs/notes/gotchas-runtime.md #4 -- so in-world actions want the rebound keys
b/v/n, and clicks inside a GUI screen are not automatable.)

Usage:  python3 mcdrive.py <cmd> [args...]
  find                       -> print window id/geometry
  focus                      -> raise + focus + warp pointer to centre
  chat "<text>"              -> press T, type text, Enter
  key <NAME>...              -> press keysym names (Escape, Return, w, ...)
  keyhold <NAME> <seconds>   -> hold one key down (charging a bow, walking)
  click <1|2|3> [n]          -> mouse button n times at current pos
  hold <1|2|3> <seconds>
  move <dx> <dy>             -> relative pointer motion (mouse-look)
  shot <path.png>            -> capture the window
  sleep <seconds>
"""
import os, subprocess, sys, time
SHOTENV = dict(os.environ, DISPLAY=':0')
from Xlib import X, XK, display
from Xlib.ext import xtest

d = display.Display(':0')
root = d.screen().root


def mc_window():
    best = None
    def walk(w, depth=0):
        nonlocal best
        try:
            name = w.get_wm_name()
            g = w.get_geometry()
        except Exception:
            return
        if name and 'Minecraft' in name and g.width > 200:
            best = (w, g)
        try:
            for c in w.query_tree().children:
                walk(c, depth + 1)
        except Exception:
            pass
    walk(root)
    return best


def press_keysym(sym, down=True):
    code = d.keysym_to_keycode(sym)
    xtest.fake_input(d, X.KeyPress if down else X.KeyRelease, code)
    d.sync()


# X keysym name -> Linux input-event scancode (what ydotool speaks).
YKEYS = {
    'Escape': 1, 'minus': 12, 'equal': 13, 'BackSpace': 14, 'Tab': 15,
    'bracketleft': 26, 'bracketright': 27, 'Return': 28, 'Control_L': 29,
    'semicolon': 39, 'apostrophe': 40, 'grave': 41, 'Shift_L': 42, 'backslash': 43,
    'comma': 51, 'period': 52, 'slash': 53, 'Shift_R': 54, 'Alt_L': 56, 'space': 57,
    'Caps_Lock': 58, 'F11': 87, 'F12': 88,
    'Home': 102, 'Up': 103, 'Prior': 104, 'Left': 105, 'Right': 106, 'End': 107,
    'Down': 108, 'Next': 109, 'Insert': 110, 'Delete': 111,
}
for _i, _c in enumerate('1234567890'):
    YKEYS[_c] = 2 + _i
for _i, _c in enumerate('qwertyuiop'):
    YKEYS[_c] = 16 + _i
for _i, _c in enumerate('asdfghjkl'):
    YKEYS[_c] = 30 + _i
for _i, _c in enumerate('zxcvbnm'):
    YKEYS[_c] = 44 + _i
for _i in range(1, 11):
    YKEYS['F%d' % _i] = 58 + _i


def ydo(*args):
    subprocess.run(['ydotool', *args], check=True)


SHIFTED = {'/': 'slash', ':': 'colon', '@': 'at', '~': 'asciitilde', '_': 'underscore',
           '!': 'exclam', '?': 'question', '"': 'quotedbl', '{': 'braceleft', '}': 'braceright',
           '[': 'bracketleft', ']': 'bracketright', '=': 'equal', '-': 'minus', ',': 'comma',
           '.': 'period', ' ': 'space', "'": 'apostrophe', '#': 'numbersign', '$': 'dollar',
           '%': 'percent', '^': 'asciicircum', '&': 'ampersand', '*': 'asterisk',
           '(': 'parenleft', ')': 'parenright', '+': 'plus'}


def type_text(text):
    ydo('type', '--', text)


def key(name, hold=0.03):
    code = YKEYS.get(name) or YKEYS.get(name.lower())
    if code is None:
        print('  !! no scancode for', name); return
    ydo('key', '%d:1' % code); time.sleep(hold); ydo('key', '%d:0' % code)


def focus():
    got = mc_window()
    if not got:
        print('NO WINDOW'); sys.exit(2)
    w, g = got
    t = w.translate_coords(root, 0, 0)
    x, y = -t.x, -t.y
    w.configure(stack_mode=X.Above)
    d.sync()
    na = d.intern_atom('_NET_ACTIVE_WINDOW')
    from Xlib import protocol
    ev = protocol.event.ClientMessage(window=w, client_type=na, data=(32, [2, X.CurrentTime, 0, 0, 0]))
    root.send_event(ev, event_mask=X.SubstructureRedirectMask | X.SubstructureNotifyMask)
    d.sync()
    time.sleep(0.4)
    w.set_input_focus(X.RevertToParent, X.CurrentTime)
    d.sync()
    xtest.fake_input(d, X.MotionNotify, x=x + g.width // 2, y=y + g.height // 2)
    d.sync()
    return w, g, x, y


cmd = sys.argv[1]
if cmd == 'find':
    got = mc_window()
    if not got: print('NO WINDOW'); sys.exit(2)
    w, g = got
    t = w.translate_coords(root, 0, 0)
    print('id=0x%x name=%r %dx%d at %d,%d' % (w.id, w.get_wm_name(), g.width, g.height, -t.x, -t.y))
elif cmd == 'focus':
    focus(); print('focused')
elif cmd == 'chat':
    focus(); time.sleep(0.25)
    key('t'); time.sleep(0.45)
    type_text(sys.argv[2]); time.sleep(0.2)
    key('Return'); time.sleep(0.2)
    print('sent:', sys.argv[2])
elif cmd == 'key':
    focus(); time.sleep(0.2)
    for n in sys.argv[2:]:
        key(n); time.sleep(0.15)
    print('keys:', sys.argv[2:])
elif cmd == 'keyhold':
    focus(); time.sleep(0.2)
    key(sys.argv[2], hold=float(sys.argv[3]))
    print('held', sys.argv[2], sys.argv[3])
elif cmd == 'click':
    focus(); time.sleep(0.2)
    btn = int(sys.argv[2]); n = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    for _ in range(n):
        xtest.fake_input(d, X.ButtonPress, btn); d.sync(); time.sleep(0.06)
        xtest.fake_input(d, X.ButtonRelease, btn); d.sync(); time.sleep(0.25)
    print('clicked', btn, n)
elif cmd == 'move':
    focus(); time.sleep(0.2)
    dx, dy = int(sys.argv[2]), int(sys.argv[3])
    xtest.fake_input(d, X.MotionNotify, detail=True, x=dx, y=dy); d.sync()
    print('moved', dx, dy)
elif cmd == 'shot':
    got = mc_window()
    if not got: print('NO WINDOW'); sys.exit(2)
    w, g = got
    out = sys.argv[2]
    subprocess.run(['xwd', '-id', hex(w.id), '-out', '/tmp/_mc.xwd'], env=SHOTENV, check=True)
    subprocess.run(['convert', '/tmp/_mc.xwd', out], env=SHOTENV, check=True)
    print('shot ->', out)
elif cmd == 'sleep':
    time.sleep(float(sys.argv[2]))
else:
    print(__doc__)
