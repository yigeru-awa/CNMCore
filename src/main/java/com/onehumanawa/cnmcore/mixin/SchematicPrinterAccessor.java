package com.onehumanawa.cnmcore.mixin;

import com.simibubi.create.content.schematics.SchematicPrinter;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Ported from CreateSimpleSchematic by leaf, used with permission.
 * Accessor for Create's {@link SchematicPrinter} private fields, required to
 * load a schematic at an arbitrary anchor without Create's deploy flow.
 * Field names verified against Create 6.0.10.
 */
@Mixin(value = SchematicPrinter.class, remap = false)
public interface SchematicPrinterAccessor {
    @Accessor("schematicAnchor")
    void setSchematicAnchor(BlockPos anchor);

    @Accessor("blockReader")
    void setBlockReader(SchematicLevel world);

    @Accessor("schematicLoaded")
    void setSchematicLoaded(boolean loaded);

    @Accessor("isErrored")
    void setIsErrored(boolean errored);

    @Accessor("printingEntityIndex")
    void setPrintingEntityIndex(int index);

    @Accessor("printStage")
    void setPrintStage(SchematicPrinter.PrintStage stage);

    @Accessor("deferredBlocks")
    List<BlockPos> getDeferredBlocks();

    @Accessor("currentPos")
    void setCurrentPos(BlockPos pos);
}
