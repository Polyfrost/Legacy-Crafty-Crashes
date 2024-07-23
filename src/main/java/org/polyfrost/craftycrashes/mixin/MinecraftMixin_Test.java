package org.polyfrost.craftycrashes.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin_Test {
    @Inject(method = "startGame", at = @At("HEAD"))
    private void iShouldShowUpInYourCrash(CallbackInfo ci) {
        System.out.println("Game started!");
        throw new RuntimeException("Test crash");
    }
}
