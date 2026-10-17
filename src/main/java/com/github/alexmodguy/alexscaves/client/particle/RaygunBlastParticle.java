package com.github.alexmodguy.alexscaves.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

public class RaygunBlastParticle extends ACQuadParticle {

    private Direction direction;

    private float randomRot = 0;

    protected RaygunBlastParticle(ClientLevel world, double x, double y, double z, Direction direction) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.direction = direction;
        this.hasPhysics = false;
        this.setSize(1.0F, 1.0F);
        this.setColor(1F, 1F, 1F);
        this.lifetime = world.getRandom().nextInt(20) + 20;
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.randomRot = (float) (Math.PI * 2F * world.getRandom().nextFloat());
        this.quadSize = 0.2F + world.getRandom().nextFloat() * 0.4F;
        this.friction = 0F;
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        float f = ((float) this.age - (float) (this.lifetime / 2)) / (float) this.lifetime;
        float f1 = this.age / (float) this.lifetime;
        float f2 = 1F - 0.1F * f1;
        friction = 1F - 0.65F * f1;
        if (this.age > lifetime / 2) {
            this.setAlpha(1.0F - f * 2F);
        }
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        BlockPos connectedTo = BlockPos.containing(this.x + direction.getStepX() * -0.1F, this.y + direction.getStepY() * -0.1F, this.z + direction.getStepZ() * -0.1F);
        BlockState state = level.getBlockState(connectedTo);
        if (this.age++ >= this.lifetime || state.isAir() || !state.isFaceSturdy(level, connectedTo, direction.getOpposite())) {
            this.remove();
        }else if(random.nextFloat() < 0.5F && this.age < this.lifetime / 2){
            this.level.addParticle(ParticleTypes.SMOKE.getType(), x, y, z, 0, 0, 0);
        }
    }

    @Override
    protected ACParticleLayer acLayer() {
        return ACParticleLayer.TRANSLUCENT;
    }

    @Override
    protected List<Consumer<Quaternionf>> acQuadRotations() {
        return List.of(
                (quaternionf) -> {
                    quaternionf.mul(direction.getRotation());
                    quaternionf.rotateX(-(float) Math.PI * 0.5F);
                    quaternionf.rotateZ(randomRot);
                },
                (quaternionf) -> {
                    quaternionf.mul(direction.getRotation());
                    quaternionf.rotateX((float) Math.PI - (float) Math.PI * 0.5F);
                    quaternionf.rotateZ(randomRot);
                }
        );
    }


    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            Direction direction = Direction.from3DDataValue((int)xSpeed);
            RaygunBlastParticle particle = new RaygunBlastParticle(worldIn, x, y, z, direction);
            particle.pickSprite(spriteSet);
            return particle;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class TremorzillaFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public TremorzillaFactory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            Direction direction = Direction.from3DDataValue((int)xSpeed);
            RaygunBlastParticle particle = new RaygunBlastParticle(worldIn, x, y, z, direction);
            particle.pickSprite(spriteSet);
            particle.quadSize = 1.0F + worldIn.getRandom().nextFloat() * 0.5F;
            particle.lifetime = 60 + worldIn.getRandom().nextInt(20);
            return particle;
        }
    }
}
