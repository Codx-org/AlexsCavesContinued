package com.github.alexmodguy.alexscaves.server.compat;

// 26.2 deleted net.minecraft.util.Tuple. It was a two-field mutable pair with no behaviour and no
// vanilla successor — nothing in 26.2 replaces it, vanilla's own users were rewritten to records or
// to com.mojang.datafixers.util.Pair one at a time.
//
// Sixteen sites in this tree name it: the drain block's two flood-fill queues and Citadel's
// raycoms pathfinder (a random-direction pair and a corner pair on the navigate API). Every one of
// them uses only getA()/getB(), so the copy below is the whole of what was lost, and a single
// `replacements.string` rule on the qualified name re-points the two imports and the two
// fully-qualified uses at it.
//
// Vendored rather than swapped for Pair deliberately: Pair is immutable and spells its accessors
// getFirst()/getSecond(), so translating would touch every call site on every one of the 58 nodes
// to buy nothing.
//? if >=26.2 {
/*public class Tuple<A, B> {

    private A a;
    private B b;

    public Tuple(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return this.a;
    }

    public B getB() {
        return this.b;
    }

    public void setA(A a) {
        this.a = a;
    }

    public void setB(B b) {
        this.b = b;
    }
}
*///?}
