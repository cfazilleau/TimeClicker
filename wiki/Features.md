# Features

Time Clicker tracks *when you last did something*.
Each tile ("Watered the plants") shows how long ago it was last done, as its two largest units
("37 seconds", "2 minutes 45 seconds", "3 days 5 hours", "1 year 12 days"), ticking live.
Tap a tile when you do it again and it resets to now: the time disappears, then fades back in after a second.
The number in the tile's bottom-right corner counts how many times it was pressed.

## Using the app

- **Tap** a tile: mark it as done now. For the next 10 seconds the tile offers to undo it
  ("Tap again to undo", with a ring counting down in its corner): tapping it again undoes the press.
  Widgets do the same (before Android 12 their ring steps once a second), and a press on one can be undone on the other.
- **Long-press** a tile: edit it in a sheet: name, group, colour or photo background, icon (or none; your icon palette, plus a searchable list of every icon with the recently used ones first),
  size (Small / Wide / Tall), undo its last press or reset its press counter, a reminder, add it to the home screen, or delete it.
  All sizes show the same content.
- **+** button (bottom centre): add a new tile, pre-filed in the group currently shown.
- **Group chips**, or **swiping left/right**, move between groups; "All" shows the tiles without a group,
  then one section per group.
  The sliders button opens **Groups**: add, rename, delete, drag to reorder.

## Reminders

A tile can notify you when it hasn't been done for a while.

- **Automatic**: when it's late on how often you usually do it ("You usually do it every 8 hours. Did you forget?"),
  guessed from its history, never at night. How it's guessed: [Architecture, Automatic reminders](Architecture.md#automatic-reminders).
- **Custom**: N hours, days or weeks since its last tap.

It notifies once; tapping the tile (or "Mark as done" on the notification) starts the wait over.
Tiles with a reminder show a bell in their top-right corner.

## Widgets

Any tile can live on the home screen (from the widget picker, or "Add to home screen" in its edit sheet).
Tapping the widget marks it as done (with the click sound); its icon opens the app.

## Settings

The gear button, next to the title, opens the settings: theme (system / light / dark), system colours, time shown as
"3 days ago" or as a date and time, vibration, click sound, press counter, **icon palette**, groups, language,
**export / import** of everything (tiles, groups, history, photos, settings) as one `.zip` file,
and a **Credits** page for the open-source projects the app uses, with their licences.

The **icon palette** lists every [Lucide](https://lucide.dev) icon (about 1,900), by category, with search
(in English, and in French when the app is in French: "poubelle", "anniversaire", "lave-linge").
Tap icons to add them to, or remove them from, the icons offered when editing a tile.
More on the icons: [Icons](Icons.md).

## Look

The look is a neumorphic "bento" grid: soft pastel tiles raised off the page that sink in when pressed,
and glide to their new place when the grid changes. Screens slide in and out, and switching the theme
fades the colours instead of flashing. Light and dark themes follow the system unless
picked in the settings. On Android 12+ the app can follow the system's **Material You** colours:
surfaces and accents, with the pastel tile colours nudged toward their hue to match.

On **tablets**, in landscape and in split screen, the grid gets as many columns as fit (two on a phone,
four on a tablet held upright, seven across a landscape tablet), so tiles keep their phone size.
Settings, Groups, Credits and the icon palette stay a readable width, centred.

## Languages

Translated into English, French, Spanish, German, Italian, Portuguese, Dutch, Polish, Russian, Turkish,
Indonesian, Arabic, Hindi, Japanese, Korean and Simplified Chinese. On Android 13+ the language can be
picked per app in the system settings.

## Privacy

Fully offline. All data lives in a local SQLite database in the app's private storage;
tile photos are copied into the app's private storage too.
The privacy policy is on the website: <https://timeclicker.cfaz.dev/privacy.html>.
