# Architecture

## Tech stack

| Concern  | Choice |
|----------|--------|
| Language | Kotlin |
| UI       | Jetpack Compose + Material 3, custom neumorphic palette (light & dark, Material You on Android 12+) |
| Widgets  | Jetpack Glance |
| Storage  | Room (SQLite) |
| Images   | Coil (local files only), Android photo picker (no permission needed) |
| Icons    | [Lucide](https://lucide.dev) (ISC), bundled as path data in `assets/lucide/icons.tsv` (see [Icons](Icons.md)) |
| Build    | Gradle (Kotlin DSL, version catalog), AGP 9 |
| Min / target SDK | 26 / 37 |

## Project layout

```
app/src/main/java/dev/cfaz/timeclicker/
├── TimeClickerApplication.kt  # creates the AppContainer
├── AppContainer.kt            # manual dependency injection
├── MainActivity.kt            # Home <-> Groups screen switch
├── data/
│   ├── Tracker.kt             # domain model: tiles, groups, colour/size enums, icon (a Lucide name)
│   ├── Reminder.kt            # a tile's reminder: automatic, or after how long without being done
│   ├── Rhythm.kt              # how often a tile is usually done, guessed from its history
│   ├── IconCatalog.kt         # the bundled Lucide icons: path data, categories, search tags
│   ├── TrackerRepository.kt   # the single entry point the UI uses
│   ├── PhotoStore.kt          # copies picked photos into private storage
│   └── local/                 # Room: entities, DAO, database + migrations
└── ui/
    ├── components/            # neumorphic buttons, text field, segmented control, tablet gutters
    ├── home/                  # home screen (group pager), bento grid, tile, edit sheet, ViewModel
    ├── groups/                # manage groups screen
    ├── icons/                 # icon palette screen, full icon chooser, icon and category names
    ├── settings/              # settings screen, credits
    ├── theme/                 # palette (+ Material You), neumorphic shadow modifiers, icons
    └── time/                  # "2 minutes 45 seconds" formatting + ticking clock
├── reminder/                  # reminder notifications: one alarm for the next due tile, "Mark as done"
└── widget/                    # home-screen widgets (Glance): widget, tile picker, refresh alarm
app/src/main/assets/lucide/    # icons.tsv (generated) and the Lucide licence
app/src/main/assets/licenses/  # licence texts shown in the credits
app/schemas/                   # exported Room schemas (commit these, they back migrations)
tools/lucide/generate_icons.py # regenerates icons.tsv from the latest Lucide release
tools/store/                   # captures and frames the Play Store screenshots (see Store listing)
fastlane/metadata/android/     # the Play Store listing: text and graphics, in every language
wiki/                          # this documentation, copied to the GitHub wiki
docs/                          # the website (timeclicker.cfaz.dev), privacy policy included
```

## Data model

- `tracker_groups`: user-defined groups (name, display position).
- `trackers`: one row per tile (name, group, colour, icon, size, photo file, display position,
  `count_since`: the press counter counts events after this time, and the reminder: `reminder_every` +
  `reminder_unit`, null for none; `reminder_unit` is `auto` for an automatic reminder).
  Deleting a group sets its tiles' group to null (they then only show under "All").
- `tracker_events`: one row per time a tile was done.

A card's "last time" is its most recent event. Resetting a card inserts an event, and tapping it again to undo deletes it.
Because the full history is kept, features like automatic reminders (and later a history view or stats)
use data that is already there.

## Automatic reminders

`data/Rhythm.kt` guesses how often a tile is done from its last 11 presses: the usual gap is the median gap
between presses (gaps under a minute are double taps and don't count). It needs at least 5 presses, with gaps
regular enough: their median deviation at most half the usual gap. Until then, an automatic reminder waits.
It comes once the tile is late by a quarter of its usual gap, or by 3 median deviations for a habit that varies
more; due between 22:00 and 08:00, it waits for 08:00. Switching a tile to Custom starts from that delay.

## Extending the database

1. Change the entities and bump `version` in `AppDatabase`.
2. Build. Room exports the new schema to `app/schemas/`.
3. Add a migration (see the `AutoMigration`s in `AppDatabase`) so existing user data is kept.
4. Extend `MigrationTest` and run it on a device or emulator: `./gradlew connectedDebugAndroidTest`.
