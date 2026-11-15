# Vapulla

Vapulla is an Android sample app for [JavaSteam](https://github.com/Longi94/JavaSteam), a
Java/Kotlin library for talking to Valve's Steam network. It signs in to a Steam account and
shows what the library can do: friends and chat, the game library and the store, game downloads,
cloud files, the Workshop, and more.

It is a showcase and a test bed for JavaSteam, not a replacement for the official Steam app.

Vapulla is not affiliated with or endorsed by Valve. Steam is a trademark of Valve Corporation.

## Download

The latest build is on the
[prerelease](https://github.com/LossyDragon/Vapulla/releases/tag/prerelease) release, as
`vapulla.apk`. It needs Android 9 or newer.

- There is one release, and each new build replaces the APK on it. Its title and notes say
  which version it is, the commit it was built from, and what changed since the build before.
- It is a pre-release: development software, built from the `Compose2` branch as work goes
  on, and not tested beyond what [TODO.txt](TODO.txt) lists.
- Every build is signed with the same key, so a newer one installs over an older one and
  keeps its data. A build you made yourself is signed with another key; uninstall it first.
- Android asks you to allow installing from the browser or file manager you open the APK
  with.
- There is no update check in the app. Look at the release for a newer build.

## Features

- Sign in with a QR code or with account name and password, including Steam Guard
- Friends list with presence, nicknames, recent chats and unread counters
- Friend chat: history, sending, Steam emoticons and stickers, image upload, typing notices,
  notifications with inline reply that Android lists among its conversations, YouTube links
  shown as a video card, and a setting to show messages as raw BBCode
- Friend profiles, with the raw persona state and rich presence Steam sent under "Under the hood",
  and for a friend request the friends you have in common
- Group chats: channels, history, sending, live messages
- Game library from licenses and PICS, with playtime, achievements, stats, leaderboards, the
  latest news, and which friends play, own or want a game
- Store: search for any game, owned or not, and see its price, reviews, tags and screenshots
- Game downloads with JavaSteam's depot downloader: pick a platform, branch (also one behind
  a password) and language,
  pause and resume, verify files, a queue that survives restarts, a free-space check before
  a download is accepted, a badge when a downloaded game has an update, installing to any
  storage or a folder of your own, and moving a finished game between them
- Steam Cloud file list and download
- Workshop browser with subscribe and unsubscribe, and downloading Workshop items
- Game server browser
- Your uploaded screenshots
- Account: wallet balance, email status, country and profile address; a notice when the
  account is in a game on another device; change your profile name; make, share and revoke
  friend invite links
- Steam Family: its members and the games it shares with you, which are in the library too
- Signed-in devices list, with signing a device out
- Optional, off by default: Steam's community and store pages open inside the app, signed in
  to your account
- Explore without signing in: an anonymous session, the way SteamCMD signs in, with the
  Under the Hood screens that need no account
- Under the Hood, a set of screens that show how Steam and JavaSteam work underneath:
  - Connection: the server you are on and what Steam answered when it signed you in
  - CM servers: every connection manager server the client knows about
  - Network log: the messages exchanged with Steam, service calls by method name, totals per
    message type, a search, and a packet's header and decoded body on tap
  - Licenses: the packages on the account, and which one grants a given app
  - PICS explorer: everything Steam's catalogue holds on any app or package, by ID or by a
    game's name, with a search inside it and values that copy on tap
  - PICS change feed: apps and packages changing across Steam, as they change, each with a
    link to its history on SteamDB
  - Depots: an app's depots, branches, build IDs and manifests, for any app ID, down to the
    files of a manifest
  - Download internals: the running download in chunks, bytes and connections
  - Web API: the interfaces and methods of api.steampowered.com that anyone may call
  - ID decoder: a Steam ID, game ID, global ID or a profile's custom URL broken into its parts
  - Auth tickets: make the ticket a game hands to a server to prove who is playing, see the
    fields inside it, and cancel it again
  - Diagnostics: connection history, and how earlier runs of the app ended

See [TODO.txt](TODO.txt) for what is unverified, known issues and ideas for more.

## Building

Requirements:

- Android Studio with JDK 17 or newer
- Android SDK 37; the app runs on Android 9 (API 28) or newer

```
./gradlew :app:installDebug
```

JavaSteam is pulled as a snapshot from Sonatype (`in.dragonbra:javasteam`, see
`gradle/libs.versions.toml`). To build against a local JavaSteam checkout, publish it with
`publishToMavenLocal`: a build in Maven local is used before the published snapshot. Delete
`~/.m2/repository/in/dragonbra` to go back to the published one.

To measure speed, such as how fast a game downloads, use the `benchmark` build type. It is the
release build signed with the debug key, so it installs over a debug build; a debug build
runs noticeably slower.

```
./gradlew :app:installBenchmark
```

The build under [Download](#download) is made by GitHub Actions
(`.github/workflows/pre-release.yml`). It runs when a push to `Compose2` raises `versionCode`
in `app/build.gradle.kts`: lint, the unit tests, then the release build, signed with a key
kept in the repository's secrets. The `prerelease` tag is moved to the commit it built. A
fork needs a key of its own; the top of the workflow file lists the secrets to set. Pull
requests get the lint and the unit tests from `.github/workflows/check.yml`.

The release build is not shrunk or obfuscated (`isMinifyEnabled = false`), so it is the same
code as a debug build. Without the key in the environment, `./gradlew :app:assembleRelease`
makes an unsigned APK.

The plain Kotlin parts have unit tests:

```
./gradlew :app:testDebugUnitTest
```

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
| `service/api` | One small class per JavaSteam surface the screens call (chat, friends, library, store, cloud, ...) |
| `service/sync` | Mirrors what Steam pushes (friends, licenses, PICS app info) into the database |
| `service/download` | The download queue on top of JavaSteam's `DepotDownloader`, and the choice of depots for a platform |
| `db` | Room database, entities and DAOs |
| `manager` | Saved account and settings (DataStore) |
| `ui` | Jetpack Compose screens, one package per screen under `ui/screens` |

The service package is the place to look for JavaSteam usage examples:

| To see how to | Look at |
|---|---|
| Set up the client, run its callbacks, reconnect | `service/SteamConnection.kt` |
| Sign in with a password or a QR code, and with a saved token | `service/auth/SteamAuthenticator.kt` |
| Follow friends, persona states and nicknames | `service/sync/FriendsSync.kt`, `service/api/FriendsApi.kt` |
| Send and receive friend messages, history, typing | `service/api/ChatApi.kt` |
| Use group chats | `service/api/ChatRoomsApi.kt` |
| Upload an image to a chat | `service/api/ChatMediaApi.kt` |
| Read licenses and PICS app info, and follow PICS changes | `service/sync/LicenseSync.kt`, `service/api/LibraryApi.kt` |
| Use Steam without an account | `service/sync/AnonymousPicsFeed.kt` |
| Pick depots and download a game | `service/download/DownloadPlan.kt`, `service/download/DownloadManager.kt` |
| Fetch a depot manifest | `service/api/ManifestApi.kt` |
| Call a unified service (most of the rest) | `service/api/SessionsApi.kt` is the shortest; Cloud, Workshop, Family, Store and Invites work the same way |
| Read leaderboards and achievements | `service/api/LeaderboardsApi.kt`, `service/api/LibraryApi.kt` |
| Query game servers | `service/api/ServerBrowserApi.kt`, `util/SourceQuery.kt` |
| Make and read an auth ticket | `service/api/AuthTicketApi.kt`, `util/AuthTicketReader.kt` |
| Get web cookies for Steam's sites | `service/api/WebSessionApi.kt` |
| Add a message handler and callback of your own | `service/handler/VapullaHandler.kt`, `service/callback/` |
| Watch every packet | `service/NetworkLog.kt`, `service/PacketInspector.kt` |

View models reach these through `SteamServiceConnection.request`, which also covers the case
of no session and logs a failure.

Nearly all text on screen is written in the code, in English, and not kept in string
resources.
That is on purpose: a sample is easier to read with the words next to the code that shows
them.

## Your account and your data

- Your password is never stored. It is passed to Steam's authentication service once and
  kept only in memory while signing in. Prefer the QR code, which avoids typing it at all.
- What is stored is the refresh token Steam issues, encrypted with a key held in the Android
  Keystore. It is deleted when Steam reports it as revoked or denied.
- Friends, chat messages and library data are cached in a local database on the device.
  They are erased when you sign out, when Steam ends the sign-in for good, and when a
  different account signs in, along with the chat shortcuts Android keeps for your
  friends. The list of downloads is kept, as the games are still there.
- "Explore without signing in" uses no account and saves nothing. It leaves a saved sign-in
  and the stored data alone.
- Downloaded games go to the app's own folder on shared storage
  (`Android/data/in.dragonbra.vapulla/files/steamapps/common`), which Android removes when the
  app is uninstalled. You can pick a folder of your own instead. That needs Android's
  "access to all files", which the app asks for only then and uses only for that folder;
  games in a folder of your own stay when the app is uninstalled.
- The password of a private branch you download is kept in the local database, so the
  download can be resumed and updated.
- The network log keeps message types, sizes and headers in memory only. Message contents are
  kept only after you turn on "Keep packet contents" in Settings, never for sign-in messages
  or the auth tickets in use, and are gone when the log is cleared or the app stops.
- An auth ticket made under Under the Hood is never saved, logged or copied. It is cancelled
  when you leave the screen or make another.
- The Account screen shows your wallet balance and email address as Steam sends them to its
  own client. They are held in memory only and are gone when you sign out.
- "Open signed in, inside the app" in Settings is off by default. Turned on, Steam's community,
  store and help pages open in a web view inside the app, signed in with an access token made
  from your saved sign-in. The token is given to those three sites only, not to the checkout,
  and is removed from the web view when you leave the page. Links in chat messages and other
  text written by other people never open there; they go to your browser as before.
- The app talks to Steam and the CDNs and storage Steam uses, with one exception: when a chat
  shows a YouTube link, the video's thumbnail, name and channel are fetched from YouTube. It
  has no analytics and no server of its own.

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

Issues and pull requests are welcome. Please run `./gradlew lintKotlin` and
`./gradlew :app:testDebugUnitTest` before opening a pull request, and say how the change was
tested. If AI tools wrote part of a contribution, mention it in the pull request.

## Credits and license

Vapulla was started by Long Tran, the original author of JavaSteam. Both JavaSteam and this
fork of Vapulla are now maintained by LossyDragon.

Licensed under the Apache License 2.0, see [LICENSE](LICENSE). Licenses of bundled third-party
work are in [third_party.txt](third_party.txt).
