package org.polyfrost.craftycrashes.mixin;

import net.minecraft.crash.CrashReportCategory;
import org.polyfrost.craftycrashes.SMAPper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrashReportCategory.class)
abstract class CrashReportCategoryMixin {
	@Shadow
	private StackTraceElement[] stackTrace;

	@Inject(method = "getPrunedStackTrace",
			at = @At(value = "INVOKE", target = "Ljava/lang/System;arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V", remap = false, shift = Shift.AFTER), remap = true)
	private void fixCause(CallbackInfoReturnable<Integer> call) {
		SMAPper.apply(stackTrace, "java.", "sun.", "net.fabricmc.loader.", "com.mojang.authlib.");
	}
}