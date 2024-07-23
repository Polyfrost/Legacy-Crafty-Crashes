# Legacy Crafty Crashes

A Forge mod which modifies stack traces in crash reports to include the relevant Mixin and source line number where appropriate, acting as a super [Mixin Trace](https://github.com/comp500/mixintrace) whilst still working alongside it. Includes an [SMAP](https://jcp.org/en/jsr/detail?id=45) reader in the off chance you need one too.

This is a port of the original [Crafty Crashes](https://github.com/Chocohead/Crafty-Crashes) on Fabric by Chocohead. The original mod is licensed under ARR, and so is this port.
I (Wyvest) tried contacting them via Discord and received no response (once at January 23rd 2023, and another time at June 19th 2024), so I'm assuming they're inactive or don't want to respond.
If you are Chocohead and want me to take this down or discuss something, please contact us at [our Discord server](https://polyfrost.org/discord).

Although I haven't tested this mod in 1.7 or 1.12, this mod should work in those versions as well. If the above licensing issue is resolved, feel free to port this to Legacy Fabric as well - it should be as easy as changing the mappings to Legacy Yarn.

## Usage

Either add this mod to your mods folder, or include it as a dependency in your build.gradle.

```groovy
// Using Essential / Polyfrost Loom.
// If you're still using ForgeGradle for 1.8.9, why???
// (It won't work on FG cause it has to remap the Mixin classes in Crafty Crashes)

repositories {
    maven {
        name = "Polyfrost"
        url = "https://repo.polyfrost.org/releases"
    }
}

dependencies {
    // We need to remap the Mixin classes to the correct names, so use modImplementation
    // Use the version you want here, this is just an example
    modImplementation ("org.polyfrost:legacy-crafty-crashes:1.0.0") {
        transitive = false // I **HIGHLY** recommend you do this, as if you don't it will attempt to remap the entirety of fastutil, which is >20MB.
    }
    implementation "it.unimi.dsi:fastutil:8.5.13" // You don't need this if you're on 1.12
    
    // We use Mixin to modify the crash reports, but Legacy Forge doesn't provide it by default.
    // If you're using either OneConfig or Essential, you can use `compileOnly` instead here.
    // And, obviously, if you already have Mixin as a dependency, you don't need to include this.
    implementation "org.spongepowered:mixin:0.7.11-SNAPSHOT"
}
```

It will automatically modify the crash reports to include the Mixin and source line number where appropriate.

### Example
Using the following Mixin as an example of a Mixin which causes issues at runtime:
```java
@Mixin(Minecraft.class)
public class MinecraftMixin_Test {
    @Inject(method = "startGame", at = @At("HEAD"))
    private void iShouldShowUpInYourCrash(CallbackInfo ci) {
        System.out.println("Game started!");
        throw new RuntimeException("Test crash");
    }
}
```
Normally when this is run you'd get the following crash report section (which is effectively the top frames of the full stack trace):
```
-- Head --
Stacktrace:
	at net.minecraft.client.Minecraft.handler$iShouldShowUpInYourCrash$zzc000(Minecraft.java:3095)
	at net.minecraft.client.Minecraft.startGame(Minecraft.java)
```
`Minecraft` only has around 2500 lines, so 3095 is entirely unhelpful to finding what is wrong. The only knowledge gained is that the Mixin which crashed had handlers called `iShouldShowUpInYourCrash`.

In contrast, running with Crafty Crashes:
```
-- Head --
Stacktrace:
	at net.minecraft.client.Minecraft.handler$iShouldShowUpInYourCrash$zzc000(org/polyfrost/craftycrashes/mixin/MinecraftMixin_Test.java:14)
	at net.minecraft.client.Minecraft.startGame(Minecraft.java)
```
Now we have the fully qualified name of the Mixin which has gone wrong, the Mixin config which registered it, and the correct line numbers for the Mixin. This makes finding and debugging the Mixin much easier, especially as searching for a Mixin config by name on Github is more likely to return a unique result than searching the handler or Mixin class's name.