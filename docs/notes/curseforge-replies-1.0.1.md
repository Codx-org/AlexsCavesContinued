# CurseForge replies for 1.0.1 — copy/paste, one per comment

Project `1645389`. Comment ids from `https://www.curseforge.com/api/v1/mods/1645389/comments`.
Post as replies to the comment named in each heading.

---

## → EinfachZ0ckt, comment `8329526` (the Alex's Mobs crash, with the correct diagnosis)

Your diagnosis was exactly right, and it saved me a good chunk of an evening. Thank you for
writing it up properly instead of just saying "it crashes".

It's fixed in 1.0.1, which is up now. You were right about the @Redirect collision on
getTeamColor, and there were five more spots with the same problem that hadn't crashed for you
yet, including two that fire on the server side before any rendering happens. All six are
rewritten as @WrapOperation / @ModifyExpressionValue, which stack instead of fighting over the
same instruction, so both mods' hooks now run.

I tested it both ways round on 26.1.2: with 1.0.0 I get your crash word for word, and with 1.0.1
it boots to the title screen with Alex's Mobs Continued installed. Nothing needs updating on the
Alex's Mobs side, just grab 1.0.1 here.

---

## → ghostchow, comment `8326521` (same crash, no log)

This is fixed in 1.0.1, just uploaded. Update Alex's Caves Continued and they'll run together.
You don't need to update Alex's Mobs Continued and you don't need a fresh client, your existing
world is fine.

Short version of what it was: Alex's Caves bundles part of Citadel, and a few of those bundled
bits patched Minecraft in a way only one mod is allowed to do at a time. Alex's Mobs patches the
same spot. Whichever loaded second failed, and that kind of failure takes the whole thing down,
which is why you got the error twice, once for each mod.

---

## → freopt, comment `8328111` (Citadel + BadOptimizations + Alex's Mobs)

All three of those are one bug, and it's fixed in 1.0.1.

Alex's Caves bundles the parts of Citadel it uses. Six of those bundled hooks patched Minecraft
in a way that only one mod can do per spot, so if you also had the real Citadel installed, one of
the two lost and crashed the game at launch. Alex's Mobs Continued depends on Citadel, so it hit
the same wall from the other direction. Those six are rewritten to a style that stacks, and I
booted 1.0.1 against real Citadel 2.6.3 to check, so Rats Unofficial should be happy now.

BadOptimizations I could not reproduce at all. I ran 2.4.1 with this build on 26.1 and it loads
clean, and BadOptimizations is written so its patches skip silently instead of crashing, so it
can't have been the thing stopping your launch by itself. I'm fairly sure it was the Citadel
clash showing up again. If something is still off after 1.0.1, it would be visual rather than a
crash, so a screenshot would tell me more than a log would.

---

## → user_qatf5lyvbsphj3up, comment `8327786` (can this replace Alex's Caves)

Yes, it's a drop-in replacement. Same mod id, same namespace, same registry names, same tags as
the original. Take Alex's Caves out, put this in, and your world keeps every block, item, mob and
biome exactly where it was.

Two things to have ready before you launch: CodxLib is required (grab the file for your exact
Minecraft version and loader), and both this and CodxLib need to be on the server as well as the
client if you're playing multiplayer.

---

## → freopt, comment `8327961` (Dimensions of Alex's Caves)

Noted, and it's a fair ask. It's someone else's mod though, so it's a separate project rather
than something I can fold into this one, and it would need the same version by version walk this
port took. Not saying no. Just not next.


---

## → user_gz5mnw7zwvxu9rgb, comment `8330289` (the acid crash on world load, NeoForge 26.1.2.97)

Cheers for pasting the whole report, the top line of it is the whole answer. Fixed in 1.0.1.

Not your modlist, it's the NeoForge build. Between 26.1.2.87 and .97 they changed how fluids are
tracked, and on .97 the call I used to check whether your head is in acid just throws for anything
that isn't water or lava. It runs every frame for the fog, so it blew up on the first frame drawn,
which is why the world never appeared. You weren't anywhere near acid.

Both builds are out there so I can't just target one. It works the depth out from the blocks itself
now and never asks NeoForge.

Also spotted Citadel and Alex's Mobs in there. 1.0.1 fixes a launch clash with those too.
