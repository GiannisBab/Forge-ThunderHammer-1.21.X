package net.giannisbab.minecraftmod.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

public class ThunderHammer extends Item {
    private static final float RAY_DISTANCE = 150.0F;

    public ThunderHammer(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide) {
            // Start a thunderstorm
            if (world instanceof ServerLevel serverWorld) {
                serverWorld.setWeatherParameters(0, 6000, true, true);
            }

            HitResult hitResult = getTargetHitResult(world, player, RAY_DISTANCE);

            if (hitResult != null) {
                switch (hitResult.getType()) {
                    case ENTITY -> {
                        EntityHitResult entityHit = (EntityHitResult) hitResult;
                        Vec3 location = entityHit.getLocation();
                        BlockPos entityPos = BlockPos.containing(location);
                        spawnLightningAtSky(world, entityPos);
                    }
                    case BLOCK -> {
                        BlockHitResult blockHit = (BlockHitResult) hitResult;
                        BlockPos blockPos = blockHit.getBlockPos();
                        spawnLightningAtSky(world, blockPos);
                    }
                    default -> {}
                }
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    // Returns the closest hit result between blocks and entities
    private HitResult getTargetHitResult(Level world, Player player, double maxDistance) {
        HitResult blockResult = player.pick(maxDistance, 0.0F, false);
        double blockDist = Double.MAX_VALUE;
        if (blockResult != null) {
            blockDist = blockResult.getLocation().distanceToSqr(player.getEyePosition(1.0F));
        }

        Vec3 startVec = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endVec = startVec.add(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance);

        AABB boundingBox = player.getBoundingBox()
                .expandTowards(lookVec.scale(maxDistance))
                .inflate(1.0D);

        EntityHitResult entityResult = ProjectileUtil.getEntityHitResult(
                world,
                player,
                startVec,
                endVec,
                boundingBox,
                e -> e instanceof LivingEntity && e != player
        );

        if (entityResult == null) {
            return blockResult;
        } else {
            double entityDist = entityResult.getLocation().distanceToSqr(player.getEyePosition(1.0F));
            return (entityDist < blockDist) ? entityResult : blockResult;
        }
    }

    private void spawnLightning(Level world, double x, double y, double z) {
        LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(world);
        if (lightningBolt != null) {
            lightningBolt.moveTo(x, y, z);
            world.addFreshEntity(lightningBolt);
        }
    }

    // Finds the highest block below sky and strikes it with lightning
    private void spawnLightningAtSky(Level world, BlockPos targetPos) {
        BlockPos strikePos = targetPos;
        while (strikePos.getY() < world.getMaxBuildHeight()) {
            BlockPos abovePos = strikePos.above();
            if (world.canSeeSky(abovePos)) {
                spawnLightning(world, strikePos.getX() + 0.5, strikePos.getY() + 1, strikePos.getZ() + 0.5);
                world.explode(null, strikePos.getX() + 0.5, strikePos.getY() + 1, strikePos.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.TNT);
                return;
            }
            strikePos = abovePos;
        }
    }
}