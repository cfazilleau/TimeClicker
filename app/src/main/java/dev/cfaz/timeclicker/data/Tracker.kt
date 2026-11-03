package dev.cfaz.timeclicker.data

import java.time.Instant

data class Tracker(
    val id: Long,
    val name: String,
    val lastDoneAt: Instant,
    val groupId: Long?,
    val color: TileColor,
    val icon: TileIcon,
    val size: TileSize,
    /** File name inside the app's photo folder, or null for a colour tile. */
    val photo: String?,
    /** Times the tile was pressed since it was created or its counter was reset. */
    val pressCount: Int = 0,
    val reminder: Reminder? = null,
    val createdAt: Instant = lastDoneAt,
    /** This tile's own time display; null follows [AppSettings.timeDisplay]. */
    val timeDisplay: TimeDisplay? = null,
) {
    /**
     * Whether the tile was pressed since it was created. Until then [lastDoneAt] is the tile's creation: it still dates
     * reminders, but no elapsed time is shown.
     */
    val hasPressed: Boolean get() = lastDoneAt > createdAt
}

data class TrackerGroup(
    val id: Long,
    val name: String,
    val icon: TileIcon = TileIcon.NONE,
)

/** Everything the user can edit on a tile. */
data class TileSpec(
    val name: String,
    val groupId: Long?,
    val color: TileColor,
    val icon: TileIcon,
    val size: TileSize,
    val photo: String?,
    val reminder: Reminder? = null,
    val timeDisplay: TimeDisplay? = null,
)

/**
 * A tile's colour: one of the [PresetColor]s, or a [CustomColor] picked from the hue spectrum. Stored by [key], in the
 * database and backups alike, so custom colours needed no new column.
 */
sealed interface TileColor {
    val key: String

    companion object {
        /** Unknown keys (e.g. from a newer app version) fall back to [PresetColor.SAGE]. */
        fun fromKey(key: String?): TileColor = CustomColor.fromKey(key) ?: PresetColor.fromKey(key)
    }
}

enum class PresetColor(override val key: String) : TileColor {
    SAGE("sage"), LAVENDER("lavender"), PEACH("peach"), SKY("sky"), BUTTER("butter"), ROSE("rose"),
    TEAL("teal"), ORCHID("orchid"), CORAL("coral"), SLATE("slate"), LIME("lime"), SAND("sand"),

    /** Material You: follow the system colours on Android 12+. */
    PRIMARY("primary"), SECONDARY("secondary"), TERTIARY("tertiary");

    companion object {
        /**
         * The colours offered when editing a tile, by hue then the neutrals. Rose and teal made room for custom
         * colours, and system colours are no longer offered either; tiles using them keep them.
         */
        val pickable: List<PresetColor> = listOf(CORAL, PEACH, BUTTER, LIME, SAGE, SKY, LAVENDER, ORCHID, SAND, SLATE)

        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: SAGE
    }
}

/** How light a [CustomColor] is. Each level has a light-theme and a dark-theme shade. */
enum class ColorLevel(val key: String) {
    LIGHT("light"), NATURAL("natural"), DARK("dark");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: NATURAL
    }
}

/** A colour from the picker: a [hue] in degrees (0 until 360) at one of three [level]s. */
data class CustomColor(val hue: Int, val level: ColorLevel) : TileColor {
    init {
        require(hue in 0 until 360) { "Hue out of range: $hue" }
    }

    override val key: String get() = "$PREFIX$hue:${level.key}"

    companion object {
        private const val PREFIX = "custom:"

        /** Offered until a custom colour is used: the pink that rose was. */
        val DEFAULT = CustomColor(350, ColorLevel.NATURAL)

        /** Parses "custom:<hue>:<level>"; null for any other key. */
        fun fromKey(key: String?): CustomColor? {
            if (key == null || !key.startsWith(PREFIX)) return null
            val parts = key.removePrefix(PREFIX).split(':')
            val hue = parts[0].toIntOrNull() ?: return null
            return CustomColor(hue.mod(360), ColorLevel.fromKey(parts.getOrNull(1)))
        }
    }
}

enum class TileSize(val key: String, val columns: Int, val rows: Int) {
    SMALL("small", 1, 1), WIDE("wide", 2, 1), TALL("tall", 1, 2);

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: SMALL
    }
}

/**
 * A tile's icon: the name of a Lucide icon ("circle-check"), or [NONE]. Stored by [key].
 * A name missing from the bundled icon set (e.g. from a newer app version) shows no icon.
 */
@JvmInline
value class TileIcon(val key: String) {
    val isNone: Boolean get() = this == NONE

    companion object {
        /** No icon: the tile shows only its text. */
        val NONE = TileIcon("none")
        val DEFAULT = TileIcon("circle-check")

        /**
         * The icons offered when editing a tile, until the user picks their own palette: the most common things to
         * track, eleven so that with "No icon" they fill two rows of six.
         */
        val defaultPalette: List<TileIcon> = listOf(
            "sprout", "shopping-cart", "broom", "trash", "bed", "pill", "scissors", "dumbbell", "phone",
            "paw-print", "car",
        ).map(::TileIcon)

        /**
         * Before database version 4 (backup format 2), tiles stored the app's own icon names. Those that aren't
         * also the Lucide name of the icon that replaced them map to it. Some ("check", "flower") are other Lucide
         * icons too, so the mapping is only for old data: the database migration and old backups.
         */
        val legacyKeys: Map<String, String> = mapOf(
            "check" to "circle-check", "drop" to "droplet", "grass" to "sprout", "flower" to "flower-2",
            "snow" to "snowflake", "cart" to "shopping-cart", "paw" to "paw-print", "brush" to "paintbrush",
        )

        fun fromKey(key: String?): TileIcon = if (key.isNullOrBlank()) DEFAULT else TileIcon(key)

        /** An icon name stored before database version 4 / backup format 2. */
        fun fromLegacyKey(key: String?): TileIcon = fromKey(key?.let { legacyKeys[it] ?: it })
    }
}
