# 1.0.1

Bug fixes. Two of them stopped the game from starting, so this is worth updating for even if
nothing below sounds familiar.

## It runs alongside Alex's Mobs Continued and Citadel now

If you installed this next to Alex's Mobs Continued, both mods failed at launch with
`MixinTransformerError` and the game never reached the menu. Same story if you had the original
Citadel installed for something else, Rats for instance.

EinfachZ0ckt worked out the cause and posted it, which saved me a lot of time, so: Alex's Caves
bundles the parts of Citadel it needs, and six of those bundled hooks patched Minecraft in a way
that only one mod is allowed to do per spot. Whichever mod got there second failed, and a mixin
that fails hard takes the whole class down with it, which is why both mods died and not just one.
It was never really Alex's Mobs against Alex's Caves. Alex's Mobs is just the mod most people have
that brings Citadel along.

Those six hooks are rewritten to a kind that stacks instead of competing. I tested it both ways
round: on 1.0.0 the game dies at launch exactly as reported, and on this build it goes straight to
the title screen, with Alex's Mobs Continued and with real Citadel 2.6.3. Nothing needs updating on
the Alex's Mobs side.

For the same reason, the "incompatible with BadOptimizations" report is almost certainly this bug
too. I ran BadOptimizations 2.4.1 with this build and it loads fine, and BadOptimizations is
written so its patches skip quietly rather than crash, so it can't have been the thing killing your
launch on its own.

## The magnetic caves and the smithing table no longer crash

Walking up to a Teletor crashed the client on 1.21.5 and up. The model was undoing a transform it
had never applied, which older Minecraft versions shrugged off and newer ones throw on. It happened
for any Teletor with a trail or a levitating weapon, so in practice, all of them.

There was a second, separate magnetic caves crash underneath that one, in the lightning particles
the magnets and tesla bulbs give off. They asked Minecraft to trace a line through the world without
saying who was tracing it, which was allowed until 1.21.2 and has thrown ever since. Nothing in the
code looked wrong; the method it calls still takes exactly the same arguments.

Opening a smithing table crashed the client on 1.21.9 and up. The mod hooks entity rendering to do
its own effects, and the little armour stand preview in that screen is not a real entity, so the
hook had nothing to look at and fell over. Anvils and enchanting tables were fine because they
don't render one.

## Acid no longer crashes the game on newer NeoForge

Some people couldn't get into a world at all on NeoForge for 26.1.2, with a crash naming acid.
NeoForge changed how it handles modded fluids partway through 26.1.2 without the Minecraft version
changing, so two builds of the same NeoForge are out there behaving differently, and one of them
refuses to answer the question the mod was asking about acid. There is no way to pick the right
answer at build time when both are called 26.1.2.

So the mod stops asking. It now works out for itself how deep you are in acid or soda by looking at
the blocks around you, using the part of Minecraft that hasn't changed in any version it supports.
That is the same code on all 58 builds now instead of two versions of it, and as a side effect it
notices fluids from other mods again.

## Soda and acid behave like liquids again

You can swim in them. Boats float. Fish don't suffocate the moment they enter soda.

Radgills, lanternfish and sea pigs still die in it, and that is deliberate. They're the acid and
water mobs, soda isn't their fluid.

## The compendium and the spelunkery table work

The Cave Compendium opened as a blank page that swallowed your clicks. Four separate things were
wrong with it and each one hid the next, which is why it looked completely dead rather than partly
broken. The worst of them turned out to affect the whole mod: Minecraft 1.21.6 stopped filling in a
missing transparency value on text, and every piece of text in Alex's Caves was written without one.
On 1.21.6 and up that draws nothing at all. The spelunkery table was blank for exactly the same
reason, on top of its buttons not registering clicks.

That was 22 of the 58 builds drawing invisible text and never logging a word about it.

## Other things that did nothing when you used them

The remote detonator never armed. Linking it to a bomb wrote the position onto a copy of the item
that was thrown away, so it always thought it was unlinked. The occult gem had the identical bug and
never bound to a beholder.

Burrowing and seeking arrows fired plain arrows. The mod's own arrow was being asked for through a
method Minecraft changed the shape of, so the game just used a normal one.

Rusty barrels couldn't be opened, same cause.

Most primordial caves mobs never spawned. They spawn on dirt, and 26.1 quietly took several blocks
out of Minecraft's dirt list, including the ones down there. The mod carries its own list now.

## Visual

On 26.2, things showed through solid rock. Ambersol shine came through stone walls and the
primordial caves were full of white starbursts visible from anywhere. Minecraft 26.2 reversed the
direction of its depth test and the mod was still comparing the old way. It was easy to prove: I
put the bug back on purpose and the screenshots matched the reports.

Frogs in the primordial caves were a magenta and black checkerboard on 1.21.5 and up. Minecraft
moved frog skins into a data file in that version and the mod's entry pointed one folder short of
where its texture actually is.

Some creative tab entries showed their internal name instead of a real one — pewen and thornwood
doors and signs, and the bioluminescent torch. Minecraft 1.21.2 stopped giving an item its block's
name automatically and made it something an item has to ask for; those seven never asked. Six other
names were simply missing from the language file, including the polarity armour trim template. All
of it is checked automatically now, 455 names, so it can't drift again.

The primordial, hazmat, diving and gingerbread armour trims render again on 1.21.4 and up. The trim
textures had to move, twice, and the mod was also re-listing sixteen vanilla trims at paths that no
longer exist.

## Not fixed

Two things were reported that I couldn't turn into a bug.

The raycat glow looks identical on 1.20.1, 1.21.5 and 26.2 when I capture it side by side on the
same frame, so whatever was reported there, I'm not seeing it. If it's still wrong for you, a
screenshot and your Minecraft version would help.

The "star artifacts" in primordial caves are the Ambersol's shine, and it renders the same here as
it does in the original on 1.20.1. It's meant to look like that. The see-through part of it was
real, and that's fixed above.

## Testing

All 58 files rebuilt from one pass and checked the same way as 1.0.0: every mixin in every file
resolved against the actual Minecraft it's built for, 16433 of them, before anything was booted. The
detonator, the arrows, the compendium, the spelunkery table and the smithing table were each
verified in a real world rather than just in the code.

Still LGPL-3.0, still AlexModGuy's mod underneath. CodxLib 1.3.6 or newer is required, same as
before.
