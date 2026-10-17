package com.github.alexmodguy.alexscaves.server.entity.util;

/**
 * A 26.3 {@code InterpolationHandler} that only records the target and never moves the entity.
 *
 * <p>26.3 turned {@code InterpolationHandler} from a class into an interface. Through 1.21.11 the
 * sixteen entities listed on {@link ACInterpolationCapture} subclassed it anonymously and overrode
 * {@code interpolateTo} <em>without</em> calling {@code super}, which captured the target into their
 * own lerp fields and suppressed vanilla's interpolation entirely. That is structurally impossible
 * from 26.3 — there is no constructor to call and no implementation to inherit — so the same
 * intent is expressed by implementing the interface directly and returning the
 * do-nothing answers for everything except the capture.
 *
 * <p>Those answers are vanilla's own: this mirrors {@code InterpolationHandler$NoOpInterpolationHandler},
 * which is what {@code Entity#createInterpolationHandler} returns by default. In particular
 * {@code target()} returning {@code null} is deliberate and safe — {@code Entity#getClientPosition}
 * null-checks it and falls back to the entity's real position, and
 * {@code Entity#getClientPositionAndRotation} routes it through {@code requireNonNullElseGet}.
 * {@code interpolationTracker()} is left to its default, which is {@code InterpolationTracker.NO_OP}.
 *
 * <p>{@code Entity#createInterpolationHandler} is called from {@code Entity}'s own constructor, so
 * this object is built before the subclass's field initialisers run. It therefore reads no entity
 * state at construction time; the captured lambda only touches fields when a packet later arrives.
 *
 * <p>The whole class is gated because every type it names is 26.3-only. Below 26.3 nothing
 * references it and it compiles away to an empty shell.
 */
//? if >=26.3 {
/*public final class ACLerpInterpolationHandler implements net.minecraft.world.entity.InterpolationHandler {

    private final net.minecraft.world.entity.Entity entity;
    private final ACInterpolationCapture capture;

    public ACLerpInterpolationHandler(net.minecraft.world.entity.Entity entity, ACInterpolationCapture capture) {
        this.entity = entity;
        this.capture = capture;
    }

    // Null means "no interpolated target"; both callers in Entity guard for it.
    @Override
    public net.minecraft.core.PositionAndRotation target() {
        return null;
    }

    // The only method that does anything. endPosition() is the final target of the path, i.e.
    // exactly the Vec3 the pre-26.3 interpolateTo(Vec3, float, float) was handed.
    //
    // path is NULLABLE, and a rotation-only update is the common case that supplies null --
    // ClientPacketListener#handleMoveEntity calls Entity#moveOrInterpolateTo(float, float) when the
    // packet has a rotation and no position, and that overload passes aconst_null straight down.
    // Vanilla's own fallback in moveOrInterpolateTo null-checks it before calling endPosition(); so
    // must we, or every ClientboundMoveEntityPacket$Rot for one of these sixteen entities is an NPE
    // on the render thread, which the client reports as "Failed to handle packet ... disconnecting"
    // and turns into a Network Protocol Error disconnect.
    //
    // Nothing like it existed below 26.3: 26.2's handleMoveEntity decoded the position codec even
    // when the packet carried no position -- a zero delta against the current base -- and always
    // called the Vec3 overload, so the capture was handed the last known position with the new
    // rotation. The entity's own position is that same value, so passing it here reproduces the old
    // behaviour exactly rather than dropping the update.
    @Override
    public boolean interpolateTo(net.minecraft.world.entity.PositionPath path, float yRot, float xRot, boolean relative) {
        net.minecraft.world.phys.Vec3 end = path == null ? this.entity.position() : path.endPosition();
        this.capture.captureInterpolationTarget(end, yRot, xRot);
        return false;
    }

    @Override
    public void interpolate() {
    }

    @Override
    public void applyPredictedMovement(net.minecraft.world.phys.Vec3 movement) {
    }

    @Override
    public boolean hasActiveInterpolation() {
        return false;
    }

    @Override
    public void cancel() {
    }
}
*///?} else {
public final class ACLerpInterpolationHandler {
}
//?}
