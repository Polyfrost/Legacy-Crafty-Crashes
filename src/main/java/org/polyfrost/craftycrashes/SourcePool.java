package org.polyfrost.craftycrashes;

import org.polyfrost.craftycrashes.smap.FileInfo;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class SourcePool {
	private static final Map<String, String> SOURCES = new HashMap<>();
	private static final Map<FileInfo, IMixinInfo> MIXINS = new IdentityHashMap<>();

	static void add(String owner, String source) {
		if (SOURCES.put(owner, source) != null) {
			throw new IllegalArgumentException("Duplicate source mapping for " + owner);
		}
	}

	public static String get(String owner) {
		return SOURCES.get(owner);
	}

	public static IMixinInfo findFor(FileInfo file) {
		if (!MIXINS.containsKey(file)) {
			if (file.path != null && file.path.endsWith(".java")) {
				Class<ClassInfo> clazz = ClassInfo.class;
				try {
					Field field = clazz.getDeclaredField("cache");
					field.setAccessible(true);
					Map<String, ClassInfo> cache = (Map<String, ClassInfo>) field.get(null);
					ClassInfo info = cache.get(file.path.substring(0, file.path.length() - 5).replace(".", "/"));
					if (info != null && info.isMixin()) {//This is very silly but also useful
						IMixinInfo out = Extension.appliedMixins.get(info.getName());
						MIXINS.put(file, out);
						return out;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			MIXINS.put(file, null);
			return null;			
		} else {
			return MIXINS.get(file);
		}
	}
}