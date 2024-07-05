package dorkix.mods;

import dorkix.mods.netherite_compass.item.NetheriteCompass;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;

public class NetheriteCompassModClient implements ClientModInitializer {
	CompassAnglePredicateProvider ANGLE_DELEGATE = new CompassAnglePredicateProvider((world, stack, entity) -> {
		return NetheriteCompass.getTrackedPos(stack);
	});

	private static float getSpinningAngle(ClientWorld world) {
		Long t = world.getTime() % 32L;
		return t.floatValue() / 32.0f;
	}

	@Override
	public void onInitializeClient() {
		ModelPredicateProviderRegistry.register(NetheriteCompassMod.NETHERITE_COMPASS, Identifier.of("angle"),
				(stack, world, entity, i) -> {
					var pos = NetheriteCompass.getTrackedPos(stack);
					if (pos == null && world != null) {
						return getSpinningAngle(world);
					}

					return ANGLE_DELEGATE.unclampedCall(stack, world, entity, i);
				});
	}
}