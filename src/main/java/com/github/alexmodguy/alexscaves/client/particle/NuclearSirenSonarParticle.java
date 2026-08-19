package com.github.alexmodguy.alexscaves.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

public class NuclearSirenSonarParticle extends ACQuadParticle {

    private float xRot;
    private float yRot;
    private float fadeR;
    private float fadeG;
    private float fadeB;
    protected NuclearSirenSonarParticle(ClientLevel world, double x, double y, double z, float xRot, float yRot) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.setSize(0.4F, 0.4F);
        this.setColor(1F, 1F, 1F);
        this.lifetime = 8;
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.quadSize = 0.4F;
        this.friction = 1F;
        this.xRot = xRot;
        this.yRot = yRot;
    }

    public void setFadeColor(int i) {
        this.fadeR = (float) ((i & 16711680) >> 16) / 255.0F;
        this.fadeG = (float) ((i & '\uff00') >> 8) / 255.0F;
        this.fadeB = (float) ((i & 255) >> 0) / 255.0F;
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        float f = ((float) this.age - (float) (this.lifetime / 2)) / (float) this.lifetime;
        float f1 = this.age / (float) this.lifetime;
        float f2 = 1F - 0.1F * f1;
        friction = 1F - 0.65F * f1;
        if (this.age > this.lifetime / 2) {
            this.setAlpha(1.0F - f * 2F);
        }
        this.rCol += (fadeR - this.rCol) * 0.1F;
        this.gCol += (fadeG - this.gCol) * 0.1F;
        this.bCol += (fadeB - this.bCol) * 0.1F;
        Vec3 motionVec = new Vec3(0, 0, 0.055F).xRot((float) Math.toRadians(xRot)).yRot(-(float) Math.toRadians(yRot));
        this.xd += motionVec.x * f2;
        this.yd += motionVec.y * f2;
        this.zd += motionVec.z * f2;
        this.hasPhysics = this.age > 3;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
            this.xd *= (double) this.friction;
            this.yd *= (double) this.friction;
            this.zd *= (double) this.friction;
        }
    }
    @Override
    protected ACParticleLayer acLayer() {
        return ACParticleLayer.TRANSLUCENT;
    }

    public int getLightColor(float partialTicks) {
        return 240;
    }

    public float getQuadSize(float scaleFactor) {
        return this.quadSize * Mth.clamp(((float) this.age + scaleFactor) / (float) this.lifetime, 0.0F, 1.0F) * 2.0F;
    }

    @Override
    protected List<Consumer<Quaternionf>> acQuadRotations() {
        return List.of(
                (quaternionf) -> quaternionf.rotateY(-(float) Math.toRadians(yRot)).rotateX(-(float) Math.toRadians(xRot)),
                (quaternionf) -> quaternionf.rotateY(-(float) Math.PI - (float) Math.toRadians(yRot)).rotateX((float) Math.toRadians(xRot))
        );
    }


    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            NuclearSirenSonarParticle particle = new NuclearSirenSonarParticle(worldIn, x, y, z, (float) xSpeed, (float) ySpeed);
            particle.pickSprite(spriteSet);
            particle.setFadeColor(0X00EE00);
            return particle;
        }
    }
}
