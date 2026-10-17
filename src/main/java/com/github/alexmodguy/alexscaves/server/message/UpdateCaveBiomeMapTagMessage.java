package com.github.alexmodguy.alexscaves.server.message;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.citadel.server.message.PacketBufferUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
public class UpdateCaveBiomeMapTagMessage {

    private UUID userUUID;
    private UUID caveBiomeMapUUID;
    private CompoundTag tag;

    public UpdateCaveBiomeMapTagMessage(UUID userUUID, UUID caveBiomeMapUUID, CompoundTag tag) {
        this.userUUID = userUUID;
        this.caveBiomeMapUUID = caveBiomeMapUUID;
        this.tag = tag;
    }


    public static UpdateCaveBiomeMapTagMessage read(FriendlyByteBuf buf) {
        return new UpdateCaveBiomeMapTagMessage(buf.readUUID(), buf.readUUID(), PacketBufferUtils.readTag(buf));
    }

    public static void write(UpdateCaveBiomeMapTagMessage message, FriendlyByteBuf buf) {
        buf.writeUUID(message.userUUID);
        buf.writeUUID(message.caveBiomeMapUUID);
        PacketBufferUtils.writeTag(buf, message.tag);
    }

    public static void handle(UpdateCaveBiomeMapTagMessage message, ACNetworkContext context) {
        context.setPacketHandled(true);
        Player playerSided = context.getSender();
        if (context.isClientSide()) {
            playerSided = AlexsCaves.PROXY.getClientSidePlayer();
        }
        if(playerSided != null){
            Player player = playerSided.level().getPlayerByUUID(message.userUUID);
            if (player != null) {
                ItemStack set = null;
                java.util.List<ItemStack> carried = ACCompat.inventoryItems(player.getInventory());
                for(int i = 0; i < carried.size(); i++){
                    ItemStack itemStack = carried.get(i);
                    if(itemStack.is(ACItemRegistry.CAVE_MAP.get()) && ACCompat.getTag(itemStack) != null){
                        CompoundTag tag = ACCompat.getOrCreateTag(itemStack);
                        if(tag.contains("MapUUID") && message.caveBiomeMapUUID.equals(ACCompat.getUUID(tag, "MapUUID"))){
                            set = itemStack;
                            break;
                        }
                    }
                }
                if(set != null){
                    ACCompat.setTag(set, message.tag);
                }
            }
        }
    }

}
