package com.onehumanawa.cnmcore.content.simpleschematic;

import com.onehumanawa.cnmcore.AllDataComponents;
import com.onehumanawa.cnmcore.CNMCore;
import com.onehumanawa.cnmcore.mixin.SchematicPrinterAccessor;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.SchematicProcessor;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.math.BBHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Ported from CreateSimpleSchematic by leaf, used with permission.
 * Loads a Simple Schematic item at an explicit anchor for instant printing,
 * bypassing Create's deploy/sync flow.
 */
public class SimpleSchematicPrinter extends SchematicPrinter {

    public void loadSimpleSchematic(
            ItemStack blueprint, BlockPos anchor, Rotation rotation, Mirror mirror,
            Level world, boolean processNBT
    ) {
        if (blueprint.isEmpty() || anchor == null || world == null)
            return;
        if (!blueprint.has(AllDataComponents.SCHEMATIC_FILE))
            return;

        StructureTemplate activeTemplate = SimpleSchematicItem.loadSchematic(
                world.holderLookup(Registries.BLOCK), blueprint);
        if (activeTemplate.getSize().equals(Vec3i.ZERO))
            return;

        SchematicLevel blockReader = new SchematicLevel(anchor, world);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        settings.setRotation(rotation);
        settings.setMirror(mirror);
        if (processNBT)
            settings.addProcessor(SchematicProcessor.INSTANCE);

        SchematicPrinterAccessor accessor = (SchematicPrinterAccessor) this;

        accessor.setSchematicAnchor(anchor);
        accessor.setBlockReader(blockReader);

        try {
            activeTemplate.placeInWorld(
                    blockReader, anchor, anchor, settings, blockReader.getRandom(), Block.UPDATE_CLIENTS);
        } catch (Exception e) {
            CNMCore.LOGGER.error("Failed to load Schematic for Printing", e);
            accessor.setSchematicLoaded(true);
            accessor.setIsErrored(true);
            return;
        }

        BlockPos extraBounds = StructureTemplate.calculateRelativePosition(
                settings, new BlockPos(activeTemplate.getSize()).offset(-1, -1, -1));
        blockReader.setBounds(BBHelper.encapsulate(blockReader.getBounds(), extraBounds));

        StructureTransform transform = new StructureTransform(settings.getRotationPivot(), Direction.Axis.Y,
                settings.getRotation(), settings.getMirror());
        for (BlockEntity be : blockReader.getBlockEntities())
            transform.apply(be);

        accessor.setPrintingEntityIndex(-1);
        accessor.setPrintStage(PrintStage.BLOCKS);
        accessor.getDeferredBlocks().clear();
        BoundingBox bounds = blockReader.getBounds();
        accessor.setCurrentPos(new BlockPos(bounds.minX() - 1, bounds.minY(), bounds.minZ()));
        accessor.setSchematicLoaded(true);
    }
}
