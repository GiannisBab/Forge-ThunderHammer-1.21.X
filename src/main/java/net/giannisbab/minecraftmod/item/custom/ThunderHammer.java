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
    private static final float RAY_DISTANCE = 150.0F; // How far you can strike
    private static final int HOLE_RADIUS = 2;         // Radius of the hole

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

                        // Create a small random hole around the strike position
                        createRandomHole(world, blockPos, HOLE_RADIUS);
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

        // If no entity was hit, return the block result
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

    /**
     * Randomly destroys blocks in a small cubic region around the given center.
     * The destruction chance can be tweaked to create a more or less "natural" hole.
     */
    private void createRandomHole(Level world, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                double distance = Math.sqrt(dx * dx + dz * dz);

                // Add a small random offset so the boundary is not perfectly circular
                if (distance <= radius + world.random.nextFloat() * 0.5f) {

                    // Vertical bounds
                    int minY = center.getY() - radius;
                    int maxY = center.getY() + radius;

                    // Find the top non-air block in this column
                    BlockPos topBlockPos = null;
                    for (int y = maxY; y >= minY; y--) {
                        BlockPos checkPos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        // If it's not air, that's our top
                        if (!world.isEmptyBlock(checkPos)) {
                            topBlockPos = checkPos;
                            break;
                        }
                    }

                    // Destroy the top block and then apply a random destruction chance to blocks below.
                    if (topBlockPos != null) {
                        // Always destroy the top block
                        world.destroyBlock(topBlockPos, false);

                        // Pick a random destruction chance
                        float destructionChance = 0.3f + world.random.nextFloat() * 0.5f;

                        // Randomly destroy blocks below the top
                        for (int y = topBlockPos.getY() - 1; y >= minY; y--) {
                            BlockPos belowPos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                            if (!world.isEmptyBlock(belowPos)) {
                                if (world.random.nextFloat() < destructionChance) {
                                    world.destroyBlock(belowPos, false);
                                }
                            }
                        }
                    }
                    else {
                        // If no top block was found at all, do random destruction or skip it:
                        float destructionChance = 0.3f + world.random.nextFloat() * 0.5f;
                        for (int y = maxY; y >= minY; y--) {
                            BlockPos currentPos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                            if (!world.isEmptyBlock(currentPos)) {
                                if (world.random.nextFloat() < destructionChance) {
                                    world.destroyBlock(currentPos, false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
