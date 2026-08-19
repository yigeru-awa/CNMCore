package com.onehumanawa.cnmcore.foundation.mixin;

import com.simibubi.create.content.schematics.client.ToolSelectionScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for Create's {@link ToolSelectionScreen} private fields, required by
 * the custom passive render. Field names verified against Create 6.0.10.
 */
@Mixin(value = ToolSelectionScreen.class, remap = false)
public interface ToolSelectionScreenAccessor {
    @Accessor("initialized")
    boolean hasInitialized();

    @Accessor("yOffset")
    float getYOffset();
}
