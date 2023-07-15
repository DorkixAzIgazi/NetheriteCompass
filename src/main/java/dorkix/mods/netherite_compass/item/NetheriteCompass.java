package dorkix.mods.netherite_compass.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import dorkix.mods.NetheriteCompassMod;
import dorkix.mods.netherite_compass.blockentity.AncientDebrisBlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

public class NetheriteCompass extends Item {

    public static final String ANCIENT_DEBRIS_POS_KEY = "AncientDebrisPos";
    public static final String ANCIENT_DEBRIS_DIMENSION_KEY = "AncientDebrisDimension";
    public static final String ANCIENT_DEBRIS_TRACKED_KEY = "AncientDebrisTracked";

    @Nullable
    public static GlobalPos getTrackedPos(NbtCompound nbt) {

        if (nbt == null)
            return null;

        boolean hasTrackedValue = nbt.contains(ANCIENT_DEBRIS_TRACKED_KEY);
        if (!hasTrackedValue) {
            return null;
        }
        boolean tracked = nbt.getBoolean(ANCIENT_DEBRIS_TRACKED_KEY);

        Optional<RegistryKey<World>> worldKey;
        boolean hasPosKey = nbt.contains(ANCIENT_DEBRIS_POS_KEY);
        boolean hasDimKey = nbt.contains(ANCIENT_DEBRIS_DIMENSION_KEY);

        if (hasPosKey && hasDimKey && tracked && (worldKey = getTrackedDimension(nbt)).isPresent()) {
            BlockPos blockPos = NbtHelper.toBlockPos(nbt.getCompound(ANCIENT_DEBRIS_POS_KEY));
            return GlobalPos.create(worldKey.get(), blockPos);
        }
        return null;
    }

    private static Optional<BlockPos> findAncientDebris(ItemStack stack, World world, Entity entity, boolean force) {
        if (world.isClient) {
            return Optional.empty();
        }
        // See if the nbt data for the item has a tracked position
        var trackedPos = getTrackedPos(stack.getNbt());
        if (!force && trackedPos != null) {
            // If its not a forced search triggered by the use the compass then check if the
            // position from the nbt still contains the Ancient Debris Block entity.
            Optional<AncientDebrisBlockEntity> trackedEntity = world.getBlockEntity(trackedPos.getPos(),
                    NetheriteCompassMod.ANCIENT_DEBRIS_BLOCK_ENTITY);

            // If it still exist don't start the search.
            // This will result in permanently locking onto one entity (until the Debris is
            // broken) but the user can trigger a new search for the closest ancient Debris
            // by right clicking the compass.
            if (!trackedEntity.isEmpty()) {
                return Optional.of(trackedPos.getPos());
            } else {
                // Check if the tracked pos is in the same dimension as the entity holding the
                // compass. trackedEntity might be null because of this. If this is the case
                // just ignore and don't search again.
                var dimKey = getTrackedDimension(stack.getNbt());
                if (dimKey.isPresent()
                        && !dimKey.get().toString().equals(entity.getWorld().getRegistryKey().toString())) {
                    return Optional.empty();
                }
            }
        }

        BlockPos entPos = entity.getBlockPos();
        // Get all the block entities in the player's nearby chunks and find the closest
        // Ancient Debris.

        Optional<BlockPos> closest = findAncientDebrisInNearbyChunks(world, entPos, 1);

        // Play sound if the tracking state changes
        playSoundOnStateChange(world, entity, stack, closest);

        // Save the result into the item's nbt data. writeNbt will handle the case if no
        // Ancient Debris has been found.
        writeNbt(world.getRegistryKey(), closest, stack.getOrCreateNbt());

        return closest;
    }

    private static Optional<RegistryKey<World>> getTrackedDimension(NbtCompound nbt) {
        return World.CODEC.parse(NbtOps.INSTANCE, nbt.get(ANCIENT_DEBRIS_DIMENSION_KEY)).result();
    }

    private static void playSoundOnStateChange(World world, Entity entity, ItemStack stack,
            Optional<BlockPos> closest) {
        if (closest.isPresent()) {
            playSound(world, entity, true);
            return;
        }
        var trackedPos = getTrackedPos(stack.getNbt());
        if (!closest.isPresent() && trackedPos != null) {
            playSound(world, entity, false);
        }
    }

    private static void writeNbt(RegistryKey<World> worldKey, Optional<BlockPos> closest, NbtCompound nbt) {
        if (nbt == null) {
            return;
        }
        if (closest.isPresent()) {
            BlockPos pos = closest.get();
            nbt.put(ANCIENT_DEBRIS_POS_KEY, NbtHelper.fromBlockPos(pos));
            World.CODEC.encodeStart(NbtOps.INSTANCE, worldKey)
                    .resultOrPartial(NetheriteCompassMod.LOGGER::error)
                    .ifPresent(nbtElement -> nbt.put(ANCIENT_DEBRIS_DIMENSION_KEY, (NbtElement) nbtElement));
            nbt.putBoolean(ANCIENT_DEBRIS_TRACKED_KEY, true);
        } else {
            nbt.put(ANCIENT_DEBRIS_POS_KEY, NbtHelper.fromBlockPos(BlockPos.ORIGIN));
            nbt.putBoolean(ANCIENT_DEBRIS_TRACKED_KEY, false);
            nbt.remove(ANCIENT_DEBRIS_DIMENSION_KEY);
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
        Map<BlockPos, AncientDebrisBlockEntity> entities = new HashMap<>();
        var chunkPos = world.getWorldChunk(entPos).getPos();

        for (int x = chunkPos.x - chunkRadius; x <= chunkPos.x + chunkRadius; x++) {
            for (int z = chunkPos.z - chunkRadius; z <= chunkPos.z + chunkRadius; z++) {
                entities.putAll(world.getChunk(x, z).getBlockEntities().entrySet().stream()
                        // only want the Ancient Debris entities
                        .filter(entry -> entry.getValue() instanceof AncientDebrisBlockEntity)
                        .collect(Collectors.toMap(entry -> entry.getKey(),
                                entry -> (AncientDebrisBlockEntity) entry.getValue())));
            }
        }
        return getClosestBlockPos(entities, entPos);
    }

    private static Optional<BlockPos> getClosestBlockPos(Map<BlockPos, AncientDebrisBlockEntity> entities,
            BlockPos entPos) {
        Optional<Entry<BlockPos, AncientDebrisBlockEntity>> min = entities.entrySet().stream()
                // comparator lambda to see if the first entry is closer than the second
                .min((entry1, entry2) -> {
                    double dist1 = GetDistance(entPos, entry1.getKey());
                    double dist2 = GetDistance(entPos, entry2.getKey());
                    return dist1 < dist2 ? -1 : dist1 > dist2 ? 1 : 0;
                });

        return min.isPresent() ? Optional.of(min.get().getKey()) : Optional.empty();
    }

    private static double GetDistance(BlockPos playerPos, BlockPos entityPos) {
        return playerPos.getSquaredDistance(entityPos);
    }

    public NetheriteCompass(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            return TypedActionResult.success(user.getStackInHand(hand));
        }
        user.getItemCooldownManager().set(this, 100);
        // we can only modify the item stack on the server, ignore the client world
        // call.
        var pos = findAncientDebris(user.getMainHandStack(), world, user, true);
        // play the correct sound depending on the result
        playSound(world, user, pos.isPresent());

        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        if (tooltipContext.isCreative()) {
            return;
        }
        if (getTrackedPos(itemStack.getNbt()) != null) {
            // Hint for new search
            tooltip.add(
                    Text.translatable("item.netherite_compass.netherite_compass.hint").formatted(Formatting.GRAY));
            var dimKey = getTrackedDimension(itemStack.getNbt());
            if (dimKey.isPresent() && !dimKey.get().toString().equals(world.getRegistryKey().toString())) {
                // Tracked in other dimension
                tooltip.add(
                        Text.translatable("item.netherite_compass.netherite_compass.wrong_dim1")
                                .formatted(Formatting.DARK_RED).formatted(Formatting.BOLD));
                tooltip.add(
                        Text.translatable("item.netherite_compass.netherite_compass.wrong_dim2")
                                .formatted(Formatting.DARK_RED).formatted(Formatting.BOLD));
            } else {
                // Tracked nearby
                tooltip.add(
                        Text.translatable("item.netherite_compass.netherite_compass.locked_on")
                                .formatted(Formatting.RED));
            }

        } else if (itemStack.getNbt() != null) {
            tooltip.add(Text.translatable("item.netherite_compass.netherite_compass.not_found")
                    .formatted(Formatting.DARK_PURPLE));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        findAncientDebris(stack, world, entity, false);
    }
}
