package net.giannisbab.minecraftmod.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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
    private static final float RAY_DISTANCE = 150.0F; // Adjust as needed

    public ThunderHammer(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide) {
            HitResult hitResult = getTargetHitResult(world, player, RAY_DISTANCE);

            if (hitResult != null) {
                switch (hitResult.getType()) {
                    case ENTITY -> {
                        // If we hit an Entity
                        EntityHitResult entityHit = (EntityHitResult) hitResult;
                        spawnLightning(world,
                                entityHit.getEntity().getX(),
                                entityHit.getEntity().getY(),
                                entityHit.getEntity().getZ());
                    }
                    case BLOCK -> {
                        // If we hit a Block
                        BlockHitResult blockHit = (BlockHitResult) hitResult;
                        BlockPos blockPos = blockHit.getBlockPos();
                        spawnLightning(world,
                                blockPos.getX() + 0.5,
                                blockPos.getY(),
                                blockPos.getZ() + 0.5);
                    }
                    default -> {
                        // Missed everything
                    }
                }
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    /**
     * Performs a combined ray trace for both blocks and entities,
     * and returns the closest HitResult (EntityHitResult or BlockHitResult).
     */
    private HitResult getTargetHitResult(Level world, Player player, double maxDistance) {
        // 1) Block pick
        HitResult blockResult = player.pick(maxDistance, 0.0F, false);
        double blockDist = Double.MAX_VALUE;
        if (blockResult != null) {
            blockDist = blockResult.getLocation().distanceToSqr(player.getEyePosition(1.0F));
        }

        // 2) Entity ray trace
        Vec3 startVec = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle();
        Vec3 endVec = startVec.add(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance);

        // Expand the player's bounding box to ensure we catch entities near the ray
        AABB boundingBox = player.getBoundingBox()
                .expandTowards(lookVec.scale(maxDistance))
                .inflate(1.0D);

        // Updated filter: now includes all LivingEntities except the player
        EntityHitResult entityResult = ProjectileUtil.getEntityHitResult(
                world,
                player,
                startVec,
                endVec,
                boundingBox,
                e -> e instanceof LivingEntity && e != player
        );

        // If no entity was hit return the block result
        if (entityResult == null) {
            return blockResult;
        } else {
            // Compare distances to see which is closer
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
}
