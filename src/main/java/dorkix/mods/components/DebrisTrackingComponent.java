package dorkix.mods.components;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.math.GlobalPos;

public record DebrisTrackingComponent(boolean isTracking, Optional<GlobalPos> target) {
  public static final Codec<DebrisTrackingComponent> CODEC = RecordCodecBuilder.create(builder -> {
    return builder.group(
        Codec.BOOL.fieldOf("isTracking").forGetter(DebrisTrackingComponent::isTracking),
        GlobalPos.CODEC.optionalFieldOf("target").forGetter(DebrisTrackingComponent::target))
        .apply(builder, DebrisTrackingComponent::new);
  });

  public static final DebrisTrackingComponent DEFAULT = new DebrisTrackingComponent(false, Optional.empty());
}
