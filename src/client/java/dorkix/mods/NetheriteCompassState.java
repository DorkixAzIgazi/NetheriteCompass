package dorkix.mods;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dorkix.mods.components.DebrisTrackingComponent;
import dorkix.mods.netherite_compass.item.NetheriteCompass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.NeedleDirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class NetheriteCompassState extends NeedleDirectionHelper {
  public static final MapCodec<NetheriteCompassState> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance
          .group(
              Codec.BOOL.optionalFieldOf("wobble", Boolean.valueOf(true)).forGetter(NetheriteCompassState::wobble))
          .apply(instance, NetheriteCompassState::new));
  private final NeedleDirectionHelper.Wobbler aimedAngler;
  private final NeedleDirectionHelper.Wobbler aimlessAngler;
  private final RandomSource random = RandomSource.create();

  public NetheriteCompassState(boolean wobble) {
    super(wobble);
    this.aimedAngler = this.newWobbler(0.8F);
    this.aimlessAngler = this.newWobbler(0.8F);
  }

  @Override
  public float calculate(ItemStack stack, ClientLevel world, int seed, @Nullable ItemOwner context) {
    var isTracking = stack.getOrDefault(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
        DebrisTrackingComponent.DEFAULT).isTracking();
    long time = world.getGameTime();

    if (!isTracking) {
      return getSpinningAngle(time);
    }

    Entity user = context != null ? context.asLivingEntity() : null;
    if (user == null) {
      return this.getAimlessAngle(seed, time);
    }

    GlobalPos globalPos = NetheriteCompass.getTrackedPos(stack);
    return !canPointTo(user, globalPos) ? this.getAimlessAngle(seed, time)
        : this.getAngleTo(user, time, globalPos.pos());
  }

  private static float getSpinningAngle(long time) {
    Long t = time % 32L;
    return t.floatValue() / 32.0f;
  }

  private float getAimlessAngle(int seed, long time) {
    if (this.aimlessAngler.shouldUpdate(time)) {
      this.aimlessAngler.update(time, this.random.nextFloat());
    }

    float f = this.aimlessAngler.rotation() + (float) scatter(seed) / 2.1474836E9F;
    return Mth.positiveModulo(f, 1.0F);
  }

  private float getAngleTo(Entity entity, long time, BlockPos pos) {
    float f = (float) getAngleTo(entity, pos);
    float g = getBodyYaw(entity);
    if (entity instanceof Player player && player.isLocalPlayer()
        && player.level().tickRateManager().runsNormally()) {
      if (this.aimedAngler.shouldUpdate(time)) {
        this.aimedAngler.update(time, 0.5F - (g - 0.25F));
      }

      float h = f + this.aimedAngler.rotation();
      return Mth.positiveModulo(h, 1.0F);
    }

    float h = 0.5F - (g - 0.25F - f);
    return Mth.positiveModulo(h, 1.0F);
  }

  private static boolean canPointTo(Entity entity, @Nullable GlobalPos pos) {
    return pos != null && pos.dimension() == entity.level().dimension()
        && !(Vec3.atCenterOf(pos.pos()).distanceToSqr(entity.position()) < 1.0E-5F);
  }

  private static double getAngleTo(Entity entity, BlockPos pos) {
    Vec3 vec3 = Vec3.atCenterOf(pos);
    return Math.atan2(vec3.z - entity.getZ(), vec3.x - entity.getX()) / (float) (Math.PI * 2);
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
