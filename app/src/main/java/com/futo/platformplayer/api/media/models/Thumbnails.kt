package com.futo.platformplayer.api.media.models

import com.caoccao.javet.values.reference.V8ValueArray
import com.caoccao.javet.values.reference.V8ValueObject
import com.futo.platformplayer.engine.IV8PluginConfig
import com.futo.platformplayer.getOrThrow

@kotlinx.serialization.Serializable
class Thumbnails {
    val sources : Array<Thumbnail>;

    constructor() { sources = arrayOf(); }
    constructor(thumbnails : Array<Thumbnail>) {
        sources = thumbnails.filter {it.url != null} .sortedBy { it.quality }.toTypedArray();
    }

    fun getHQThumbnail(alternative: Boolean = false) : String? {
        var result : Thumbnail? = null;

        if (alternative) {
            result = sources.reduceOrNull { acc, cur ->  if (cur.type === ThumbnailType.ALTERNATIVE && cur.quality > acc.quality) cur else acc }
        }

        if (result == null) {
            result = sources.reduceOrNull { acc, cur ->  if (cur.type === ThumbnailType.MAIN && cur.quality > acc.quality) cur else acc }
        }

        return result?.url;
    }
    fun getLQThumbnail(alternative: Boolean = false) : String? {
        var result : Thumbnail? = null;

        if (alternative) {
            result = sources.reduceOrNull { acc, cur ->  if (cur.type === ThumbnailType.ALTERNATIVE && cur.quality < acc.quality) cur else acc }
        }

        if (result == null) {
            result = sources.reduceOrNull { acc, cur ->  if (cur.type === ThumbnailType.MAIN && cur.quality < acc.quality) cur else acc }
        }

        return result?.url;
    }
    fun getMinimumThumbnail(quality: Int, alternative: Boolean = false): String? {
        return sources.firstOrNull { it.quality >= quality }?.url ?: getHQThumbnail(alternative);
    }

    fun hasMultiple() = sources.size > 1;

    fun hasAlternative() = sources.any { it.type == ThumbnailType.ALTERNATIVE };

    companion object {
        fun fromV8(config: IV8PluginConfig, value: V8ValueObject): Thumbnails {
            return Thumbnails((value.getOrThrow<V8ValueArray>(config, "sources", "Thumbnails"))
                .toArray()
                .map { Thumbnail.fromV8(it as V8ValueObject) }
                .toTypedArray());
        }
    }
}
@kotlinx.serialization.Serializable
data class Thumbnail(val url : String?, val quality : Int = 0, val type : ThumbnailType = ThumbnailType.MAIN) {

    companion object {
        fun fromV8(value: V8ValueObject): Thumbnail {
            return Thumbnail(
                value.getString("url"),
                value.getInteger("quality"),
                if (value.has("type")) ThumbnailType.fromValue(value.getString("type"))!! else ThumbnailType.MAIN
            );
        }
    }
};