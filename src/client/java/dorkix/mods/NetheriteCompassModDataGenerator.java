package dorkix.mods;

import java.util.List;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.RangeSelectItemModel;

public class NetheriteCompassModDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(NetheriteCompassModelGenerator::new);
	}
}

class NetheriteCompassModelGenerator extends FabricModelProvider {
	public NetheriteCompassModelGenerator(FabricPackOutput generator) {
		super(generator);
	}

	@Override
	public void generateItemModels(ItemModelGenerators itemModelGenerator) {
		List<RangeSelectItemModel.Entry> list = itemModelGenerator
				.createCompassModels(NetheriteCompassMod.NETHERITE_COMPASS);
		itemModelGenerator.itemModelOutput
				.accept(
						NetheriteCompassMod.NETHERITE_COMPASS,
						ItemModelUtils.rangeSelect(new NetheriteCompassProperty(true), 32.0F, list));
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
	}
}