package com.onehumanawa.cnmcore.content.simpleschematic;

import com.onehumanawa.cnmcore.AllDataComponents;
import com.onehumanawa.cnmcore.CNMCore;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;

/**
 * A schematic item that loads its structure from the local {@code schematics/} folder,
 * referenced by the {@link AllDataComponents#SCHEMATIC_FILE} data component.
 */
public class SimpleSchematicItem extends Item {

    public SimpleSchematicItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        String name = super.getName(stack).getString();
        String key = getTranslateKey(stack);
        if (key == null) {
            return Component.literal(name)
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        return Component.literal(name)
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable("item.cnmcore.simple_schematic.dash")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.translatable(key)
                        .withStyle(ChatFormatting.GOLD));
    }

    /**
     * Returns the display/translation key for the schematic file, or null if absent/invalid.
     * Never throws: any malformed component value yields null.
     */
    public static String getTranslateKey(ItemStack stack) {
        String fileName = stack.get(AllDataComponents.SCHEMATIC_FILE);
        if (fileName == null)
            return null;
        fileName = fileName.strip();
        if (!fileName.contains(".nbt"))
            return null;
        String cleanName = fileName.replaceAll("§[0-9a-fk-or]", "");
        return cleanName.endsWith(".nbt") ? cleanName.substring(0, cleanName.length() - 4) : cleanName;
    }

    /**
     * Loads the {@link StructureTemplate} referenced by the item's schematic file component.
     * Defensive against missing/malformed components, path traversal and IO errors: always
     * returns a valid (possibly empty) template and never throws.
     */
    public static StructureTemplate loadSchematic(HolderGetter<Block> lookup, ItemStack blueprint) {
        StructureTemplate t = new StructureTemplate();

        String schematic;
        try {
            schematic = blueprint.get(AllDataComponents.SCHEMATIC_FILE);
        } catch (Exception e) {
            CNMCore.LOGGER.warn("Failed to read schematic file component", e);
            return t;
        }
        if (schematic == null || !schematic.endsWith(".nbt"))
            return t;

        Path dir = Paths.get("schematics").toAbsolutePath();
        Path path;
        try {
            path = dir.resolve(Paths.get(schematic)).normalize();
        } catch (InvalidPathException e) {
            CNMCore.LOGGER.warn("Invalid schematic path: {}", schematic);
            return t;
        }
        // Path traversal guard: refuse anything escaping the schematics directory
        if (!path.startsWith(dir))
            return t;
        if (!Files.isRegularFile(path))
            return t;

        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(Files.newInputStream(path, StandardOpenOption.READ))))) {
            CompoundTag nbt = NbtIo.read(stream, new NbtAccounter(0x20000000L, 512));
            t.load(lookup, nbt);
        } catch (IOException e) {
            CNMCore.LOGGER.warn("Failed to read schematic: {}", schematic, e);
        } catch (Exception e) {
            CNMCore.LOGGER.error("Failed to parse schematic: {}", schematic, e);
        }

        return t;
    }
}
