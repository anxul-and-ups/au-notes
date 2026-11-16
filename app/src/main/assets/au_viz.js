/*!
 * AU Viz — glue between AI output and the offline engines (ApexCharts + Mermaid), used by chat cards,
 * HTML artifacts and exported .html files. Never throws to the page: any failure shows a placeholder.
 */
(function (g) {
  "use strict";
  var A = {};
  var ERR = "Chart could not be generated";

  A.decodeB64 = function (b64) {
    try {
      return decodeURIComponent(Array.prototype.map.call(atob(b64), function (c) { return "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2); }).join(""));
    } catch (e) { try { return atob(b64); } catch (e2) { return ""; } }
  };
  g.__auDecodeB64 = A.decodeB64;

  A.error = function (mount, msg) {
    if (!mount) return;
    mount.innerHTML = '<div class="au-viz-error" style="padding:22px 16px;border-radius:12px;border:1px dashed rgba(239,68,68,.45);background:rgba(239,68,68,.08);color:#EF4444;text-align:center;font:700 13px system-ui,sans-serif">' + (msg || ERR) + "</div>";
  };

  // ---- tolerant JSON (code fences, comments, trailing commas, smart quotes) ----
  A.parseJson = function (raw) {
    var t = String(raw || "").trim().replace(/^```[a-z:]*\s*/i, "").replace(/```\s*$/, "").trim();
    try { return JSON.parse(t); } catch (e) { }
    t = t.replace(/\/\/[^\n]*/g, "").replace(/\/\*[\s\S]*?\*\//g, "").replace(/[\u201C\u201D]/g, '"').replace(/,\s*([}\]])/g, "$1");
    return JSON.parse(t);
  };

  // ---- simple AU spec  ->  ApexCharts options (standard Apex options are passed through) ----
  var TYPE = { bar: "bar", column: "bar", columns: "bar", barh: "bar", horizontalbar: "bar", "horizontal bar": "bar", line: "line", area: "area", pie: "pie", donut: "donut", doughnut: "donut", radar: "radar", radialbar: "radialBar", "radial bar": "radialBar", scatter: "scatter" };
  A.toApex = function (spec, o) {
    o = o || {};
    var mode = o.dark ? "dark" : "light";
    if (spec && spec.chart && spec.chart.type) {
      var std = JSON.parse(JSON.stringify(spec));
      std.theme = std.theme || {}; if (!std.theme.mode) std.theme.mode = mode;
      std.chart.toolbar = { show: false };
      if (!std.chart.height || std.chart.height === "auto") std.chart.height = o.height || 320;
      std.chart.type = TYPE[String(std.chart.type).toLowerCase()] || std.chart.type;
      return std;
    }
    var rawType = String(spec.chart_type || spec.type || "bar").toLowerCase();
    var type = TYPE[rawType] || "bar";
    var horizontal = spec.horizontal === true || /barh|horizontal/.test(rawType);
    var series = spec.series;
    if ((!series || !series.length) && Array.isArray(spec.items || spec.data)) {
      var items = spec.items || spec.data;
      series = (type === "pie" || type === "donut" || type === "radialBar") ? items.map(function (i) { return Number(i.value); }) : [{ name: spec.title || "Series 1", data: items.map(function (i) { return Number(i.value); }) }];
      spec.categories = spec.categories || items.map(function (i) { return String(i.label || i.name || ""); });
      spec.labels = spec.labels || spec.categories;
      if (!spec.colors) spec.colors = items.map(function (i) { return i.color; }).filter(Boolean);
    }
    if (!series || !series.length) throw new Error("no series");
    var cats = spec.categories || (spec.xaxis && spec.xaxis.categories) || spec.labels || [];
    var opts = {
      chart: { type: type, height: Number(spec.height) || o.height || 320, toolbar: { show: false } },
      series: series,
      labels: spec.labels || cats,
      xaxis: { categories: cats, title: { text: spec.xAxisLabel || spec.xAxis || "" } },
      yaxis: { title: { text: spec.yAxisLabel || spec.yAxis || "" } },
      theme: { mode: mode },
      unit: spec.unit || "",
      plotOptions: { bar: { horizontal: horizontal } }
    };
    if (spec.colors && spec.colors.length) opts.colors = spec.colors;
    if (type === "bar" && series.length === 1 && opts.colors && opts.colors.length >= cats.length && cats.length > 1) opts.plotOptions.bar.distributed = true;
    if (spec.unit === "%") {
      var vals = []; series.forEach(function (s) { (s && s.data ? s.data : [s]).forEach(function (v) { v = Number(isFinite(+v) ? v : (v && v.y)); if (isFinite(v)) vals.push(v); }); });
      if (vals.length && Math.max.apply(null, vals) <= 100 && type !== "pie" && type !== "donut") opts.yaxis.max = 100;
    }
    if (spec.stroke) opts.stroke = spec.stroke;
    if (spec.dataLabels) opts.dataLabels = spec.dataLabels;
    if (spec.legend) opts.legend = spec.legend;
    if (o.title && spec.title) opts.title = { text: String(spec.title) };
    return opts;
  };

  A.renderChart = function (mount, raw, o) {
    try {
      if (!g.ApexCharts) throw new Error("ApexCharts missing");
      var spec = typeof raw === "string" ? A.parseJson(raw) : raw;
      if (!spec || typeof spec !== "object") throw new Error("bad json");
      var opts = A.toApex(spec, o);
      mount.innerHTML = "";
      var chart = new g.ApexCharts(mount, opts);
      var p = chart.render();
      if (p && p.catch) p.catch(function () { A.error(mount); });
      mount.__auChart = chart;
      return chart;
    } catch (e) { A.error(mount); return null; }
  };

  // ---- Mermaid (real library, async API) ----
  var mseq = 0, mInit = null;
  function mermaidCfg(dark) {
    var v = dark
      ? { background: "#17171B", primaryColor: "#2A2118", primaryTextColor: "#F4F1EA", primaryBorderColor: "#FB923C", secondaryColor: "#1E2A3A", tertiaryColor: "#1F2A22", lineColor: "#9CA3AF", textColor: "#E5E7EB", mainBkg: "#2A2118", nodeBorder: "#FB923C", clusterBkg: "#202026", clusterBorder: "#3B3B46", edgeLabelBackground: "#17171B", noteBkgColor: "#2A2A33", noteTextColor: "#F4F1EA", actorBkg: "#2A2118", actorBorder: "#FB923C", actorTextColor: "#F4F1EA", signalColor: "#E5E7EB", signalTextColor: "#E5E7EB", labelBoxBkgColor: "#2A2118", labelTextColor: "#F4F1EA", cScale0: "#9A4A12", cScale1: "#1E4FA8", cScale2: "#15803D", cScale3: "#7E22CE", cScale4: "#B91C1C", cScale5: "#A16207", cScaleLabel0: "#FFF", cScaleLabel1: "#FFF", cScaleLabel2: "#FFF", cScaleLabel3: "#FFF", cScaleLabel4: "#FFF", cScaleLabel5: "#FFF" }
      : { background: "#FFFFFF", primaryColor: "#FFF1E6", primaryTextColor: "#1F2937", primaryBorderColor: "#EA580C", secondaryColor: "#E8F0FF", tertiaryColor: "#E9F7EE", lineColor: "#64748B", textColor: "#1F2937", mainBkg: "#FFF1E6", nodeBorder: "#EA580C", clusterBkg: "#F8FAFC", clusterBorder: "#CBD5E1", edgeLabelBackground: "#FFFFFF", noteBkgColor: "#FEF3C7", noteTextColor: "#1F2937", actorBkg: "#FFF1E6", actorBorder: "#EA580C", actorTextColor: "#1F2937", signalColor: "#334155", signalTextColor: "#334155", labelBoxBkgColor: "#FFF1E6", labelTextColor: "#1F2937", cScale0: "#FDBA74", cScale1: "#93C5FD", cScale2: "#86EFAC", cScale3: "#D8B4FE", cScale4: "#FCA5A5", cScale5: "#FDE68A", cScaleLabel0: "#1F2937", cScaleLabel1: "#1F2937", cScaleLabel2: "#1F2937", cScaleLabel3: "#1F2937", cScaleLabel4: "#1F2937", cScaleLabel5: "#1F2937" };
    return { startOnLoad: false, securityLevel: "strict", theme: "base", themeVariables: v, fontFamily: "Inter, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif", htmlLabels: false, flowchart: { htmlLabels: false, curve: "basis", useMaxWidth: true, padding: 14, nodeSpacing: 42, rankSpacing: 48 }, sequence: { useMaxWidth: true, mirrorActors: false, wrap: true }, mindmap: { useMaxWidth: true, padding: 12 }, state: { useMaxWidth: true }, timeline: { useMaxWidth: true }, pie: { useMaxWidth: true }, journey: { useMaxWidth: true }, gantt: { useMaxWidth: true } };
  }
  A.cleanMermaid = function (code) {
    var t = String(code || "").replace(/\r/g, "").trim().replace(/^```(?:mermaid)?\s*/i, "").replace(/```\s*$/, "").replace(/[\u201C\u201D]/g, '"').replace(/[\u2018\u2019]/g, "'");
    t = t.replace(/&quot;/g, '"').replace(/&gt;/g, ">").replace(/&lt;/g, "<").replace(/&amp;/g, "&");
    return t.trim();
  };
  A.renderMermaid = function (mount, code, o) {
    o = o || {};
    return new Promise(function (resolve) {
      var done = function (ok) { resolve(ok); };
      try {
        if (!g.mermaid) throw new Error("Mermaid missing");
        var c = A.cleanMermaid(code);
        if (!c) throw new Error("empty");
        var key = o.dark ? "d" : "l";
        if (mInit !== key) { g.mermaid.initialize(mermaidCfg(!!o.dark)); mInit = key; }
        var id = "au_m_" + (++mseq);
        Promise.resolve(g.mermaid.render(id, c)).then(function (r) {
          mount.innerHTML = r.svg;
          var svg = mount.querySelector("svg");
          if (svg) {
            svg.removeAttribute("height"); svg.style.maxWidth = "100%"; svg.style.height = "auto"; svg.style.display = "block"; svg.style.margin = "0 auto";
            if (!svg.getAttribute("xmlns")) svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
          }
          if (r.bindFunctions) { try { r.bindFunctions(mount); } catch (e) { } }
          done(true);
        }).catch(function () {
          var stray = document.getElementById("d" + id); if (stray) stray.remove();
          var stray2 = document.getElementById(id); if (stray2 && stray2.parentNode && stray2.parentNode !== mount) stray2.remove();
          A.error(mount); done(false);
        });
      } catch (e) { A.error(mount); done(false); }
    });
  };

  // ---- export helpers ----
  function svgText(el) {
    var svg = el && el.querySelector ? el.querySelector("svg") : null;
    if (!svg) return "";
    var s = new XMLSerializer().serializeToString(svg);
    if (!/^<svg[^>]+xmlns=/.test(s)) s = s.replace(/^<svg/, '<svg xmlns="http://www.w3.org/2000/svg"');
    return s;
  }
  function svgSize(svg) {
    var vb = (svg.getAttribute("viewBox") || "").trim().split(/[\s,]+/).map(parseFloat);
    var w = vb.length === 4 && vb[2] > 0 ? vb[2] : (svg.getBoundingClientRect().width || 680);
    var h = vb.length === 4 && vb[3] > 0 ? vb[3] : (svg.getBoundingClientRect().height || 340);
    return { w: w, h: h };
  }
  g.__auExportChartSvg = function (id, name) {
    var el = document.getElementById(id); var s = svgText(el); if (!s) return;
    if (g.AUChartBridge && g.AUChartBridge.saveSvg) g.AUChartBridge.saveSvg(s, name || "chart.svg");
    else { var a = document.createElement("a"); a.href = URL.createObjectURL(new Blob([s], { type: "image/svg+xml" })); a.download = name || "chart.svg"; a.click(); }
  };
  g.__auExportChartPng = function (id, name) {
    var el = document.getElementById(id); var svg = el && el.querySelector("svg"); if (!svg) return;
    var sz = svgSize(svg), scale = 2;
    var s = svgText(el).replace(/<svg([^>]*)>/, function (m, attrs) {
      attrs = attrs.replace(/\swidth="[^"]*"/, "").replace(/\sheight="[^"]*"/, "");
      return '<svg' + attrs + ' width="' + sz.w + '" height="' + sz.h + '">';
    });
    var dark = !!(document.body && /rgb\((1[0-9]|2[0-9]|3[0-9]), /.test(getComputedStyle(document.body).backgroundColor));
    var cv = document.createElement("canvas"); cv.width = sz.w * scale; cv.height = sz.h * scale;
    var ctx = cv.getContext("2d"); ctx.fillStyle = dark ? "#17171B" : "#FFFFFF"; ctx.fillRect(0, 0, cv.width, cv.height); ctx.scale(scale, scale);
    var img = new Image();
    img.onload = function () {
      ctx.drawImage(img, 0, 0, sz.w, sz.h);
      var url = cv.toDataURL("image/png");
      if (g.AUChartBridge && g.AUChartBridge.savePngDataUrl) g.AUChartBridge.savePngDataUrl(url, name || "chart.png");
      else { var a = document.createElement("a"); a.href = url; a.download = name || "chart.png"; a.click(); }
    };
    img.onerror = function () { if (g.AUChartBridge && g.AUChartBridge.toast) g.AUChartBridge.toast("PNG export failed"); };
    img.src = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(s);
  };
  g.__auCopyVizSource = function (b64, btn) {
    var txt = A.decodeB64(b64);
    if (g.AUChartBridge && g.AUChartBridge.copyText) g.AUChartBridge.copyText(txt);
    else if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(txt);
    if (btn) { var o = btn.textContent; btn.textContent = "Copied!"; setTimeout(function () { btn.textContent = o; }, 1400); }
  };

  // ---- boot: render every <div class="au-apex-mount|au-mermaid-mount" data-*-b64> in the page ----
  A.boot = function (o) {
    o = o || {};
    var dark = o.dark;
    if (dark == null) dark = !!(document.body && /rgb\((1[0-9]|2[0-9]|3[0-9]), /.test(getComputedStyle(document.body).backgroundColor));
    var apex = document.querySelectorAll(".au-apex-mount[data-apex-b64]");
    for (var i = 0; i < apex.length; i++) {
      var n = apex[i]; if (n.getAttribute("data-rendered") === "1") continue; n.setAttribute("data-rendered", "1");
      A.renderChart(n, A.decodeB64(n.getAttribute("data-apex-b64") || ""), { dark: dark, title: true });
    }
    var mm = document.querySelectorAll(".au-mermaid-mount[data-mermaid-b64]"), chain = Promise.resolve();
    Array.prototype.forEach.call(mm, function (n) {
      if (n.getAttribute("data-rendered") === "1") return; n.setAttribute("data-rendered", "1");
      chain = chain.then(function () { return A.renderMermaid(n, A.decodeB64(n.getAttribute("data-mermaid-b64") || ""), { dark: dark }); });
    });
  };

  g.AUViz = A;
})(window);
