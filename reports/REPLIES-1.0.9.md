# Replies for the 1.0.9 wave

Paste-ready, one per report. Fixed in **1.0.9**, up on Modrinth and CurseForge.

---

## Report 1 — boss dino crashes when damaged

Thanks, this one was a real bug and it's fixed in 1.0.9.

The Luxtructosaurus (and six other big mobs) are built out of several hitbox pieces rather than one
box. Those pieces never got an entity ID on the client side. That was harmless on every version up
to 26.1.2, but in 26.2 Mojang made reading a missing ID throw an error instead of returning 0 — so
the first hit that landed on one of the extra hitboxes took the game down. Creative dodged it
because you weren't landing hits on the same path.

Fixed by giving the hitbox pieces proper IDs, the same way vanilla does it for the Ender Dragon.
Same fix covers Tremorzilla, Hullbreaker, Corrodent, Magnetron, Quarry Smasher and the Gossamer Worm,
which all had it too.

---

## Report 2 — 1.21.5 Fabric: underwater flashing, diving helmet, Skin Layers, pewen boat crash

Thanks for the detailed list. Two of the four are fixed in 1.0.9, and I need a bit more on the other
two.

**Underwater flashing / screen filling with biome fog** — fixed. There's a bit of code that stretches
the fog distance inside the mod's own fog volumes, and it was reading back the fog distance it had
already written the frame before. So every frame multiplied the last frame's value and the fog
collapsed onto the camera within a second or two, which is why it reads as flashing and then as a
flat sheet of the biome colour. It was only guarded against on 1.21.6+; the version where the
behaviour actually changed is 1.21.2, so 1.21.2 through 1.21.5 got hit. Toxic Caves, Forlorn Hollows
and Abyssal Chasm being the worst fits — those are the strongest-tinted volumes.

**Pewen boat crash** — fixed, thanks for the log, it pointed straight at it. Two things were wrong at
once: the boat renderer was still typed against the old boat class (1.21.2 moved chest boats onto a
separate branch), and the boats' shared interface was relying on a vanilla method to fill in one of
its methods, which works on Forge/NeoForge but not on Fabric because the name is different at
runtime there. Both fixed.

**Diving helmet sides** — I went over the model and the texture sheet and couldn't find anything
wrong: no UVs off the sheet, no overlapping regions, and the only fully transparent faces are two
that point inward and are never visible. Could you send a screenshot? A shot of the helmet worn from
the angle where it looks wrong would settle it quickly.

**Skin Layers 3D overlap** — this one I can't do anything with until I can reproduce it against that
mod; it's on the list. If you can say which of its layer settings you're running it'd help.

---

## Report 3 — no Alex's Caves enchantments at the enchanting table

Good catch, and the book detail is what identified it — thank you.

From 1.21.2 onwards Minecraft decides whether an item can be enchanted purely from a data component
on the item. Forge and NeoForge builds had code that stamps that component on the mod's gear;
**Fabric didn't**, so on Fabric all thirteen enchantable Alex's Caves items came out of the table with
nothing offered. Books were unaffected because the game special-cases books and never checks the
component for them, which is exactly why you could get the cave enchantments on a book but not on the
gear itself.

Fixed in 1.0.9 for all Fabric versions.

---

## Report 4 — giant sweetberry x-ray over ice cream

Thanks for the report — I couldn't reproduce this one, so I'd like a bit more from you before I go
further.

I built four test setups on 26.2 (open-air walls, sealed rooms, ice cream vs a plain control block,
berries on top, several camera angles) and everything rendered solid. I also traced every path that
could hide a face, and all of them err the other way — those blocks tell the game to draw *more*
faces than strictly needed, never fewer, so they shouldn't be able to open a hole.

If you still see it on 1.0.9, could you send:
- a screenshot of it happening,
- which Minecraft version and loader,
- and whether it survives pressing F3+A (chunk reload) — that separates a real render bug from a
  chunk that just baked wrong once.

Also worth checking whether you've got Sodium/Embeddium or a culling mod (More Culling, Entity
Culling) installed — this shape of bug is very often those interacting with a non-cube block.

One thing did come out of looking at it: the giant sweetberry was telling the game it was a full
solid cube, when the model is a berry on a stem. That's now corrected in 1.0.9. It's not the same as
what you saw, but it was wrong.

---

## Report 5 — Tremorzilla crash when interacting after scaling it

Fixed in 1.0.9, and it wasn't really about the scaling.

Tremorzilla is made of several hitbox pieces rather than one big box, and those pieces never got an
entity ID on the client. That was fine on every version before 26.2, but 26.2 changed reading a
missing ID from "return 0" into a hard error — so any interact that landed on one of the extra
hitboxes crashed. Shrinking it with `/attribute` just made those pieces much easier to hit, which is
why it looked like the scaling caused it. Same bug was reported separately against the
Luxtructosaurus, and it turned out to be the same fix. Six other mobs had it too.

On the other rendering problems on 26.2 — I'd like to chase those, but I need specifics. If you can
list what you're seeing (and ideally a screenshot or two), I'll take them one at a time.
