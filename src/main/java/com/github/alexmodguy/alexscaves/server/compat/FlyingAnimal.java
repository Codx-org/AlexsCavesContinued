package com.github.alexmodguy.alexscaves.server.compat;

// 26.2 deleted net.minecraft.world.entity.animal.FlyingAnimal — a one-method marker interface
// (`boolean isFlying()`) that bees, parrots and allays implemented. It has no successor: Bee no
// longer implements anything in its place (javap'd), and no interface in the new
// world.entity.animal tree carries the method.
//
// The interface itself is vendored because this mod's Subterranodon *implements* it and reads its
// own isFlying() in a dozen places, so the declaration has to keep existing. What CANNOT be
// recovered by vendoring is the other half of what it was for: three `instanceof` sites ask "is
// that thing over there a flying animal", and a vendored interface answers no for every vanilla
// mob. ACFlyingAnimals is where that question is asked instead — see it for the substitute test.
//? if >=26.2 {
/*public interface FlyingAnimal {

    boolean isFlying();
}
*///?}
