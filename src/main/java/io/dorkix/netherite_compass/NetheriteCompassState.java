package io.dorkix.netherite_compass;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.dorkix.netherite_compass.items.NetheriteCompassItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.NeedleDirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NetheriteCompassState extends NeedleDirectionHelper {
  public static final MapCodec<NetheriteCompassState> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance
          .group(
              Codec.BOOL.optionalFieldOf("wobble", Boolean.valueOf(true)).forGetter(NetheriteCompassState::wobble))
          .apply(instance, NetheriteCompassState::new));
  private final NeedleDirectionHelper.Wobbler wobbler;
  private final NeedleDirectionHelper.Wobbler noTargetWobbler;
  private final RandomSource random = RandomSource.create();

  public NetheriteCompassState(boolean wobble) {
    super(wobble);
    this.wobbler = this.newWobbler(0.8F);
    this.noTargetWobbler = this.newWobbler(0.8F);
  }

  @Override
  protected float calculate(ItemStack stack, ClientLevel world, int seed, Entity user) {
    var isTracking = stack.getOrDefault(NetheriteCompass.DEBRIS_TRACKING_COMPONENT.get(),
        DebrisTrackingComponent.DEFAULT).isTracking();
    long time = world.getGameTime();

    if (!isTracking) {
      return getSpinningAngle(time);
    }

    GlobalPos globalPos = NetheriteCompassItem.getTrackedPos(stack);
    return !canPointTo(user, globalPos) ? this.getAimlessAngle(seed, time)
        : this.getAngleTo(user, time, globalPos.pos());
  }

  private static float getSpinningAngle(long time) {
    Long t = time % 32L;
    return t.floatValue() / 32.0f;
  }

  private float getAimlessAngle(int seed, long time) {
    if (this.noTargetWobbler.shouldUpdate(time)) {
      this.noTargetWobbler.update(time, this.random.nextFloat());
    }

    float f = this.noTargetWobbler.rotation() + (float) scatter(seed) / 2.1474836E9F;
    return Mth.positiveModulo(f, 1.0F);
  }

  private float getAngleTo(Entity entity, long time, BlockPos pos) {
    float f = (float) getAngleTo(entity, pos);
    float g = getBodyYaw(entity);
    if (entity instanceof Player playerEntity && playerEntity.isLocalPlayer()
        && playerEntity.level().tickRateManager().runsNormally()) {
      if (this.wobbler.shouldUpdate(time)) {
        this.wobbler.update(time, 0.5F - (g - 0.25F));
      }

      float h = f + this.wobbler.rotation();
      return Mth.positiveModulo(h, 1.0F);
    }

    float h = 0.5F - (g - 0.25F - f);
    return Mth.positiveModulo(h, 1.0F);
  }

  private static boolean canPointTo(Entity entity, @Nullable GlobalPos pos) {
    return pos != null && pos.dimension() == entity.level().dimension()
        && !(pos.pos().distToCenterSqr(entity.position()) < 1.0E-5F);
  }

  private static double getAngleTo(Entity entity, BlockPos pos) {
    Vec3 vec3d = Vec3.atCenterOf(pos);
    return Math.atan2(vec3d.z() - entity.getZ(), vec3d.x() - entity.getX()) / (float) (Math.PI * 2);
  }

  private static float getBodyYaw(Entity entity) {
    return Mth.positiveModulo(entity.getVisualRotationYInDegrees() / 360.0F, 1.0F);
  }

  /**
   * Scatters a seed by integer overflow in multiplication onto the whole
   * int domain.
   */
  private static int scatter(int seed) {
    return seed * 1327217883;
  }
}
