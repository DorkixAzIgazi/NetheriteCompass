package io.dorkix.netherite_compass.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.serialization.MapCodec;

import io.dorkix.netherite_compass.NetheriteCompass;
import io.dorkix.netherite_compass.NetheriteCompassProperty;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(RangeSelectItemModelProperties.class)
public class RangeSelectItemModelPropertiesMixin {
  @Inject(method = "bootstrap", at = @At("TAIL"))
  private static void injectCustomProperty(CallbackInfo ci) {
    ExtraCodecs.LateBoundIdMapper<ResourceLocation, MapCodec<? extends RangeSelectItemModelProperty>> idMapper;
    try {
      // Access private static final field via reflection
      var field = RangeSelectItemModelProperties.class.getDeclaredField("ID_MAPPER");
      field.setAccessible(true);
      idMapper = (ExtraCodecs.LateBoundIdMapper<ResourceLocation, MapCodec<? extends RangeSelectItemModelProperty>>) field
          .get(null);

      // Register your custom property
      idMapper.put(ResourceLocation.fromNamespaceAndPath(NetheriteCompass.MODID, NetheriteCompass.MODID),
          NetheriteCompassProperty.CODEC);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject custom property into ID_MAPPER", e);
    }
  }
}
