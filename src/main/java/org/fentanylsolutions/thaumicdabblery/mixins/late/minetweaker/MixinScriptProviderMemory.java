package org.fentanylsolutions.thaumicdabblery.mixins.late.minetweaker;

import java.io.IOException;
import java.io.InputStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import minetweaker.runtime.providers.ScriptProviderMemory;
import minetweaker.util.FileUtil;

@Mixin(value = ScriptProviderMemory.class, remap = false)
public abstract class MixinScriptProviderMemory {

    @Redirect(
        method = "collect",
        at = @At(value = "INVOKE", target = "Lminetweaker/util/FileUtil;read(Ljava/io/InputStream;)[B"))
    private static byte[] td$closeCollectedScript(InputStream input) throws IOException {
        // The collector owns script.open(). Leaving it open prevents replacing scripts on Windows until GC.
        try (InputStream stream = input) {
            return FileUtil.read(stream);
        }
    }
}
