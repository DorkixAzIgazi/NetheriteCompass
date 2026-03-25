package dorkix.mods;

import dorkix.mods.netherite_compass.Constants;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.resources.Identifier;

public class NetheriteCompassModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		RangeSelectItemModelProperties.ID_MAPPER.put(Identifier.fromNamespaceAndPath(Constants.MODID, Constants.MODID),
				NetheriteCompassProperty.CODEC);
	}
}