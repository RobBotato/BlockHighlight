package com.botato.blockhighlight.mixin;

import com.botato.blockhighlight.BlockMarkerClientMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    private int rightClickDelay;

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void blockhighlight_cancelStartUseItem(CallbackInfo ci) {
        if (BlockMarkerClientMod.editModeActive) {
            rightClickDelay = 4;
            ci.cancel();
        }
    }
}
