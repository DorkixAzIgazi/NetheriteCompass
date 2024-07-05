package dorkix.mods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dorkix.mods.netherite_compass.Constants;
import dorkix.mods.netherite_compass.blockentity.AncientDebrisBlockEntity;
import dorkix.mods.netherite_compass.item.NetheriteCompass;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class NetheriteCompassMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Constants.MODID);

	public static final Item NETHERITE_COMPASS = new NetheriteCompass(
			new Settings().rarity(Rarity.EPIC).fireproof().maxCount(1));

	public static final ItemGroup NETHERITE_COMPASS_GROUP = FabricItemGroup.builder()
			.icon(() -> new ItemStack(NETHERITE_COMPASS))
			.displayName(Text.translatable("item.netherite_compass.netherite_compass"))
			.build();

	public static final BlockEntityType<AncientDebrisBlockEntity> ANCIENT_DEBRIS_BLOCK_ENTITY = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.ofVanilla("ancient_debris"),
			BlockEntityType.Builder.create(AncientDebrisBlockEntity::new, Blocks.ANCIENT_DEBRIS).build());

	@Override
	public void onInitialize() {
		Registry.register(Registries.ITEM, Identifier.of(Constants.MODID, "netherite_compass"), NETHERITE_COMPASS);

		var netheriteCompassGroupId = Identifier.of(Constants.MODID, "item_group");

		Registry.register(Registries.ITEM_GROUP, netheriteCompassGroupId, NETHERITE_COMPASS_GROUP);
		var reg = RegistryKey.of(Registries.ITEM_GROUP.getKey(), netheriteCompassGroupId);

		ItemGroupEvents.modifyEntriesEvent(reg).register(content -> {
			content.add(NETHERITE_COMPASS);
		});
	}
}