package io.dorkix.netherite_compass;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import io.dorkix.netherite_compass.items.NetheriteCompassItem;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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

        private static final DeferredRegister<DataComponentType<?>> REGISTER = DeferredRegister
                        .create(Registries.DATA_COMPONENT_TYPE, MODID);

        public static final RegistryObject<DataComponentType<DebrisTrackingComponent>> DEBRIS_TRACKING_COMPONENT = REGISTER
                        .register("tracked_debris", () -> DataComponentType.<DebrisTrackingComponent>builder()
                                        .persistent(DebrisTrackingComponent.CODEC)
                                        .build());

        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
        public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
                        .create(Registries.CREATIVE_MODE_TAB, MODID);

        public static final ResourceKey<Item> NETHERITE_COMPASS_KEY = ResourceKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(MODID, "netherite_compass"));

        public static final RegistryObject<Item> NETHERITE_COMPASS_ITEM = ITEMS.register(
                        "netherite_compass",
                        () -> new NetheriteCompassItem(
                                        new Item.Properties().rarity(Rarity.EPIC).fireResistant().stacksTo(1)
                                                        .setId(NETHERITE_COMPASS_KEY)
                                                        .component(DEBRIS_TRACKING_COMPONENT.get(),
                                                                        DebrisTrackingComponent.DEFAULT)));

        public static final RegistryObject<CreativeModeTab> NETHERITE_COMPASS_TAB = CREATIVE_MODE_TABS.register(
                        "netherite_compass_tab",
                        () -> CreativeModeTab.builder()
                                        .withTabsBefore(CreativeModeTabs.COMBAT)
                                        .title(Component.translatable("item.netherite_compass.netherite_compass"))
                                        .icon(() -> NETHERITE_COMPASS_ITEM.get().getDefaultInstance())
                                        .displayItems((parameters, output) -> {
                                                output.accept(NETHERITE_COMPASS_ITEM.get());
                                        }).build());

        public NetheriteCompass(FMLJavaModLoadingContext context) {
                IEventBus modEventBus = context.getModEventBus();
                modEventBus.addListener(this::clientSetup);
                REGISTER.register(modEventBus);
                ITEMS.register(modEventBus);
                CREATIVE_MODE_TABS.register(modEventBus);
        }

        private void clientSetup(final FMLClientSetupEvent event) {
                RangeSelectItemModelProperties.ID_MAPPER.put(ResourceLocation.fromNamespaceAndPath(MODID, MODID),
                                NetheriteCompassProperty.CODEC);
        }
}
