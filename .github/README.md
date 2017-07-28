# ConnectedTexturesMod [![Curseforge](http://cf.way2muchnoise.eu/full_ctm_downloads.svg)](https://minecraft.curseforge.com/projects/ctm) [![Curseforge](http://cf.way2muchnoise.eu/versions/For%20MC_ctm_all.svg)](https://minecraft.curseforge.com/projects/ctm)

Extentions to the vanilla model system to allow contextual rendering in much more complex ways.

## What is CTM?

CTM originates from the render code that powered [Chisel](https://github.com/Chisel-Team/Chisel) through MC 1.10. Since then, it has been split, and made into its own fully functional library mod.

CTM allows resourcepack authors and modders alike to create complex render effects, such as connected textures, patterned textures, glowing elements, and more. Best of all, it allows all of this without any code dependencies whatsoever!

That's right, CTM can be used without ever writing a line of code, everything you need is exposed to the resource system, through model and mcmeta JSON files. For specific implementation details, please read the [wiki](https://github.com/Chisel-Team/ConnectedTexturesMod/wiki).

## How does it work?

CTM tries to do everything "by the book" as far as rendering is concerned. This means that there is no ASM hook for 1.7-style rendering, everything is handled via baked models. CTM takes the baked model provided by vanilla and transforms it on demand to suit the extra rendering effects specified in the special CTM JSON data. Additionally, it provides ways for mods and resourcepacks to be entirely non-depdendant on CTM, they can render something entirely different when CTM is present with a few extra lines of JSON.

# Setup Instructions

CTM is a normal mod, that is, all that is necessary to set up your own workspace is `gradlew sDecW` and then set up for your IDE of choice. See the [Forge Docs page on mod setup](http://mcforge.readthedocs.io/en/latest/gettingstarted/) for more info.

For information on contributing to CTM, see [the CONTRIBUTING guide](https://github.com/Chisel-Team/ConnectedTexturesMod/.github/CONTRIBUTING.md)
