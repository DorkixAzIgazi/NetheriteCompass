package dorkix.mods;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dorkix.mods.components.DebrisTrackingComponent;
import dorkix.mods.netherite_compass.item.NetheriteCompass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.property.numeric.NeedleAngleState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class NetheriteCompassState extends NeedleAngleState {
  public static final MapCodec<NetheriteCompassState> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance
          .group(
              Codec.BOOL.optionalFieldOf("wobble", Boolean.valueOf(true)).forGetter(NetheriteCompassState::hasWobble))
          .apply(instance, NetheriteCompassState::new));
  private final NeedleAngleState.Angler aimedAngler;
  private final NeedleAngleState.Angler aimlessAngler;
  private final Random random = Random.create();

  public NetheriteCompassState(boolean wobble) {
    super(wobble);
    this.aimedAngler = this.createAngler(0.8F);
    this.aimlessAngler = this.createAngler(0.8F);
  }

  @Override
  protected float getAngle(ItemStack stack, ClientWorld world, int seed, Entity user) {
    var isTracking = stack.getOrDefault(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
        DebrisTrackingComponent.DEFAULT).isTracking();
    long time = world.getTime();

    if (!isTracking) {
      return getSpinningAngle(time);
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

    float f = this.aimlessAngler.getAngle() + (float) scatter(seed) / 2.1474836E9F;
    return MathHelper.floorMod(f, 1.0F);
  }

  private float getAngleTo(Entity entity, long time, BlockPos pos) {
    float f = (float) getAngleTo(entity, pos);
    float g = getBodyYaw(entity);
    if (entity instanceof PlayerEntity playerEntity && playerEntity.isMainPlayer()
        && playerEntity.getWorld().getTickManager().shouldTick()) {
      if (this.aimedAngler.shouldUpdate(time)) {
        this.aimedAngler.update(time, 0.5F - (g - 0.25F));
      }

      float h = f + this.aimedAngler.getAngle();
      return MathHelper.floorMod(h, 1.0F);
    }

    float h = 0.5F - (g - 0.25F - f);
    return MathHelper.floorMod(h, 1.0F);
  }

  private static boolean canPointTo(Entity entity, @Nullable GlobalPos pos) {
    return pos != null && pos.dimension() == entity.getWorld().getRegistryKey()
        && !(pos.pos().getSquaredDistance(entity.getPos()) < 1.0E-5F);
  }

  private static double getAngleTo(Entity entity, BlockPos pos) {
    Vec3d vec3d = Vec3d.ofCenter(pos);
    return Math.atan2(vec3d.getZ() - entity.getZ(), vec3d.getX() - entity.getX()) / (float) (Math.PI * 2);
  }

  private static float getBodyYaw(Entity entity) {
    return MathHelper.floorMod(entity.getBodyYaw() / 360.0F, 1.0F);
  }

  /**
   * Scatters a seed by integer overflow in multiplication onto the whole
   * int domain.
   */
  private static int scatter(int seed) {
    return seed * 1327217883;
  }

}
