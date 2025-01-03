package dorkix.mods;

import dorkix.mods.netherite_compass.Constants;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.item.property.numeric.NumericProperties;
import net.minecraft.util.Identifier;

public class NetheriteCompassModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		NumericProperties.ID_MAPPER.put(Identifier.of(Constants.MODID, Constants.MODID), NetheriteCompassProperty.CODEC);
	}
}