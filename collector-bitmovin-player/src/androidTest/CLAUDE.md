# Bitmovin Player system tests

Create every player through `DestroyPlayersRule`:

```kotlin
@get:Rule
val players = DestroyPlayersRule()

val player = players.track(Player.create(appContext, playerConfig))
```

All system tests share one 192 MB process. A player that a test leaves alive keeps its buffers (~22–32 MB) until the suite crashes with `OutOfMemoryError` (AN-5980). The rule destroys any tracked player still alive after the test, whether it passed or failed, so no `@After` cleanup is needed.
