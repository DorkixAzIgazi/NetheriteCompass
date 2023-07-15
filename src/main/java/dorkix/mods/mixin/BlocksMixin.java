package dorkix.mods.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import dorkix.mods.netherite_compass.block.AncientDebrisBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

@Mixin(Blocks.class)
public abstract class BlocksMixin {
    @Redirect(slice = @Slice(from = @At(value = "CONSTANT", args = {
            "stringValue=ancient_debris"
    }, ordinal = 0)), at = @At(value = "NEW", target = "Lnet/minecraft/block/Block;*", ordinal = 0), method = "<clinit>")
    private static Block ancientDebris(AbstractBlock.Settings settings) {
        return new AncientDebrisBlock(settings);
    }
}
