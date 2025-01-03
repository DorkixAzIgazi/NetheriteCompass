package dorkix.mods;

import com.mojang.serialization.MapCodec;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.property.numeric.NumericProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public class NetheriteCompassProperty implements NumericProperty {
  public static final MapCodec<NetheriteCompassProperty> CODEC = NetheriteCompassState.CODEC.xmap(
      NetheriteCompassProperty::new,
      property -> property.state);
  private final NetheriteCompassState state;

  public NetheriteCompassProperty(boolean wobble) {
    this(new NetheriteCompassState(wobble));
  }

  private NetheriteCompassProperty(NetheriteCompassState state) {
    this.state = state;
  }

  @Override
  public float getValue(ItemStack stack, ClientWorld world, LivingEntity holder, int seed) {
    return this.state.getValue(stack, world, holder, seed);
  }

  @Override
  public MapCodec<NetheriteCompassProperty> getCodec() {
    return CODEC;
  }
}
