package com.onehumanawa.cnmcore.foundation.data;

import com.tterrag.registrate.Registrate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.NotNull;

public class CreateRegistrate extends Registrate {

    private CreateRegistrate(String modId) {
        super(modId);
    }

    public static @NotNull CreateRegistrate create(@NotNull String modId) {
        return new CreateRegistrate(modId);
    }

    @Override
    public @NotNull CreateRegistrate defaultCreativeTab(@NotNull ResourceKey<CreativeModeTab> tab) {
        return (CreateRegistrate) super.defaultCreativeTab(tab);
    }
}