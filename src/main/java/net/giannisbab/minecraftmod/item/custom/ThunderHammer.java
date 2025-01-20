package net.giannisbab.minecraftmod.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ThunderHammer extends Item {
    private static final float RAY_DISTANCE = 150.0F; // Adjust as needed

    public ThunderHammer(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide) {
            summonLightning(world, player);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    private void summonLightning(Level world, Player player) {
        // Ray-trace from the player's viewpoint
        HitResult hitResult = player.pick(RAY_DISTANCE, 0.0F, false);

        // Only summon lightning if the ray-trace hits a block
        if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = blockHit.getBlockPos();
            // Create and spawn the lightning at the hit position
            LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(world);
            if (lightningBolt != null) {
                lightningBolt.moveTo(hitPos.getX() + 0.5, hitPos.getY(), hitPos.getZ() + 0.5);
                world.addFreshEntity(lightningBolt);
            }
        }
    }
}
