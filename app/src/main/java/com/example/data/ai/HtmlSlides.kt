package com.example.data.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds a REAL slide deck (one slide per screen, swipe / arrow keys) from structured JSON.
 * The app owns the layout, so photos can never overlap, overflow or get stuck at random places.
 * Images are written as <img data-q="search words"> or <img data-file="inbox file"> and are
 * downloaded/copied by ArtifactBuilder before the card is shown.
 */
object HtmlSlides {
    private class Theme(val bg: String, val ink: String, val acc: String, val card: String)

    private val THEMES = mapOf(
        "paper" to Theme("#F4EFE6", "#141414", "#C2410C", "#E8E1D3"),
        "olive" to Theme("#EFEADB", "#1B1F14", "#4B5D23", "#E3DCC5"),
        "blue" to Theme("#EEF2F7", "#0F1B2D", "#1D4ED8", "#DDE5F0"),
        "red" to Theme("#F6EFE9", "#1A1212", "#B91C1C", "#EBDDD2"),
        "dark" to Theme("#14161B", "#ECE8DF", "#E8A33D", "#23262E")
    )

    private fun esc(s: String?): String =
        (s ?: "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /** *word* -> highlighted word */
    private fun rich(s: String?): String = esc(s)
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "<b>$1</b>")
        .replace(Regex("\\*([^*]+)\\*"), "<em>$1</em>")

    private fun JSONObject.s(k: String): String = optString(k, "").trim()

    fun build(title: String, slides: JSONArray, themeName: String, accentHex: String): String {
        val t = THEMES[themeName.lowercase()] ?: THEMES["paper"]!!
        val acc = if (Regex("^#[0-9a-fA-F]{6}$").matches(accentHex.trim())) accentHex.trim() else t.acc
        val sb = StringBuilder()
        val total = slides.length()
        for (i in 0 until total) {
            val o = slides.optJSONObject(i) ?: continue
            sb.append(slide(o, i, total))
        }
        return """<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>${esc(title)}</title>
<style>
:root{--bg:${t.bg};--ink:${t.ink};--acc:$acc;--card:${t.card}}
*{box-sizing:border-box}html,body{margin:0;height:100%;background:var(--bg);color:var(--ink);font-family:system-ui,-apple-system,"Segoe UI",Roboto,Helvetica,Arial,sans-serif}
#deck{display:flex;height:100%;overflow-x:auto;overflow-y:hidden;scroll-snap-type:x mandatory;-webkit-overflow-scrolling:touch}
.slide{flex:0 0 100%;height:100%;scroll-snap-align:start;padding:8vh 6vw 9vh;display:flex;flex-direction:column;justify-content:center;gap:2.4vh;overflow-y:auto;position:relative}
h1,h2{margin:0;line-height:1.06;font-weight:800;letter-spacing:-.02em}
h1{font-size:clamp(34px,min(9vw,13vh),110px)}h2{font-size:clamp(26px,min(6vw,8.5vh),70px)}
.sub{margin:0;font-size:clamp(16px,min(2.6vw,3.6vh),32px);opacity:.75;line-height:1.35}
em{font-style:normal;color:var(--acc)}b{font-weight:800}
p{margin:0;font-size:clamp(15px,min(2.4vw,3.3vh),30px);line-height:1.45}
ul{margin:0;padding-left:1.1em;display:flex;flex-direction:column;gap:1.3vh;font-size:clamp(15px,min(2.4vw,3.3vh),30px);line-height:1.38}
li::marker{color:var(--acc)}
.tag{position:absolute;top:2.6vh;left:6vw;font-size:12px;letter-spacing:.14em;text-transform:uppercase;font-weight:700;opacity:.6}
.cols{display:grid;grid-template-columns:1.15fr .85fr;gap:4vw;align-items:center;min-height:0}
.l-image_left .cols{grid-template-columns:.85fr 1.15fr}
.txt{display:flex;flex-direction:column;gap:2vh;min-width:0}
.ph{margin:0;position:relative;overflow:hidden;border:3px solid var(--ink);border-radius:16px;background:var(--card);aspect-ratio:4/3;max-height:58vh;width:100%}
.ph img{position:absolute;left:0;top:0;width:100%;height:100%;object-fit:cover;display:block}
.ph figcaption{position:absolute;left:0;right:0;bottom:0;padding:6px 10px;font-size:12px;background:rgba(0,0,0,.6);color:#fff}
@media (max-aspect-ratio:1/1){.cols,.l-image_left .cols{grid-template-columns:1fr}.ph{aspect-ratio:16/10;max-height:32vh}.l-image_left .ph{order:2}}
.stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(130px,1fr));gap:3vw}
.stat{border-top:4px solid var(--ink);padding-top:1vh}.stat b{display:block;font-size:clamp(34px,min(8vw,12vh),100px);line-height:1;color:var(--acc)}.stat span{font-size:clamp(13px,2vw,24px)}
.tl{display:flex;flex-direction:column}.tl div{display:grid;grid-template-columns:clamp(70px,14vw,170px) 1fr;gap:3vw;border-bottom:2px solid var(--ink);padding:1vh 0;align-items:baseline;font-size:clamp(14px,2.2vw,26px)}.tl b{font-size:clamp(20px,4vw,44px);color:var(--acc);line-height:1}
.bars{display:flex;flex-direction:column;gap:1.6vh}.bar{display:grid;grid-template-columns:30% 1fr 14%;gap:2vw;align-items:center;font-size:clamp(13px,2.1vw,26px)}.bar i{display:block;height:3.4vh;min-height:16px;background:var(--acc);border:3px solid var(--ink)}.bar span:last-child{font-weight:800}
table{border-collapse:collapse;width:100%;font-size:clamp(13px,2vw,24px)}th{text-align:left;border-bottom:4px solid var(--ink);padding:6px 8px 6px 0;text-transform:uppercase;font-size:.7em;letter-spacing:.1em}td{border-bottom:2px solid var(--ink);padding:8px 8px 8px 0;vertical-align:top}
blockquote{margin:0;font-size:clamp(24px,min(5.5vw,7.5vh),64px);line-height:1.2;font-weight:700}
.two{display:grid;grid-template-columns:1fr 1fr;gap:4vw}.two h2{font-size:clamp(20px,3.6vw,40px);margin-bottom:1vh;color:var(--acc)}@media (max-aspect-ratio:1/1){.two{grid-template-columns:1fr}}
.l-image_full{color:#fff;justify-content:flex-end}.l-image_full .ph{position:absolute;left:0;top:0;right:0;bottom:0;max-height:none;aspect-ratio:auto;border:0;border-radius:0;width:auto}.l-image_full .ph::after{content:"";position:absolute;left:0;top:0;right:0;bottom:0;background:linear-gradient(to top,rgba(0,0,0,.8),rgba(0,0,0,.15) 65%)}.l-image_full .txt{position:relative;z-index:2}.l-image_full em{color:#FFD08A}
.l-title{justify-content:center}.l-closing{justify-content:center}
#cnt{position:fixed;right:5vw;bottom:calc(8px + env(safe-area-inset-bottom,0px));font-size:13px;font-weight:700;opacity:.6;pointer-events:none}
</style></head><body><div id="deck">
$sb</div><div id="cnt"></div>
<script>
(function(){var d=document.getElementById('deck'),c=document.getElementById('cnt'),n=d.children.length;
function u(){var i=Math.round(d.scrollLeft/Math.max(d.clientWidth,1))+1;c.textContent=i+' / '+n}
d.addEventListener('scroll',u,{passive:true});u();
document.addEventListener('keydown',function(e){if(e.key==='ArrowRight')d.scrollBy({left:d.clientWidth,behavior:'smooth'});if(e.key==='ArrowLeft')d.scrollBy({left:-d.clientWidth,behavior:'smooth'})});})();
</script></body></html>"""
    }

    private fun img(o: JSONObject): String {
        val q = o.s("image_query")
        val f = o.s("image_file")
        if (q.isBlank() && f.isBlank()) return ""
        val alt = esc(o.s("caption").ifBlank { o.s("title").ifBlank { q.ifBlank { f } } })
        val attr = if (f.isNotBlank()) "data-file=\"${esc(f)}\"" else "data-q=\"${esc(q)}\""
        val cap = o.s("caption").let { if (it.isNotBlank()) "<figcaption>${esc(it)}</figcaption>" else "" }
        return "<figure class=\"ph\"><img $attr alt=\"$alt\">$cap</figure>"
    }

    private fun bullets(o: JSONObject, key: String = "bullets"): String {
        val a = o.optJSONArray(key) ?: return ""
        val sb = StringBuilder("<ul>")
        for (i in 0 until a.length()) {
            val v = a.opt(i)
            when (v) {
                is JSONObject -> sb.append("<li><b>").append(rich(v.s("head").ifBlank { v.s("label") })).append("</b> ").append(rich(v.s("text"))).append("</li>")
                else -> sb.append("<li>").append(rich(v?.toString())).append("</li>")
            }
        }
        return sb.append("</ul>").toString()
    }

    private fun body(o: JSONObject): String {
        val t = o.s("text")
        return (if (t.isNotBlank()) "<p>${rich(t)}</p>" else "") + bullets(o)
    }

    private fun slide(o: JSONObject, idx: Int, total: Int): String {
        var layout = o.s("layout").lowercase().replace("-", "_")
        val hasImg = o.s("image_query").isNotBlank() || o.s("image_file").isNotBlank()
        if (layout.isBlank()) {
            layout = when {
                idx == 0 -> "title"
                o.has("stats") -> "stats"
                o.has("quote") -> "quote"
                o.has("rows") -> "table"
                o.has("bars") -> "bars"
                o.has("items") -> "timeline"
                hasImg -> "image_right"
                else -> "content"
            }
        }
        val title = o.s("title")
        val tag = o.s("section").let { if (it.isNotBlank()) "<div class=\"tag\">${esc(it)}</div>" else "" }
        val h2 = if (title.isNotBlank()) "<h2>${rich(title)}</h2>" else ""
        val inner: String = when (layout) {
            "title", "cover" -> {
                val txt = "<div class=\"txt\"><h1>${rich(title)}</h1>" +
                    (if (o.s("subtitle").isNotBlank()) "<p class=\"sub\">${rich(o.s("subtitle"))}</p>" else "") + body(o) + "</div>"
                if (hasImg) "<div class=\"cols\">$txt${img(o)}</div>" else txt
            }
            "closing" -> "<h1>${rich(title.ifBlank { "Thank you" })}</h1>" +
                (if (o.s("subtitle").isNotBlank()) "<p class=\"sub\">${rich(o.s("subtitle"))}</p>" else "") + body(o)
            "image_left", "image_right" ->
                if (hasImg) "$h2<div class=\"cols\"><div class=\"txt\">${body(o)}</div>${img(o)}</div>" else "$h2${body(o)}"
            "image_full" ->
                if (hasImg) "${img(o)}<div class=\"txt\">${h2}${body(o)}</div>" else "$h2${body(o)}"
            "stats" -> {
                val a = o.optJSONArray("stats") ?: JSONArray()
                val sb = StringBuilder("$h2${if (o.s("text").isNotBlank()) "<p>${rich(o.s("text"))}</p>" else ""}<div class=\"stats\">")
                for (i in 0 until a.length()) {
                    val s = a.optJSONObject(i) ?: continue
                    sb.append("<div class=\"stat\"><b>${esc(s.s("value"))}</b><span>${rich(s.s("label"))}</span></div>")
                }
                sb.append("</div>").toString()
            }
            "timeline" -> {
                val a = o.optJSONArray("items") ?: JSONArray()
                val sb = StringBuilder("$h2<div class=\"tl\">")
                for (i in 0 until a.length()) {
                    val s = a.optJSONObject(i) ?: continue
                    sb.append("<div><b>${esc(s.s("label").ifBlank { s.s("big") })}</b><span>${rich(s.s("text"))}</span></div>")
                }
                sb.append("</div>").toString()
            }
            "bars" -> {
                val a = o.optJSONArray("bars") ?: JSONArray()
                var max = 0.0
                for (i in 0 until a.length()) max = maxOf(max, a.optJSONObject(i)?.optDouble("value", 0.0) ?: 0.0)
                if (max <= 0.0) max = 1.0
                val unit = esc(o.s("unit"))
                val sb = StringBuilder("$h2<div class=\"bars\">")
                for (i in 0 until a.length()) {
                    val s = a.optJSONObject(i) ?: continue
                    val v = s.optDouble("value", 0.0).coerceAtLeast(0.0)
                    val pct = (v / max * 100).coerceIn(2.0, 100.0).toInt()
                    val vs = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
                    sb.append("<div class=\"bar\"><span>${esc(s.s("label"))}</span><i style=\"width:$pct%\"></i><span>$vs$unit</span></div>")
                }
                sb.append("</div>").toString()
            }
            "table" -> {
                val h = o.optJSONArray("header") ?: JSONArray()
                val r = o.optJSONArray("rows") ?: JSONArray()
                val sb = StringBuilder("$h2<div style=\"overflow-x:auto\"><table><tr>")
                for (i in 0 until h.length()) sb.append("<th>${esc(h.optString(i))}</th>")
                sb.append("</tr>")
                for (i in 0 until r.length()) {
                    val row = r.optJSONArray(i) ?: continue
                    sb.append("<tr>")
                    for (j in 0 until row.length()) sb.append("<td>${rich(row.optString(j))}</td>")
                    sb.append("</tr>")
                }
                sb.append("</table></div>").toString()
            }
            "quote" -> "<blockquote>“${esc(o.s("quote").ifBlank { o.s("text") })}”</blockquote>" +
                (if (o.s("author").isNotBlank()) "<p class=\"sub\">— ${esc(o.s("author"))}</p>" else "")
            "two_column" -> {
                fun col(k: String): String {
                    val c = o.optJSONObject(k) ?: return "<div></div>"
                    return "<div><h2>${rich(c.s("title"))}</h2>${(if (c.s("text").isNotBlank()) "<p>${rich(c.s("text"))}</p>" else "")}${bullets(c)}</div>"
                }
                "$h2<div class=\"two\">${col("left")}${col("right")}</div>"
            }
            else -> if (hasImg) "$h2<div class=\"cols\"><div class=\"txt\">${body(o)}</div>${img(o)}</div>" else "$h2${body(o)}"
        }
        return "<section class=\"slide l-$layout\">$tag$inner</section>\n"
    }
}
