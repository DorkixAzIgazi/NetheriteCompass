package dorkix.mods;

import com.mojang.serialization.MapCodec;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class NetheriteCompassProperty implements RangeSelectItemModelProperty {
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
  public float get(ItemStack stack, ClientLevel world, ItemOwner context, int seed) {
    return this.state.get(stack, world, context, seed);
  }

  @Override
  public MapCodec<NetheriteCompassProperty> type() {
    return CODEC;
  }
}
