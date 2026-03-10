package dorkix.mods.netherite_compass.item;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import dorkix.mods.NetheriteCompassMod;
import dorkix.mods.components.DebrisTrackingComponent;
import dorkix.mods.netherite_compass.Constants;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

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

    private static Optional<BlockPos> findAncientDebris(ItemStack stack, Level world, Entity entity, boolean force) {
        if (world.isClientSide()) {
            return Optional.empty();
        }
        // See if the component data for the item has a tracked position
        var trackedPos = getTrackedPos(stack);
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
                var dimKey = getTrackedDimension(stack);
                if (dimKey.isPresent()
                        && !(dimKey.get() == entity.level().dimension())) {
                    return Optional.empty();
                }
            }
        }

        BlockPos entPos = entity.blockPosition();
        // Get all the blocks in the player's nearby chunks and find the closest
        // Ancient Debris.

        Optional<BlockPos> closest = findAncientDebrisInNearbyChunks(world, entPos,
                NetheriteCompassMod.config.chunkRadius);
        // Play sound if the tracking state changes
        playSoundOnStateChange(world, entity, stack, closest);

        // Save the result into the item's component data. writeComponent will handle
        // the case if no Ancient Debris has been found.
        writeComponent(stack, world.dimension(), closest);

        // Only update tooltip when force is true (triggered by right-click search)
        if (force) {
            setTooltip(stack, world, entity);
        }

        return closest;
    }

    private static Optional<ResourceKey<Level>> getTrackedDimension(ItemStack stack) {
        var trackingComponent = stack.getOrDefault(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
                DebrisTrackingComponent.DEFAULT);

        if (trackingComponent.isTracking() && trackingComponent.target().isPresent()) {
            return Optional.of(trackingComponent.target().get().dimension());
        }
        return Optional.empty();
    }

    private static void playSoundOnStateChange(Level world, Entity entity, ItemStack stack,
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

    private static void writeComponent(ItemStack itemStack, net.minecraft.resources.ResourceKey<Level> worldKey,
            Optional<BlockPos> closest) {
        if (closest.isPresent()) {
            itemStack.set(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
                    new DebrisTrackingComponent(true, Optional.of(GlobalPos.of(worldKey, closest.get()))));
        } else {
            itemStack.set(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
                    DebrisTrackingComponent.DEFAULT);
        }
        itemStack.set(DataComponents.TOOLTIP_STYLE, Identifier.fromNamespaceAndPath(Constants.MODID, "compass"));
    }

    private static void playSound(Level world, Entity entity, boolean success) {
        world.playSound(null, entity.blockPosition(),
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
    private static Optional<BlockPos> findAncientDebrisInNearbyChunks(Level world, BlockPos entPos,
            int chunkRadius) {
        chunkRadius = Math.max(0, chunkRadius);
        HashSet<BlockPos> blocks = new HashSet<>();
        var chunkPos = world.getChunkAt(entPos).getPos();

        for (int x = chunkPos.x - chunkRadius; x <= chunkPos.x + chunkRadius; x++) {
            for (int z = chunkPos.z - chunkRadius; z <= chunkPos.z + chunkRadius; z++) {
                world.getChunk(x, z).findBlocks(
                        blockState -> blockState.getBlock() == Blocks.ANCIENT_DEBRIS,
                        (pos, blockState) -> {
                            // copy values as the pos given by the lamba is mutable and will change the
                            // reference during the block filtering
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

    public NetheriteCompass(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!stack.is(NetheriteCompassMod.NETHERITE_COMPASS)) {
            return InteractionResult.PASS;
        }

        user.getCooldowns().addCooldown(stack, 100);
        // we can only modify the item stack on the server, ignore the client world
        // call.
        var pos = findAncientDebris(stack, world, user, true);
        // play the correct sound depending on the result
        playSound(world, user, pos.isPresent());

        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    public static void setTooltip(ItemStack itemStack, Level world, Entity entity) {
        var trackingComponent = itemStack.getOrDefault(NetheriteCompassMod.DEBRIS_TRACKING_COMPONENT,
                DebrisTrackingComponent.DEFAULT);
        if (getTrackedPos(itemStack) != null) {
            // Hint for new search
            var dimKey = getTrackedDimension(itemStack);
            if (dimKey.isPresent() &&
                    !(dimKey.get() == world.dimension())) {
                // Tracked in other dimension
                setWrongDimensionLore(itemStack);
            } else {
                // Tracked nearby
                setLockedOnLore(itemStack, entity);
            }
        } else if (!trackingComponent.isTracking()) {
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

    public static void setLockedOnLore(ItemStack stack, Entity entity) {
        if (entity instanceof Player player) {
            var trackedPos = getTrackedPos(stack);
            if (trackedPos != null) {
                double distance = Math.sqrt(player.blockPosition().distSqr(trackedPos.pos()));
                int roundedDistance = (int) Math.ceil(distance);
                String distanceKey = roundedDistance == 1
                        ? "item.netherite_compass.netherite_compass.distance.single"
                        : "item.netherite_compass.netherite_compass.distance.multiple";
                stack.set(DataComponents.LORE,
                        new ItemLore(
                                List.of(getHintText(),
                                        Component.translatable("item.netherite_compass.netherite_compass.locked_on")
                                                .setStyle(Style.EMPTY.withItalic(false).withBold(false)
                                                        .withColor(ChatFormatting.RED)),
                                        Component.translatable(distanceKey, roundedDistance)
                                                .setStyle(Style.EMPTY.withItalic(false).withBold(false)
                                                        .withColor(ChatFormatting.DARK_GRAY)))));
                return;
            }
        }
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

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        findAncientDebris(stack, world, entity, false);

        // Update tooltip when player has inventory open and compass is not in their
        // hand
        if (entity instanceof Player player &&
                (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) &&
                player.containerMenu == player.inventoryMenu) {
            setTooltip(stack, world, entity);
        }
    }

    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack,
            ItemStack newStack) {
        // Update tooltip when switching to a new compass stack
        if (!newStack.isEmpty() && player.level() != null) {
            setTooltip(newStack, player.level(), player);
        }
        return super.allowComponentsUpdateAnimation(player, hand, oldStack, newStack);
    }
}
