package dorkix.mods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dorkix.mods.components.DebrisTrackingComponent;
import dorkix.mods.config.ModConfig;
import dorkix.mods.netherite_compass.Constants;
import dorkix.mods.netherite_compass.item.NetheriteCompass;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class NetheriteCompassMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Constants.MODID);
	public static ModConfig config;

	public static final ComponentType<DebrisTrackingComponent> DEBRIS_TRACKING_COMPONENT = Registry.register(
			Registries.DATA_COMPONENT_TYPE,
			Identifier.of(Constants.MODID, "tracked_debris"),
			ComponentType.<DebrisTrackingComponent>builder().codec(DebrisTrackingComponent.CODEC).build());

	public static final RegistryKey<Item> NETHERITE_COMPASS_KEY = RegistryKey.of(RegistryKeys.ITEM,
			Identifier.of(Constants.MODID, "netherite_compass"));

	public static final Item NETHERITE_COMPASS = new NetheriteCompass(
			new Settings().rarity(Rarity.EPIC).fireproof().maxCount(1).registryKey(NETHERITE_COMPASS_KEY)
					.component(DEBRIS_TRACKING_COMPONENT, DebrisTrackingComponent.DEFAULT));

	public static final RegistryKey<ItemGroup> NETHERITE_COMPASS_GROUP_KEY = RegistryKey.of(
			Registries.ITEM_GROUP.getKey(), Identifier.of(Constants.MODID, "item_group"));

	public static final ItemGroup NETHERITE_COMPASS_GROUP = FabricItemGroup.builder()
			.icon(() -> new ItemStack(NETHERITE_COMPASS))
			.displayName(Text.translatable("item.netherite_compass.netherite_compass"))
			.build();

	@Override
	public void onInitialize() {
		config = ModConfig.load();
		LOGGER.info("Netherite Compass Chunk Search Radius: " + config.chunkRadius);

		Registry.register(Registries.ITEM, NETHERITE_COMPASS_KEY, NETHERITE_COMPASS);

		Registry.register(Registries.ITEM_GROUP, NETHERITE_COMPASS_GROUP_KEY, NETHERITE_COMPASS_GROUP);

		ItemGroupEvents.modifyEntriesEvent(NETHERITE_COMPASS_GROUP_KEY).register(content -> {
			content.add(NETHERITE_COMPASS);
		});
	}
}