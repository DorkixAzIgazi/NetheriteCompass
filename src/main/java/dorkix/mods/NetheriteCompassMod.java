package dorkix.mods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dorkix.mods.components.DebrisTrackingComponent;
import dorkix.mods.config.ModConfig;
import dorkix.mods.netherite_compass.Constants;
import dorkix.mods.netherite_compass.item.NetheriteCompass;
import net.fabricmc.api.ModInitializer;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.CreativeModeTab.Row;

public class NetheriteCompassMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Constants.MODID);
	public static ModConfig config;

	public static final DataComponentType<DebrisTrackingComponent> DEBRIS_TRACKING_COMPONENT = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(Constants.MODID, "tracked_debris"),
			DataComponentType.<DebrisTrackingComponent>builder().persistent(DebrisTrackingComponent.CODEC).build());

	public static final ResourceKey<Item> NETHERITE_COMPASS_KEY = ResourceKey.create(Registries.ITEM,
			Identifier.fromNamespaceAndPath(Constants.MODID, "netherite_compass"));

	public static final Item NETHERITE_COMPASS = new NetheriteCompass(
			new Item.Properties().rarity(Rarity.EPIC).fireResistant().stacksTo(1).setId(NETHERITE_COMPASS_KEY)
					.component(DataComponents.CUSTOM_NAME,
							Component.translatable("item.netherite_compass.netherite_compass")
									.setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFF4100).withItalic(false)))
					.component(DEBRIS_TRACKING_COMPONENT, DebrisTrackingComponent.DEFAULT));

	public static final ResourceKey<CreativeModeTab> NETHERITE_COMPASS_GROUP_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(Constants.MODID, "item_group"));

	public static final CreativeModeTab NETHERITE_COMPASS_GROUP = CreativeModeTab.builder(Row.TOP, 0)
			.icon(() -> new ItemStack(NETHERITE_COMPASS))
			.title(Component.translatable("item.netherite_compass.netherite_compass"))
			.displayItems((params, output) -> output.accept(NETHERITE_COMPASS))
			.build();

	@Override
	public void onInitialize() {
		config = ModConfig.load();
		LOGGER.info("Netherite Compass Chunk Search Radius: " + config.chunkRadius);

		Registry.register(BuiltInRegistries.ITEM, NETHERITE_COMPASS_KEY, NETHERITE_COMPASS);

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, NETHERITE_COMPASS_GROUP_KEY, NETHERITE_COMPASS_GROUP);
	}
}