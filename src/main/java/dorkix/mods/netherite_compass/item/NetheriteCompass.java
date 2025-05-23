package dorkix.mods.netherite_compass.item;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import dorkix.mods.NetheriteCompassMod;
import dorkix.mods.components.DebrisTrackingComponent;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

public class NetheriteCompass extends Item {

    public static final String ANCIENT_DEBRIS_POS_KEY = "AncientDebrisPos";
    public static final String ANCIENT_DEBRIS_DIMENSION_KEY = "AncientDebrisDimension";
    public static final String ANCIENT_DEBRIS_TRACKED_KEY = "AncientDebrisTracked";

    @Nullable
    public static GlobalPos getTrackedPos(ItemStack stack) {

        var trackingComponent = stack.getOrDefault(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
                DebrisTrackingComponent.DEFAULT);

        if (trackingComponent.isTracking() && trackingComponent.target().isPresent()) {
            return trackingComponent.target().get();
        }
        return null;
    }

    private static Optional<BlockPos> findAncientDebris(ItemStack stack, World world, Entity entity, boolean force) {
        if (world.isClient) {
            return Optional.empty();
        }
        // See if the component data for the item has a tracked position
        var trackedPos = getTrackedPos(stack);
        setTooltip(stack, world);
        if (!force && trackedPos != null) {
            // If its not a forced search triggered by the use of the compass then check if
            // the position from the component still contains the Ancient Debris Block.
            var trackedBlock = world.getBlockState(trackedPos.pos());

            // If it still exist don't start the search.
            // This will result in permanently locking onto one entity (until the Debris is
            // broken) but the user can trigger a new search for the closest ancient Debris
            // by right clicking the compass.
            if (trackedBlock.getBlock() == Blocks.ANCIENT_DEBRIS) {
                return Optional.of(trackedPos.pos());
            } else {
                // Check if the tracked pos is in the same dimension as the entity holding the
                // compass. trackedEntity might be null because of this. If this is the case
                // just ignore and don't search again.
                setTooltip(stack, world);
                var dimKey = getTrackedDimension(stack);
                if (dimKey.isPresent()
                        && !(dimKey.get() == entity.getWorld().getRegistryKey())) {
                    return Optional.empty();
                }
            }
        }

        BlockPos entPos = entity.getBlockPos();
        // Get all the blocks in the player's nearby chunks and find the closest
        // Ancient Debris.

        Optional<BlockPos> closest = findAncientDebrisInNearbyChunks(world, entPos,
                NetheriteCompassMod.config.chunkRadius);
        // Play sound if the tracking state changes
        playSoundOnStateChange(world, entity, stack, closest);

        // Save the result into the item's component data. writeComponent will handle
        // the case if no Ancient Debris has been found.
        writeComponent(stack, world.getRegistryKey(), closest);
        setTooltip(stack, world);
        return closest;
    }

    private static Optional<RegistryKey<World>> getTrackedDimension(ItemStack stack) {
        var trackingComponent = stack.getOrDefault(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
                DebrisTrackingComponent.DEFAULT);

        if (trackingComponent.isTracking() && trackingComponent.target().isPresent()) {
            return Optional.of(trackingComponent.target().get().dimension());
        }
        return Optional.empty();
    }

    private static void playSoundOnStateChange(World world, Entity entity, ItemStack stack,
            Optional<BlockPos> closest) {
        if (closest.isPresent()) {
            playSound(world, entity, true);
            return;
        }
        var trackedPos = getTrackedPos(stack);
        if (!closest.isPresent() && trackedPos != null) {
            playSound(world, entity, false);
        }
    }

    private static void writeComponent(ItemStack itemStack, RegistryKey<World> worldKey, Optional<BlockPos> closest) {
        if (closest.isPresent()) {
            itemStack.set(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
                    new DebrisTrackingComponent(true, Optional.of(GlobalPos.create(worldKey, closest.get()))));
        } else {
            itemStack.set(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
                    DebrisTrackingComponent.DEFAULT);
        }
    }

    private static void playSound(World world, Entity entity, boolean success) {
        world.playSound(null, entity.getBlockPos(),
                success ? SoundEvents.ITEM_LODESTONE_COMPASS_LOCK : SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.PLAYERS, 1f, 1f);
    }

    /**
     * @param world       The world to search
     * @param entPos      position of the entity holding the compass
     * @param chunkRadius 0 = only the entity's chunk, 1 = 3x3 chunks around the
     *                    entity, 2 = 5x5 etc...
     * @return Optional position of the closest Ancient Debris
     */
    private static Optional<BlockPos> findAncientDebrisInNearbyChunks(World world, BlockPos entPos,
            int chunkRadius) {
        chunkRadius = Math.max(0, chunkRadius);
        HashSet<BlockPos> blocks = new HashSet<>();
        var chunkPos = world.getWorldChunk(entPos).getPos();

        for (int x = chunkPos.x - chunkRadius; x <= chunkPos.x + chunkRadius; x++) {
            for (int z = chunkPos.z - chunkRadius; z <= chunkPos.z + chunkRadius; z++) {
                world.getChunk(x, z).forEachBlockMatchingPredicate(
                        blockState -> blockState.getBlock() == Blocks.ANCIENT_DEBRIS,
                        (pos, blockState) -> {
                            // copy values as the pos given by the lamba is mutable and will change the
                            // reference during the block filtering
                            blocks.add(BlockPos.ofFloored(pos.getX(), pos.getY(), pos.getZ()));
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
        return playerPos.getSquaredDistance(entityPos);
    }

    public NetheriteCompass(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (world.isClient) {
            return ActionResult.PASS;
        }
        if (!stack.isOf(NetheriteCompassMod.NETHERITE_COMPASS)) {
            return ActionResult.PASS;
        }

        user.getItemCooldownManager().set(stack, 100);
        // we can only modify the item stack on the server, ignore the client world
        // call.
        var pos = findAncientDebris(stack, world, user, true);
        // play the correct sound depending on the result
        playSound(world, user, pos.isPresent());

        return ActionResult.SUCCESS.withNewHandStack(stack);
    }

    public static void setTooltip(ItemStack itemStack, World world) {
        var trackingComponent = itemStack.getOrDefault(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
                DebrisTrackingComponent.DEFAULT);
        if (getTrackedPos(itemStack) != null) {
            // Hint for new search
            var dimKey = getTrackedDimension(itemStack);
            if (dimKey.isPresent() &&
                    !(dimKey.get() == world.getRegistryKey())) {
                // Tracked in other dimension
                setWrongDimensionLore(itemStack);
            } else {
                // Tracked nearby
                setLockedOnLore(itemStack);
            }
        } else if (!trackingComponent.isTracking()) {
            setLocatingLore(itemStack);
        }
    }

    public static Text getHintText() {
        return Text.translatable("item.netherite_compass.netherite_compass.hint")
                .setStyle(Style.EMPTY.withItalic(false).withBold(false).withColor(Formatting.GRAY));
    }

    public static void setWrongDimensionLore(ItemStack stack) {
        stack.set(DataComponentTypes.LORE,
                new LoreComponent(List.of(getHintText(),
                        Text.translatable("item.netherite_compass.netherite_compass.wrong_dim1")
                                .setStyle(Style.EMPTY.withItalic(false).withBold(true).withColor(Formatting.DARK_RED)),
                        Text.translatable("item.netherite_compass.netherite_compass.wrong_dim2")
                                .setStyle(
                                        Style.EMPTY.withItalic(false).withBold(true).withColor(Formatting.DARK_RED)))));
    }

    public static void setLockedOnLore(ItemStack stack) {
        stack.set(DataComponentTypes.LORE,
                new LoreComponent(
                        List.of(getHintText(), Text.translatable("item.netherite_compass.netherite_compass.locked_on")
                                .setStyle(Style.EMPTY.withItalic(false).withBold(false).withColor(Formatting.RED)))));
    }

    public static void setLocatingLore(ItemStack stack) {
        stack.set(DataComponentTypes.LORE,
                new LoreComponent(List.of(Text.translatable("item.netherite_compass.netherite_compass.not_found")
                        .setStyle(Style.EMPTY.withItalic(false).withBold(false).withColor(Formatting.DARK_PURPLE)))));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        findAncientDebris(stack, world, entity, false);
    }
}
