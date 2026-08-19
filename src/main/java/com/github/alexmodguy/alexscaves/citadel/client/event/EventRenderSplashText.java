package com.github.alexmodguy.alexscaves.citadel.client.event;

import net.minecraft.client.gui.GuiGraphics;
import com.github.alexmodguy.alexscaves.citadel.CitadelEvent;

public class EventRenderSplashText extends CitadelEvent {
    private String splashText;

    private final GuiGraphics guiGraphics;
    private final float partialTicks;

    public EventRenderSplashText(String splashText, GuiGraphics guiGraphics, float partialTicks) {
        this.splashText = splashText;
        this.guiGraphics = guiGraphics;
        this.partialTicks = partialTicks;
    }

    public String getSplashText() {
        return splashText;
    }

    public void setSplashText(String splashText) {
        this.splashText = splashText;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public static class Pre extends EventRenderSplashText {

        private int splashTextColor;

        public Pre(String splashText, GuiGraphics guiGraphics, float partialTicks, int splashTextColor) {
            super(splashText, guiGraphics, partialTicks);
            this.splashTextColor = splashTextColor;
        }

        public int getSplashTextColor() {
            return splashTextColor;
        }

        public void setSplashTextColor(int splashTextColor) {
            this.splashTextColor = splashTextColor;
        }

        //? if forge && >=1.21.6
        /*public static final net.minecraftforge.eventbus.api.bus.EventBus<Pre> BUS = net.minecraftforge.eventbus.api.bus.EventBus.create(Pre.class);*/

        /** @see EventPosePlayerHand#post */
        public static void post(Pre event) {
            //? if forge && >=1.21.6
            /*BUS.post(event);*/
            //? if !forge || <1.21.6
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        }
    }

    public static class Post extends EventRenderSplashText {

        public Post(String splashText, GuiGraphics guiGraphics, float partialTicks) {
            super(splashText, guiGraphics, partialTicks);
        }

        //? if forge && >=1.21.6
        /*public static final net.minecraftforge.eventbus.api.bus.EventBus<Post> BUS = net.minecraftforge.eventbus.api.bus.EventBus.create(Post.class);*/

        /** @see EventPosePlayerHand#post */
        public static void post(Post event) {
            //? if forge && >=1.21.6
            /*BUS.post(event);*/
            //? if !forge || <1.21.6
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        }
    }

}
