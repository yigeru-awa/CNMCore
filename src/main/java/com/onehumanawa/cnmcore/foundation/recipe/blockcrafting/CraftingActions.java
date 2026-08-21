package com.onehumanawa.cnmcore.foundation.recipe.blockcrafting;

import com.onehumanawa.cnmcore.CNMCore;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * Built-in {@link CraftingAction} factories for {@code KubeJavaBlockCrafting},
 * so common crafting effects need no custom lambda:
 *
 * <pre>{@code
 * blockCrafting("cnmcore:andesite_alloy_craft")
 *         .pattern("I", "A", "Z")
 *         .where('I', "minecraft:iron_block")
 *         .where('A', "minecraft:andesite")
 *         .where('Z', "create:zinc_block")
 *         .center('A')
 *         .input("create:wrench")
 *         .result("create:andesite_alloy", 4)
 *         .action(CraftingActions.sound("minecraft:block.anvil_land"))
 *         .action(CraftingActions.particles("minecraft:happy_villager", 12))
 *         .register();
 * }</pre>
 *
 * Every factory is defensive: unknown sound or particle ids are logged and
 * skipped at execution time, never throwing.
 */
public final class CraftingActions {

    private CraftingActions() {}

    /**
     * Plays a sound at the crafting position.
     *
     * @param soundId sound event id, e.g. {@code "minecraft:block.anvil_land"}
     * @param volume  sound volume (1 = normal)
     * @param pitch   sound pitch (1 = normal)
     */
    public static CraftingAction sound(String soundId, float volume, float pitch) {
        return (level, center, player) -> {
            SoundEvent sound = resolveSound(soundId);
            if (sound != null) {
                level.playSound(null, center, sound, SoundSource.BLOCKS, volume, pitch);
            }
        };
    }

    /** Plays a sound at the crafting position with default volume and pitch. */
    public static CraftingAction sound(String soundId) {
        return sound(soundId, 0.1f, 0.1f);
    }

    /** Plays the given sound event at the crafting position. */
    public static CraftingAction sound(SoundEvent sound) {
        return (level, center, player) ->
                level.playSound(null, center, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    /**
     * Spawns particles above the crafting position.
     * Only simple particle types (those without extra options, like
     * {@code minecraft:happy_villager} or {@code minecraft:flame}) are
     * supported; others are logged and skipped.
     *
     * @param particleId particle type id, e.g. {@code "minecraft:happy_villager"}
     * @param count      number of particles
     */
    public static CraftingAction particles(String particleId, int count) {
        return (level, center, player) -> {
            ParticleType<?> type = resolveParticle(particleId);
            if (type == null) return;
            if (!(type instanceof SimpleParticleType simple)) {
                CNMCore.LOGGER.warn("[BlockCrafting] Particle type '{}' needs extra options and is not supported by particles()", particleId);
                return;
            }
            spawn(simple, level, center, count);
        };
    }

    /** Spawns the given simple particles above the crafting position. */
    public static CraftingAction particles(SimpleParticleType particle, int count) {
        return (level, center, player) -> spawn(particle, level, center, count);
    }

    private static void spawn(ParticleOptions options, ServerLevel level, BlockPos center, int count) {
        level.sendParticles(options,
                center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                count, 0.5, 0.5, 0.5, 0.05);
    }

    /**
     * Runs a command as the server at the crafting position.
     * Relative coordinates in the command refer to the crafting center.
     *
     * @param command command without leading slash, e.g. {@code "summon minecraft:lightning_bolt ~ ~1 ~"}
     */
    public static CraftingAction command(String command) {
        return (level, center, player) -> {
            CommandSourceStack source = new CommandSourceStack(
                    CommandSource.NULL,
                    Vec3.atCenterOf(center.above()),
                    Vec2.ZERO,
                    level,
                    2,
                    "cnmcore/block_crafting",
                    Component.literal("Block Crafting"),
                    level.getServer(),
                    null);
            level.getServer().getCommands().performPrefixedCommand(source, command);
        };
    }

    /**
     * Gives the player experience points.
     *
     * @param amount amount of experience points
     */
    public static CraftingAction experience(int amount) {
        return (level, center, player) -> player.giveExperiencePoints(amount);
    }

    /**
     * Shows a translated action bar message to the player.
     *
     * @param translationKey translation key, register translations through
     *                       {@code KubeJavaBlockCrafting.Builder#feedback(String, String, String)}
     *                       or the language provider
     */
    public static CraftingAction message(String translationKey) {
        return (level, center, player) ->
                player.displayClientMessage(Component.translatable(translationKey), true);
    }

    /**
     * Shows a literal action bar message to the player.
     *
     * @param text plain text, formatting codes allowed
     */
    public static CraftingAction literalMessage(String text) {
        return (level, center, player) ->
                player.displayClientMessage(Component.literal(text), true);
    }

    @Nullable
    private static SoundEvent resolveSound(String soundId) {
        if (soundId == null || soundId.isBlank()) return null;
        ResourceLocation id = ResourceLocation.tryParse(soundId);
        if (id == null) {
            CNMCore.LOGGER.warn("[BlockCrafting] Invalid sound id: {}", soundId);
            return null;
        }
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound == null) {
            CNMCore.LOGGER.warn("[BlockCrafting] Unknown sound id: {}", soundId);
        }
        return sound;
    }

    @Nullable
    private static ParticleType<?> resolveParticle(String particleId) {
        if (particleId == null || particleId.isBlank()) return null;
        ResourceLocation id = ResourceLocation.tryParse(particleId);
        if (id == null) {
            CNMCore.LOGGER.warn("[BlockCrafting] Invalid particle id: {}", particleId);
            return null;
        }
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(id);
        if (type == null) {
            CNMCore.LOGGER.warn("[BlockCrafting] Unknown particle id: {}", particleId);
        }
        return type;
    }
}
