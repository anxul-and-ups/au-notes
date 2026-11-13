package com.example.data.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds a REAL slide deck (one slide per screen, swipe / arrow keys / Prev & Next buttons)
 * from structured JSON.
 * The app owns the layout, so photos never overlap, stretch full-screen, or render upside-down.
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

    /** *word* -> highlighted word, ==word== -> highlighted badge */
    private fun rich(s: String?): String = esc(s)
        .replace(Regex("==([^=]+)=="), "<em>$1</em>")
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
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, viewport-fit=cover">
<title>${esc(title)}</title>
<style>
:root{--bg:${t.bg};--ink:${t.ink};--acc:$acc;--card:${t.card}}
*{box-sizing:border-box}
html,body{margin:0;padding:0;width:100%;height:100vh;min-height:230px;overflow:hidden;background:var(--bg);color:var(--ink);font-family:system-ui,-apple-system,"Segoe UI",Roboto,Helvetica,Arial,sans-serif}
#deck{display:flex;width:100vw;height:100vh;min-height:230px;overflow-x:auto;overflow-y:hidden;scroll-snap-type:x mandatory;-webkit-overflow-scrolling:touch;scroll-behavior:smooth}
#deck::-webkit-scrollbar{display:none}
.slide{flex:0 0 100vw;width:100vw;height:100vh;min-height:230px;scroll-snap-align:start;padding:38px 20px 54px;display:flex;flex-direction:column;justify-content:center;gap:12px;overflow-y:auto;position:relative;border-right:1px solid rgba(0,0,0,.06)}
h1,h2{margin:0;line-height:1.12;font-weight:800;letter-spacing:-.02em}
h1{font-size:clamp(22px,5.5vw,48px)}
h2{font-size:clamp(18px,4.5vw,34px)}
.sub{margin:0;font-size:clamp(13px,2.8vw,20px);opacity:.78;line-height:1.38}
em{font-style:normal;color:var(--acc);background:rgba(194,65,12,.1);padding:1px 6px;border-radius:5px;font-weight:700}
b{font-weight:800}
p{margin:0;font-size:clamp(13px,2.6vw,18px);line-height:1.48}
ul{margin:0;padding-left:1.2em;display:flex;flex-direction:column;gap:8px;font-size:clamp(13px,2.6vw,18px);line-height:1.42}
li::marker{color:var(--acc);font-weight:800}
.tag{position:absolute;top:12px;left:20px;right:20px;display:flex;justify-content:space-between;align-items:center;font-size:10px;letter-spacing:.14em;text-transform:uppercase;font-weight:800;opacity:.68;border-bottom:2px solid var(--ink);padding-bottom:5px}
.cols{display:grid;grid-template-columns:1.15fr .85fr;gap:16px;align-items:center;min-height:0;width:100%}
.l-image_left .cols{grid-template-columns:.85fr 1.15fr}
.txt{display:flex;flex-direction:column;gap:10px;min-width:0}
.ph{margin:0;position:relative;overflow:hidden;border:2.5px solid var(--ink);border-radius:14px;background:var(--card);width:100%;height:clamp(130px,36vh,280px);display:flex;align-items:center;justify-content:center}
.ph img{width:100%;height:100%;object-fit:contain;object-position:center;display:block;padding:4px}
.ph figcaption{position:absolute;left:0;right:0;bottom:0;padding:4px 8px;font-size:10px;background:rgba(20,20,20,.78);color:#fff;text-align:center;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
@media (max-width:540px){.cols,.l-image_left .cols{grid-template-columns:1fr}.ph{height:clamp(115px,26vh,190px)}.l-image_left .ph{order:2}}
.stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(105px,1fr));gap:12px}
.stat{border:2px solid var(--ink);background:var(--card);border-radius:12px;padding:10px 12px}.stat b{display:block;font-size:clamp(22px,5vw,42px);line-height:1.05;color:var(--acc)}.stat span{font-size:clamp(11px,2.2vw,15px);font-weight:600;opacity:.85}
.tl{display:flex;flex-direction:column;gap:4px}.tl div{display:grid;grid-template-columns:minmax(64px,24%) 1fr;gap:12px;border-bottom:1.5px solid var(--ink);padding:7px 0;align-items:baseline;font-size:clamp(12px,2.4vw,17px)}.tl b{font-size:clamp(14px,3vw,22px);color:var(--acc);line-height:1.1}
.bars{display:flex;flex-direction:column;gap:9px}.bar{display:grid;grid-template-columns:28% 1fr auto;gap:10px;align-items:center;font-size:clamp(12px,2.3vw,16px)}.bar span:first-child{font-weight:700;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.bar i{display:block;height:20px;background:var(--acc);border:2px solid var(--ink);border-radius:5px}.bar span:last-child{font-weight:800;min-width:44px;text-align:right}
table{border-collapse:collapse;width:100%;font-size:clamp(11px,2.2vw,16px)}th{text-align:left;border-bottom:3px solid var(--ink);padding:6px 8px 6px 0;text-transform:uppercase;font-size:.8em;letter-spacing:.08em;color:var(--acc)}td{border-bottom:1.5px solid var(--ink);padding:6px 8px 6px 0;vertical-align:top}
blockquote{margin:0;padding:12px 16px;border-left:5px solid var(--acc);background:var(--card);border-radius:0 12px 12px 0;font-size:clamp(16px,3.8vw,28px);line-height:1.32;font-weight:700}
.two{display:grid;grid-template-columns:1fr 1fr;gap:14px}.two > div{background:var(--card);border:2px solid var(--ink);border-radius:12px;padding:10px 12px}.two h2{font-size:clamp(15px,3.2vw,22px);margin-bottom:6px;color:var(--acc)}@media (max-width:520px){.two{grid-template-columns:1fr}}
.l-title,.l-closing{justify-content:center}
#nav{position:fixed;left:16px;right:16px;bottom:10px;display:flex;justify-content:space-between;align-items:center;pointer-events:none;z-index:20}
.nav-pill{pointer-events:auto;display:inline-flex;align-items:center;gap:8px;background:var(--ink);color:var(--bg);padding:5px 11px;border-radius:999px;font-size:11.5px;font-weight:800;box-shadow:0 2px 8px rgba(0,0,0,.22)}
.nav-btn{pointer-events:auto;background:var(--card);color:var(--ink);border:2px solid var(--ink);border-radius:999px;width:30px;height:30px;font-size:16px;font-weight:900;display:inline-flex;align-items:center;justify-content:center;cursor:pointer;user-select:none}
.nav-btn:active{background:var(--acc);color:#fff}
</style></head><body><div id="deck">
$sb</div>
<div id="nav">
  <div class="nav-pill"><span id="cnt">1 / $total</span></div>
  <div style="display:flex;gap:6px;pointer-events:auto">
    <button class="nav-btn" id="prevBtn" aria-label="Previous slide">&#8249;</button>
    <button class="nav-btn" id="nextBtn" aria-label="Next slide">&#8250;</button>
  </div>
</div>
<script>
(function(){
  var d=document.getElementById('deck'),c=document.getElementById('cnt'),n=d.children.length;
  function w(){return Math.max(d.clientWidth||window.innerWidth||320,1)}
  function cur(){return Math.min(Math.max(Math.round(d.scrollLeft/w()),0),Math.max(n-1,0))}
  function u(){c.textContent=(cur()+1)+' / '+n}
  function go(delta){var target=Math.min(Math.max(cur()+delta,0),Math.max(n-1,0));d.scrollTo({left:target*w(),behavior:'smooth'});setTimeout(u,180)}
  d.addEventListener('scroll',u,{passive:true});
  window.addEventListener('resize',u);
  document.getElementById('prevBtn').addEventListener('click',function(e){e.stopPropagation();go(-1)});
  document.getElementById('nextBtn').addEventListener('click',function(e){e.stopPropagation();go(1)});
  document.addEventListener('keydown',function(e){if(e.key==='ArrowRight')go(1);if(e.key==='ArrowLeft')go(-1)});
  u();
})();
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
        if (a.length() == 0) return ""
        val sb = StringBuilder("<ul>")
        for (i in 0 until a.length()) {
            val v = a.opt(i)
            when (v) {
                is JSONObject -> sb.append("<li><b>").append(rich(v.s("head").ifBlank { v.s("label").ifBlank { v.s("title") } })).append("</b> ").append(rich(v.s("text").ifBlank { v.s("detail") })).append("</li>")
                else -> {
                    val str = v?.toString().orEmpty().trim()
                    if (str.isNotBlank()) sb.append("<li>").append(rich(str)).append("</li>")
                }
            }
        }
        return sb.append("</ul>").toString()
    }

    private fun body(o: JSONObject): String {
        val t = o.s("text").ifBlank { o.s("content").ifBlank { o.s("description") } }
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
        // Keep image_full inside a clean editorial split/framed layout so images never stretch full-screen or cover text
        if (layout == "image_full") layout = "image_right"

        val title = o.s("title").ifBlank { o.s("heading") }
        val sectionLabel = o.s("section").ifBlank { "SLIDE" }
        val numStr = String.format("%02d / %02d", idx + 1, total)
        val tag = "<div class=\"tag\"><span>${esc(sectionLabel)}</span><span>$numStr</span></div>"
        val h2 = if (title.isNotBlank()) "<h2>${rich(title)}</h2>" else ""
        val inner: String = when (layout) {
            "title", "cover" -> {
                val sub = o.s("subtitle")
                val statsHtml = if (o.has("stats")) renderStats(o.optJSONArray("stats")) else ""
                val txt = "<div class=\"txt\"><h1>${rich(title.ifBlank { "Presentation" })}</h1>" +
                    (if (sub.isNotBlank()) "<p class=\"sub\">${rich(sub)}</p>" else "") + body(o) + statsHtml + "</div>"
                if (hasImg) "<div class=\"cols\">$txt${img(o)}</div>" else txt
            }
            "closing" -> "<h1>${rich(title.ifBlank { "Key Takeaways" })}</h1>" +
                (if (o.s("subtitle").isNotBlank()) "<p class=\"sub\">${rich(o.s("subtitle"))}</p>" else "") + body(o)
            "image_left" ->
                if (hasImg) "$h2<div class=\"cols\">${img(o)}<div class=\"txt\">${body(o)}</div></div>" else "$h2${body(o)}"
            "image_right" ->
                if (hasImg) "$h2<div class=\"cols\"><div class=\"txt\">${body(o)}</div>${img(o)}</div>" else "$h2${body(o)}"
            "stats" -> {
                val statsBlock = renderStats(o.optJSONArray("stats"))
                "$h2${if (o.s("text").isNotBlank()) "<p>${rich(o.s("text"))}</p>" else ""}$statsBlock${bullets(o)}"
            }
            "timeline" -> {
                val a = o.optJSONArray("items") ?: o.optJSONArray("rows") ?: JSONArray()
                val sb = StringBuilder("$h2<div class=\"tl\">")
                for (i in 0 until a.length()) {
                    val s = a.optJSONObject(i) ?: continue
                    val lbl = s.s("label").ifBlank { s.s("year").ifBlank { s.s("big").ifBlank { "${i + 1}" } } }
                    val txt = s.s("text").ifBlank { listOf(s.s("title"), s.s("detail")).filter { it.isNotBlank() }.joinToString(" — ") }
                    sb.append("<div><b>${esc(lbl)}</b><span>${rich(txt)}</span></div>")
                }
                sb.append("</div>").toString()
            }
            "bars" -> {
                val a = o.optJSONArray("bars") ?: o.optJSONArray("chartItems") ?: o.optJSONArray("items") ?: JSONArray()
                var max = 0.0
                for (i in 0 until a.length()) max = maxOf(max, a.optJSONObject(i)?.optDouble("value", 0.0) ?: 0.0)
                if (max <= 0.0) max = 1.0
                val unit = esc(o.s("unit"))
                val unitSpace = if (unit.length > 2) " $unit" else unit
                val sb = StringBuilder("$h2<div class=\"bars\">")
                for (i in 0 until a.length()) {
                    val s = a.optJSONObject(i) ?: continue
                    val v = s.optDouble("value", 0.0).coerceAtLeast(0.0)
                    val pct = (v / max * 100).coerceIn(4.0, 100.0).toInt()
                    val vs = if (v % 1.0 == 0.0) v.toLong().toString() else String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0")
                    sb.append("<div class=\"bar\"><span>${esc(s.s("label"))}</span><i style=\"width:$pct%\"></i><span>$vs$unitSpace</span></div>")
                }
                sb.append("</div>").toString()
            }
            "table" -> {
                val h = o.optJSONArray("header") ?: JSONArray()
                val r = o.optJSONArray("rows") ?: JSONArray()
                val sb = StringBuilder("$h2<div style=\"overflow-x:auto\"><table>")
                if (h.length() > 0) {
                    sb.append("<tr>")
                    for (i in 0 until h.length()) sb.append("<th>${esc(h.optString(i))}</th>")
                    sb.append("</tr>")
                }
                for (i in 0 until r.length()) {
                    val rowAny = r.opt(i)
                    if (rowAny is JSONArray) {
                        sb.append("<tr>")
                        for (j in 0 until rowAny.length()) sb.append("<td>${rich(rowAny.optString(j))}</td>")
                        sb.append("</tr>")
                    } else if (rowAny is JSONObject) {
                        sb.append("<tr><td><b>${esc(rowAny.s("year").ifBlank { rowAny.s("label") })}</b></td><td>${rich(rowAny.s("title"))}</td><td>${rich(rowAny.s("detail").ifBlank { rowAny.s("text") })}</td></tr>")
                    }
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

    private fun renderStats(a: JSONArray?): String {
        if (a == null || a.length() == 0) return ""
        val sb = StringBuilder("<div class=\"stats\">")
        for (i in 0 until a.length()) {
            val s = a.optJSONObject(i) ?: continue
            sb.append("<div class=\"stat\"><b>${esc(s.s("value"))}</b><span>${rich(s.s("label"))}</span></div>")
        }
        return sb.append("</div>").toString()
    }
}
