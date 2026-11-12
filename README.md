# Vapulla

Vapulla is an Android sample app for [JavaSteam](https://github.com/Longi94/JavaSteam), a
Java/Kotlin library for talking to Valve's Steam network. It signs in to a Steam account and
shows what the library can do: friends and chat, the game library, cloud files, the Workshop,
and more.

It is a showcase and a test bed for JavaSteam, not a replacement for the official Steam app.

Vapulla is not affiliated with or endorsed by Valve. Steam is a trademark of Valve Corporation.

## Features

- Sign in with a QR code or with account name and password, including Steam Guard
- Friends list with presence, nicknames, recent chats and unread counters
- Friend chat: history, sending, Steam emoticons and stickers, image upload, typing notices,
  notifications with inline reply
- Group chats: channels, history, sending, live messages
- Game library from licenses and PICS, with playtime, achievements and stats
- Steam Cloud file list and download
- Workshop browser with subscribe and unsubscribe
- Game server browser
- Signed-in devices list
- Network log of the messages exchanged with Steam

See [TODO.txt](TODO.txt) for what is unverified, known issues and ideas for more.

## Building

Requirements:

- Android Studio with JDK 17 or newer
- Android SDK 37; the app needs Android 15 (API 35) or newer to run

```
./gradlew :app:installDebug
```

JavaSteam is pulled as a snapshot from Sonatype (`in.dragonbra:javasteam`, see
`gradle/libs.versions.toml`). To build against a local JavaSteam checkout, publish it with
`publishToMavenLocal` and add `mavenLocal()` to the repositories in `settings.gradle.kts`.

Code style is checked with kotlinter:

```
./gradlew lintKotlin
./gradlew formatKotlin
```

## Project layout

All code is under `app/src/main/java/in/dragonbra/vapulla`.

| Package | Contents |
|---|---|
| `service` | Foreground service that keeps the Steam session alive, and `SteamConnection`, which owns the JavaSteam client |
| `service/auth` | Credential and QR sign-in |
| `service/api` | One small class per JavaSteam surface the screens call (chat, friends, library, cloud, ...) |
| `service/sync` | Mirrors what Steam pushes (friends, licenses, PICS app info) into the database |
| `db` | Room database, entities and DAOs |
| `manager` | Saved account and settings (DataStore) |
| `ui` | Jetpack Compose screens, one package per screen under `ui/screens` |

The service package is the place to look for JavaSteam usage examples.

## Your account and your data

- Your password is never stored. It is passed to Steam's authentication service once and
  kept only in memory while signing in. Prefer the QR code, which avoids typing it at all.
- What is stored is the refresh token Steam issues, encrypted with a key held in the Android
  Keystore. It is deleted when Steam reports it as revoked or denied.
- Friends, chat messages and library data are cached in a local database on the device.
- The app talks only to Steam and the CDNs and storage Steam uses. It has no analytics and no
  server of its own.

To revoke Vapulla's access, open the official Steam app, go to Steam Guard, tap the gear icon,
choose Authorized Devices, select `PC Steam Client - "Vapulla <version>"` and tap Remove
Access.

Use it at your own risk. This is an unofficial client using an unofficial library, and it is
development software. It is developed and tested against the maintainer's
[personal main Steam account](https://steamcommunity.com/id/LossyDragon/).

## AI usage

AI coding assistants were used to write code in this repository, always under heavy human
intervention: the maintainer decides what is built, directs and corrects the changes, and
tests them on an emulator and a physical device.

Treat the code as you would any other sample code and review it before relying on it.

## Contributing

Issues and pull requests are welcome. Please run `./gradlew lintKotlin` before opening a pull
request, and say how the change was tested. If AI tools wrote part of a contribution, mention
it in the pull request.

## Credits and license

Vapulla was started by Long Tran, the original author of JavaSteam. Both JavaSteam and this
fork of Vapulla are now maintained by LossyDragon.

Licensed under the Apache License 2.0, see [LICENSE](LICENSE). Licenses of bundled third-party
work are in [third_party.txt](third_party.txt).
