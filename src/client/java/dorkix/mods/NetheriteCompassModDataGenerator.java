package dorkix.mods;

import java.util.List;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.BlockStateModelGenerator;
import net.minecraft.client.data.ItemModelGenerator;
import net.minecraft.client.data.ItemModels;
import net.minecraft.client.render.item.model.RangeDispatchItemModel;

public class NetheriteCompassModDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(NetheriteCompassModelGenerator::new);
	}
}

class NetheriteCompassModelGenerator extends FabricModelProvider {
	public NetheriteCompassModelGenerator(FabricDataOutput generator) {
		super(generator);
	}

	@Override
	public void generateItemModels(ItemModelGenerator itemModelGenerator) {
		List<RangeDispatchItemModel.Entry> list = itemModelGenerator
				.createCompassRangeDispatchEntries(NetheriteCompassMod.NETHERITE_COMPASS);
		itemModelGenerator.output
				.accept(
						NetheriteCompassMod.NETHERITE_COMPASS,
						ItemModels.rangeDispatch(new NetheriteCompassProperty(true), 32.0F, list));
	}

	@Override
	public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
	}
}