package io.dorkix.netherite_compass.items;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import io.dorkix.netherite_compass.NetheriteCompass;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class NetheriteCompassItem extends Item {

    public static final String ANCIENT_DEBRIS_POS_KEY = "AncientDebrisPos";
    public static final String ANCIENT_DEBRIS_DIMENSION_KEY = "AncientDebrisDimension";
    public static final String ANCIENT_DEBRIS_TRACKED_KEY = "AncientDebrisTracked";

    public NetheriteCompassItem(Properties p_41383_) {
        super(p_41383_);
    }

    public static GlobalPos getTrackedPos(ItemStack stack) {
        var nbtComponent = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComponent == null) {
            return null;
        }

        var nbt = nbtComponent.copyTag();

        if (nbt == null)
            return null;

        boolean hasTrackedValue = nbt.contains(ANCIENT_DEBRIS_TRACKED_KEY);
        if (!hasTrackedValue) {
            return null;
        }
        boolean tracked = nbt.getBoolean(ANCIENT_DEBRIS_TRACKED_KEY);

        Optional<ResourceKey<Level>> worldKey;
        boolean hasPosKey = nbt.contains(ANCIENT_DEBRIS_POS_KEY);
        boolean hasDimKey = nbt.contains(ANCIENT_DEBRIS_DIMENSION_KEY);

        if (hasPosKey && hasDimKey && tracked && (worldKey = getTrackedDimension(stack)).isPresent()) {
            BlockPos blockPos = NbtUtils.readBlockPos(nbt, ANCIENT_DEBRIS_POS_KEY).get();
            return GlobalPos.of(worldKey.get(), blockPos);
        }
        return null;
    }

    private static Optional<BlockPos> findAncientDebris(ItemStack stack, Level level, Entity entity, boolean force) {
        if (level.isClientSide) {
            return Optional.empty();
        }
        // See if the nbt data for the item has a tracked position
        var trackedPos = getTrackedPos(stack);
        setTooltip(stack, level);
        if (!force && trackedPos != null) {
            // If its not a forced search triggered by the use the compass then check if the
            // position from the nbt still contains the Ancient Debris Block.
            var trackedBlock = level.getBlockState(trackedPos.pos());

            // If it still exist don't start the search.
            // This will result in permanently locking onto one entity (until the Debris is
            // broken) but the user can trigger a new search for the closest ancient Debris
            // by right clicking the compass.
            if (trackedBlock.getBlock() == Blocks.ANCIENT_DEBRIS) {
                return Optional.of(trackedPos.pos());
            } else {
                // Check if the tracked pos is in the same dimension as the entity holding the
                // compass. trackedBlock might be any other block because of this. If this is
                // the case
                // just ignore and don't search again.
                setTooltip(stack, level);
                var dimKey = getTrackedDimension(stack);
                if (dimKey.isPresent()
                        && !dimKey.get().toString().equals(entity.level().dimension().toString())) {
                    return Optional.empty();
                }
            }
        }

        BlockPos entPos = entity.blockPosition();
        // Get all the block entities in the player's nearby chunks and find the closest
        // Ancient Debris.

        Optional<BlockPos> closest = findAncientDebrisInNearbyChunks(level, entPos, 1);
        // Play sound if the tracking state changes
        playSoundOnStateChange(level, entity, stack, closest);

        // Save the result into the item's nbt data. writeNbt will handle the case if no
        // Ancient Debris has been found.
        writeNbt(stack, level.dimension(), closest);
        setTooltip(stack, level);
        return closest;
    }

    private static Optional<ResourceKey<Level>> getTrackedDimension(ItemStack stack) {
        var nbtComponent = stack.get(DataComponents.CUSTOM_DATA);
        if (nbtComponent == null) {
            return null;
        }

        var nbt = nbtComponent.copyTag();
        return Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, nbt.get(ANCIENT_DEBRIS_DIMENSION_KEY)).result();
    }

    private static void playSoundOnStateChange(Level level, Entity entity, ItemStack stack,
            Optional<BlockPos> closest) {
        if (closest.isPresent()) {
            playSound(level, entity, true);
            return;
        }
        var trackedPos = getTrackedPos(stack);
        if (!closest.isPresent() && trackedPos != null) {
            playSound(level, entity, false);
        }
    }

    private static void writeNbt(ItemStack itemStack, ResourceKey<Level> levelKey, Optional<BlockPos> closest) {
        var nbtComponent = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);

        var nbt = nbtComponent.copyTag();

        if (closest.isPresent()) {
            BlockPos pos = closest.get();
            nbt.put(ANCIENT_DEBRIS_POS_KEY, NbtUtils.writeBlockPos(pos));
            Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, levelKey)
                    .resultOrPartial(NetheriteCompass.LOGGER::error)
                    .ifPresent(nbtElement -> nbt.put(ANCIENT_DEBRIS_DIMENSION_KEY, (Tag) nbtElement));
            nbt.putBoolean(ANCIENT_DEBRIS_TRACKED_KEY, true);
        } else {
            nbt.put(ANCIENT_DEBRIS_POS_KEY, NbtUtils.writeBlockPos(BlockPos.ZERO));
            nbt.putBoolean(ANCIENT_DEBRIS_TRACKED_KEY, false);
            nbt.remove(ANCIENT_DEBRIS_DIMENSION_KEY);
        }

        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

    }

    private static void playSound(Level level, Entity entity, boolean success) {
        level.playSound(null, entity.blockPosition(),
                success ? SoundEvents.LODESTONE_COMPASS_LOCK : SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 1f, 1f);
    }

    /**
     * @param world       The world to search
     * @param entPos      position of the entity holding the compass
     * @param chunkRadius 0 = only the entity's chunk, 1 = 3x3 chunks around the
     *                    entity, 2 = 5x5 etc...
     * @return Optional position of the closest Ancient Debris
     */
    private static Optional<BlockPos> findAncientDebrisInNearbyChunks(Level level, BlockPos entPos,
            int chunkRadius) {
        chunkRadius = Math.max(0, chunkRadius);
        HashSet<BlockPos> blocks = new HashSet<BlockPos>();
        var chunkPos = level.getChunk(entPos).getPos();

        for (int x = chunkPos.x - chunkRadius; x <= chunkPos.x + chunkRadius; x++) {
            for (int z = chunkPos.z - chunkRadius; z <= chunkPos.z + chunkRadius; z++) {
                level.getChunk(x, z).findBlocks(blockState -> blockState.getBlock() == Blocks.ANCIENT_DEBRIS,
                        (pos, blockState) -> {
                            // copy values as the pos given by the lamba is mutable and will change during
                            // the block filtering
                            blocks.add(BlockPos.containing(pos.getX(), pos.getY(), pos.getZ()));
                        });
            }
        }
        return getClosestBlockPos(blocks, entPos);
    }

    private static Optional<BlockPos> getClosestBlockPos(HashSet<BlockPos> blocks,
            BlockPos entPos) {
        var min = blocks.stream()
                // comparator lambda to see if the first entry is closer than the second
                .min((entry1, entry2) -> {
                    double dist1 = GetDistance(entPos, entry1);
                    double dist2 = GetDistance(entPos, entry2);
                    return dist1 < dist2 ? -1 : dist1 > dist2 ? 1 : 0;
                });

        return min.isPresent() ? Optional.of(min.get()) : Optional.empty();
    }

    private static double GetDistance(BlockPos playerPos, BlockPos entityPos) {
        return playerPos.distSqr(entityPos);
    }

    public static void setTooltip(ItemStack itemStack, Level level) {
        if (getTrackedPos(itemStack) != null) {
            // Hint for new search
            var dimKey = getTrackedDimension(itemStack);
            if (dimKey.isPresent() &&
                    !dimKey.get().toString().equals(level.dimension().toString())) {
                // Tracked in other dimension
                setWrongDimensionLore(itemStack);
            } else {
                // Tracked nearby
                setLockedOnLore(itemStack);
            }

        } else if (itemStack.get(DataComponents.CUSTOM_DATA) != null) {
            setLocatingLore(itemStack);
        }
    }

    public static Component getHintText() {
        return Component.translatable("item.netherite_compass.netherite_compass.hint")
                .setStyle(Style.EMPTY.withItalic(false).withBold(false).withColor(ChatFormatting.GRAY));
    }

    public static void setWrongDimensionLore(ItemStack stack) {
        stack.set(DataComponents.LORE,
                new ItemLore(List.of(getHintText(),
                        Component.translatable("item.netherite_compass.netherite_compass.wrong_dim1")
                                .setStyle(Style.EMPTY.withItalic(false).withBold(true)
                                        .withColor(ChatFormatting.DARK_RED)),
                        Component.translatable("item.netherite_compass.netherite_compass.wrong_dim2")
                                .setStyle(
                                        Style.EMPTY.withItalic(false).withBold(true)
                                                .withColor(ChatFormatting.DARK_RED)))));
    }

    public static void setLockedOnLore(ItemStack stack) {

        stack.set(DataComponents.LORE,
                new ItemLore(
                        List.of(getHintText(), Component
                                .translatable("item.netherite_compass.netherite_compass.locked_on")
                                .setStyle(
                                        Style.EMPTY.withItalic(false).withBold(false).withColor(ChatFormatting.RED)))));
    }

    public static void setLocatingLore(ItemStack stack) {
        stack.set(DataComponents.LORE,
                new ItemLore(List.of(Component.translatable("item.netherite_compass.netherite_compass.not_found")
                        .setStyle(
                                Style.EMPTY.withItalic(false).withBold(false).withColor(ChatFormatting.DARK_PURPLE)))));
    }

    public static float getSpinningAngle(ClientLevel world) {
        Long t = world.getGameTime() % 32L;
        return t.floatValue() / 32.0f;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        findAncientDebris(stack, level, entity, false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }
        if (!stack.is(NetheriteCompass.NETHERITE_COMPASS_ITEM.get())) {
            return InteractionResultHolder.pass(stack);
        }

        player.getCooldowns().addCooldown(this, 100);
        // we can only modify the item stack on the server, ignore the client world
        // call.
        var pos = findAncientDebris(stack, level, player, true);
        // play the correct sound depending on the result
        playSound(level, player, pos.isPresent());

        return InteractionResultHolder.success(stack);
    }

}
