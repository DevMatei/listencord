# listencord

shows what you're listening to on your phone as discord rich presence. pick which apps it watches, it picks up whatever they're playing and puts it on your profile through the discord app using [antisocialcord-kt](https://github.com/Nightdavisao/antisocialcord-kt).

album art comes from your navidrome server if you set one up, otherwise listenbrainz.

## setup

1. make an app at [discord.com/developers](https://discord.com/developers/applications), its icon shows up when there's no album art
2. install, give it notification access (on android 13+ sideloaded you have to allow restricted settings in app info first)
3. paste the application id, pick your apps, done

you need the discord app installed and logged in.

## battery

no foreground service, no wakelocks, no polling. it's a notification listener so android keeps it around for free, and it only does anything when a player says something changed. art gets looked up once per album and cached. discord gets let go a minute after you stop playing.

## building

```
./gradlew assembleRelease
```

apk ends up in `app/build/outputs/apk/release/`.
