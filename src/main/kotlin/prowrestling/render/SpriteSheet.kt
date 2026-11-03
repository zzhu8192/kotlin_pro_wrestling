package prowrestling.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion

class SpriteSheet(val characterId: String, val texture: Texture) {
    val cols: Int = 10
    val rows: Int = 10
    val frameWidth: Int = texture.width / cols
    val frameHeight: Int = texture.height / rows

    private val regions: Array<TextureRegion> = Array(cols * rows) { idx ->
        val c = idx % cols
        val r = idx / cols
        TextureRegion(texture, c * frameWidth, r * frameHeight, frameWidth, frameHeight)
    }

    fun getFrame(index: Int): TextureRegion {
        val safeIdx = Math.max(0, Math.min(index, regions.size - 1))
        return regions[safeIdx]
    }

    fun dispose() {
        texture.dispose()
    }

    companion object {
        private val cache = mutableMapOf<String, SpriteSheet>()
        private val failedSheets = mutableSetOf<String>()

        fun load(characterId: String): SpriteSheet? {
            if (cache.containsKey(characterId)) {
                return cache[characterId]
            }
            if (failedSheets.contains(characterId)) {
                return null
            }

            return try {
                val filename = when (characterId) {
                    "referee" -> "assets/sprites/atlas/referee.png"
                    else -> "assets/sprites/atlas/wrestler_$characterId.png"
                }

                val fileHandle = if (Gdx.files != null && Gdx.files.internal(filename).exists()) {
                    Gdx.files.internal(filename)
                } else if (Gdx.files != null && Gdx.files.classpath(filename).exists()) {
                    Gdx.files.classpath(filename)
                } else if (Gdx.files != null && Gdx.files.internal("assets/sprites/atlas/wrestler_base_sheet.png").exists()) {
                    Gdx.files.internal("assets/sprites/atlas/wrestler_base_sheet.png")
                } else {
                    null
                }

                if (fileHandle != null) {
                    val texture = Texture(fileHandle)
                    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                    val sheet = SpriteSheet(characterId, texture)
                    cache[characterId] = sheet
                    sheet
                } else {
                    failedSheets.add(characterId)
                    null
                }
            } catch (e: Throwable) {
                System.err.println("[SpriteSheet] Could not load sprite for '$characterId': ${e.message}")
                failedSheets.add(characterId)
                null
            }
        }

        fun disposeAll() {
            cache.values.forEach { it.dispose() }
            cache.clear()
            failedSheets.clear()
        }
    }
}
