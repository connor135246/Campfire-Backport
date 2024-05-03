# Campfire Backport
 Backports Minecraft 1.14/1.16 campfires to 1.7.10

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/campfire-backport)<br>

Looking for detailed information about complicated config settings and CraftTweaker support? [Click here.](https://github.com/connor135246/Campfire-Backport/wiki) <br> 
You can also use the command "/campfirebackport dumpinfo" to create a text file in your config folder that has this information.

The normal jar embeds [Mixin 0.7.11](https://github.com/SpongePowered/Mixin). Mixin is Copyright (c) SpongePowered & contributors and is licensed under the [MIT License](https://github.com/SpongePowered/Mixin/blob/master/LICENSE.txt).  
The "+nomixin" jar doesn't, and requires a separate mixin mod such as [UniMixins](https://github.com/LegacyModdingMC/UniMixins).

***

#### Building

Use the build argument `-Pnomixin` to build the "+nomixin" jar.  

Compile-time dependencies in the "lib2" folder include Advanced Rocketry, Botania, CodeChickenLib, CraftTweaker, Galacticraft Core, Gregtech 6, NEI, Thaumcraft, Waila, and Witchery.
