# listencord

shows what you're listening to on your phone as discord rich presence. pick which apps it watches, it picks up whatever they're playing and puts it on your profile through the discord app using [antisocialcord-kt](https://github.com/Nightdavisao/antisocialcord-kt).

album art comes from your navidrome server if you set one up, otherwise listenbrainz.

## setup

1. install, give it notification access (on android 13+ sideloaded you have to allow restricted settings in app info first)
2. pick your apps, done

you need the discord app installed and logged in.

it comes with my discord app id already filled in, so it works out of the box. if you want your own name or icon to show up, make an app at [discord.com/developers](https://discord.com/developers/applications) and paste its id in presence. its icon shows up when there's no album art.

## battery

no foreground service, no wakelocks, no polling. it's a notification listener so android keeps it around for free, and it only does anything when a player says something changed. art gets looked up once per album and cached. discord gets let go a minute after you stop playing.

## building

```
./gradlew assembleRelease
```

apk ends up in `app/build/outputs/apk/release/`.

## credits

- [antisocialcord-kt](https://github.com/Nightdavisao/antisocialcord-kt) by nightdavisao, does all the discord talking
- [navidrome](https://github.com/navidrome/navidrome), album art from your own server
- [feishin](https://github.com/jeffvli/feishin), copied how it gets shareable art urls out of navidrome
- [listenbrainz](https://listenbrainz.org), [musicbrainz](https://musicbrainz.org) and the [cover art archive](https://coverartarchive.org) for art when there's no navidrome
- [last.fm](https://www.last.fm) for the song and artist links
- [jetpack compose](https://developer.android.com/compose) and [material 3 expressive](https://m3.material.io) for the ui

## license

[unlicense](LICENSE), do whatever.
