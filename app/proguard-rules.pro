# GlyphBoard release shrink rules.
#
# The IME service is referenced only from AndroidManifest.xml / method.xml,
# so keep it (and its metadata) explicitly.
-keep class io.github.ranzlappen.glyphboard.ime.GlyphBoardService { *; }

# Nothing else needs special handling: no reflection, no serialization,
# no JNI, no networking.
