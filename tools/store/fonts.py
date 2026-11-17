"""Fonts of the store graphics and the website: Noto Sans, plus a Noto font for the scripts it lacks.

Store locale -> (CSS family, Google Fonts query name). Locales not listed only need Noto Sans.
"""

FONT = {
    "ar": ("Noto Sans Arabic", "Noto+Sans+Arabic"), "hi-IN": ("Noto Sans Devanagari", "Noto+Sans+Devanagari"),
    "ja-JP": ("Noto Sans JP", "Noto+Sans+JP"), "ko-KR": ("Noto Sans KR", "Noto+Sans+KR"),
    "zh-CN": ("Noto Sans SC", "Noto+Sans+SC"),
}


def font(locale: str) -> tuple[str, str]:
    return FONT.get(locale, ("Noto Sans", "Noto+Sans"))
