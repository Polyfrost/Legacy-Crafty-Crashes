package org.polyfrost.craftycrashes.mixin;

import net.minecraft.crash.CrashReport;
import org.polyfrost.craftycrashes.SMAPper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReport.class)
abstract class CrashReportMixin {
	@Shadow
	private @Final Throwable cause;

	@Inject(method = "populateEnvironment", at = @At("HEAD"))
	private void fixCause(CallbackInfo call) {
		SMAPper.apply(cause, "java.", "sun.", "net.fabricmc.loader.", "net.minecraft.launchwrapper.", "com.mojang.authlib.", "net.fabricmc.devlaunchinjector.");
	}
}