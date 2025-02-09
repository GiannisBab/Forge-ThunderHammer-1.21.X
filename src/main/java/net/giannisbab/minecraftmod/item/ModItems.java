package net.giannisbab.minecraftmod.item;

import net.giannisbab.minecraftmod.item.custom.ThunderHammer;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "minecraftmod");

    public static final RegistryObject<Item> THUNDERHAMMER = ITEMS.register("thunderhammer",
            () -> new ThunderHammer(new Item.Properties().stacksTo(1).durability(250)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
