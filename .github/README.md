# ConnectedTexturesMod [![Discord](https://img.shields.io/discord/166066006186262529.svg?colorB=7289DA&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHYAAABWAgMAAABnZYq0AAAACVBMVEUAAB38%2FPz%2F%2F%2F%2Bm8P%2F9AAAAAXRSTlMAQObYZgAAAAFiS0dEAIgFHUgAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQfhBxwQJhxy2iqrAAABoElEQVRIx7WWzdGEIAyGgcMeKMESrMJ6rILZCiiBg4eYKr%2Fd1ZAfgXFm98sJfAyGNwno3G9sLucgYGpQ4OGVRxQTREMDZjF7ILSWjoiHo1n%2BE03Aw8p7CNY5IhkYd%2F%2F6MtO3f8BNhR1QWnarCH4tr6myl0cWgUVNcfMcXACP1hKrGMt8wcAyxide7Ymcgqale7hN6846uJCkQxw6GG7h2MH4Czz3cLqD1zHu0VOXMfZjHLoYvsdd0Q7ZvsOkafJ1P4QXxrWFd14wMc60h8JKCbyQvImzlFjyGoZTKzohwWR2UzSONHhYXBQOaKKsySsahwGGDnb%2FiYPJw22sCqzirSULYy1qtHhXGbtgrM0oagBV4XiTJok3GoLoDNH8ooTmBm7ZMsbpFzi2bgPGoXWXME6XT%2BRJ4GLddxJ4PpQy7tmfoU2HPN6cKg%2BledKHBKlF8oNSt5w5g5o8eXhu1IOlpl5kGerDxIVT%2BztzKepulD8utXqpChamkzzuo7xYGk%2FkpSYuviLXun5bzdRf0Krejzqyz7Z3p0I1v2d6HmA07dofmS48njAiuMgAAAAASUVORK5CYII%3D)](http://discord.gg/0vVjLvWg5kyQwnHG) [![Curseforge](http://cf.way2muchnoise.eu/full_267602_downloads.svg)](https://minecraft.curseforge.com/projects/ctm) [![Curseforge](http://cf.way2muchnoise.eu/versions/For%20MC_267602_all.svg)](https://minecraft.curseforge.com/projects/ctm)

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
