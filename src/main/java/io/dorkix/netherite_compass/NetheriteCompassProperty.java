package io.dorkix.netherite_compass;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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
  public float get(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity holder, int seed) {
    return this.state.get(stack, world, holder, seed);
  }

  @Override
  public MapCodec<NetheriteCompassProperty> type() {
    return CODEC;
  }
}
