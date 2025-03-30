package io.dorkix.netherite_compass;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = NetheriteCompass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
        private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        private static final ForgeConfigSpec.IntValue CHUNK_RADIUS = BUILDER
                        .comment("Chunk search radius")
                        .defineInRange("chunkRadius", 1, 1, 16);

        static final ForgeConfigSpec SPEC = BUILDER.build();

        public static int chunkRadius = 1;

        @SubscribeEvent
        static void onLoad(final ModConfigEvent event) {
                chunkRadius = CHUNK_RADIUS.get();
        }

        public static void setChunkRadius(int newRadius) {
                newRadius = Math.clamp(newRadius, 1, 16);
                chunkRadius = newRadius;
                CHUNK_RADIUS.set(newRadius);
        }
}
