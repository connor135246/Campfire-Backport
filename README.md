# Campfire Backport
 Backports Minecraft 1.14/1.16 campfires to 1.7.10

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/campfire-backport)<br>

Looking for detailed information about complicated config settings and CraftTweaker support? [Click here.](https://github.com/connor135246/Campfire-Backport/wiki) <br> 
You can also use the command "/campfirebackport dumpinfo" to create a text file in your config folder that has this information.

***

##### Building

The "no mixins" version uses nm.build.gradle and nm.mcmod.info. It doesn't have the mixin package, the mixin coremod, or the mixin config jsons.
And it doesn't embed Mixin 0.7.

Compile-time dependencies in the "lib2" folder include Advanced Rocketry, Botania, CodeChickenLib, CraftTweaker, Galacticraft, NEI, Thaumcraft, Waila, and Witchery.
