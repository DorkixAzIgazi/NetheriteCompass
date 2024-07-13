package io.dorkix.netherite_compass;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import io.dorkix.netherite_compass.items.NetheriteCompassItem;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(NetheriteCompass.MODID)
public class NetheriteCompass {
    public static final String MODID = "netherite_compass";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> NETHERITE_COMPASS_ITEM = ITEMS.register(
            "netherite_compass",
            () -> new NetheriteCompassItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> NETHERITE_COMPASS_TAB = CREATIVE_MODE_TABS.register(
            "netherite_compass_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .title(Component.translatable("item.netherite_compass.netherite_compass"))
                    .icon(() -> NETHERITE_COMPASS_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(NETHERITE_COMPASS_ITEM.get());
                    }).build());

    public NetheriteCompass() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::clientSetup);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    CompassItemPropertyFunction ANGLE_DELEGATE = new CompassItemPropertyFunction((world, stack, entity) -> {
        return NetheriteCompassItem.getTrackedPos(stack);
    });

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(NETHERITE_COMPASS_ITEM.get(),
                    ResourceLocation.parse("angle"), (stack, level, living, id) -> {
                        var pos = NetheriteCompassItem.getTrackedPos(stack);
                        if (pos == null && level != null) {
                            return NetheriteCompassItem.getSpinningAngle(level);
                        }

                        return ANGLE_DELEGATE.unclampedCall(stack, level, living, id);
                    });
        });
    }
}
