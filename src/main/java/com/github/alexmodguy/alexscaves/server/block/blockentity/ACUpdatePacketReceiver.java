package com.github.alexmodguy.alexscaves.server.block.blockentity;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

/**
 * Declares {@code BlockEntity#onDataPacket} as something the mod owns, so that the nine block
 * entities which override it can be dispatched to on a loader that does not have it.
 *
 * <p>{@code onDataPacket} is a <b>Forge patch</b>, not vanilla: Forge and NeoForge rewrite
 * {@code ClientPacketListener#handleBlockEntityData} to call it in place of the vanilla
 * {@code BlockEntity#load(tag)}, giving a block entity a hook that fires only for an update packet
 * and can read a partial tag. Vanilla has no such split — it feeds the update tag straight to
 * {@code load}, which is the same method a full chunk load uses.
 *
 * <p>That difference matters here rather than being cosmetic. Every one of the nine implementations
 * reads only the handful of keys its {@code getUpdateTag} writes and deliberately does <em>not</em>
 * delegate to {@code load}; letting vanilla run {@code load} over that partial tag instead would
 * read the absent keys as zero and blank the client's copy of everything the update tag omits. So
 * the Fabric dispatcher replaces the vanilla call rather than running alongside it — see
 * {@code mixin.fabric.client.ClientPacketListenerMixin}.
 *
 * <p>The method is declared with the loader patch's exact name and signature on purpose. On Forge
 * and NeoForge the inherited patch then already satisfies it, so implementing this interface costs
 * those loaders nothing and the nine {@code @Override}s keep meaning what they always did; on
 * Fabric the very same {@code @Override}s are satisfied by this declaration instead. The
 * {@code implements} clauses are therefore unconditional and no block entity needed a gate.
 *
 * <p><b>⚠️ But "the loader patch's exact signature" is four different signatures, so the METHOD is
 * gated even though the interface is not.</b> An unconditional declaration is only ever right
 * within one band of the patch it mirrors, and this patch moved twice and then forked:
 *
 * <ul>
 *   <li>{@code <1.20.5} — {@code (Connection, ClientboundBlockEntityDataPacket)}.</li>
 *   <li>{@code >=1.20.5 && <1.21.6} — a trailing {@code HolderLookup.Provider}, from the same sweep
 *       that threaded one through every block-entity load/save path.</li>
 *   <li>{@code >=1.21.6} — the packet argument became a {@code ValueInput}, and here the two
 *       loaders <em>disagree</em>: NeoForge's {@code IBlockEntityExtension} takes
 *       {@code (Connection, ValueInput)} and expects the lookup to be read off the input, while
 *       Forge's {@code IForgeBlockEntity} kept the provider it added in 1.20.5 and takes
 *       {@code (Connection, ValueInput, Provider)}. Both read out of each loader's own universal
 *       jar; neither is derivable from the other.</li>
 *   <li>Fabric {@code >=1.21.6} — neither {@code !mc216-be-datapacket-*} rule fires (both are
 *       loader-scoped), so the nine implementations keep the original two-argument declaration and
 *       so does this. That is not a stopgap: on Fabric nothing inherits {@code onDataPacket} from
 *       anywhere, the only caller is this mod's own dispatcher, and it has the packet in hand.</li>
 * </ul>
 *
 * <p>The arms below therefore mirror the {@code !mc205-be-datapacket} and
 * {@code !mc216-be-datapacket-nf}/{@code -fg} replacement rules exactly, argument name included —
 * <b>those rules rewrite the nine {@code public void … {} declarations and cannot reach this one</b>,
 * because they are anchored on a {@code public} modifier and a trailing brace that an interface
 * method has neither of. Change a rule and this chain has to move with it.
 */
public interface ACUpdatePacketReceiver {

    //? if <1.20.5 {
    void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet);
    //?} elif <1.21.6 {
    /*void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, net.minecraft.core.HolderLookup.Provider acRegistries);
    *///?} elif neoforge {
    /*void onDataPacket(Connection net, net.minecraft.world.level.storage.ValueInput acIn_packet);
    *///?} elif forge {
    /*void onDataPacket(Connection net, net.minecraft.world.level.storage.ValueInput acIn_packet, net.minecraft.core.HolderLookup.Provider acRegistries);
    *///?} else {
    /*void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet);
    *///?}
}
