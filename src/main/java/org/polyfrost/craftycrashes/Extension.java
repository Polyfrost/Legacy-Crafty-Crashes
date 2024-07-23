package org.polyfrost.craftycrashes;

import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Extension implements IMixinConfigPlugin, IExtension {
	public static final HashMap<String, IMixinInfo> appliedMixins = new HashMap<>();
	@Override
	public void onLoad(String mixinPackage) {
		Object transformer = MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
		if (!(transformer instanceof MixinTransformer)) throw new IllegalStateException("Running with an odd transformer: " + transformer);

		Class<MixinTransformer> clazz = MixinTransformer.class;
		try {
			Field field = clazz.getDeclaredField("extensions");
			field.setAccessible(true);
			Extensions extensions = (Extensions) field.get(transformer);
			extensions.add(this);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean checkActive(MixinEnvironment environment) {
		return true;
	}


	@Override
	public List<String> getMixins() {
		return Collections.emptyList();
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}


	@Override
	public void preApply(ITargetClassContext context) {
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		appliedMixins.put(mixinClassName, mixinInfo);
	}

	@Override
	public void postApply(ITargetClassContext context) {

	}

	@Override
	public void export(MixinEnvironment env, String name, boolean force, byte[] bytes) {
		ClassReader classReader = new ClassReader(bytes);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, 0);
		SourcePool.add(name, classNode.sourceDebug);
	}
}