package io.dorkix.netherite_compass;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = NetheriteCompass.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    private static final Field ID_MAPPER = ObfuscationReflectionHelper.findField(RangeSelectItemModelProperties.class,
            "ID_MAPPER");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {

        FMLJavaModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new ConfigScreen(screen)));
    }

    @SubscribeEvent
    public static void addCustomProperty(FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            try {
                ExtraCodecs.LateBoundIdMapper<ResourceLocation, MapCodec<? extends RangeSelectItemModelProperty>> idMapper = (ExtraCodecs.LateBoundIdMapper<ResourceLocation, MapCodec<? extends RangeSelectItemModelProperty>>) ID_MAPPER
                        .get(null);
                idMapper.put(ResourceLocation.fromNamespaceAndPath(NetheriteCompass.MODID, NetheriteCompass.MODID),
                        NetheriteCompassProperty.CODEC);
            } catch (IllegalAccessException e) {
                NetheriteCompass.LOGGER.error("Failed to register netherite_compass property");
                throw new RuntimeException("Failed to register netherite_compass property");
            }
        });
    }

}