"use strict";

/*
 * HappyCamper web prototype (demo-2) — guided workflow + roster, coexisting.
 *
 * The page has two modes on a single SPA:
 *   - mode-setup:  the guided workflow (Setup → Import → Run → Warnings → Results) centered on the
 *                  page. This is where you configure features, choose files, run the analysis, and
 *                  review real import warnings + derived result assertions.
 *   - mode-roster: the roster table view. The same workflow collapses into a left slide-in panel
 *                  ("⚙ Imports & checks") so you can tweak inputs and re-run without leaving the data.
 *
 * The server (WebServer.java) only imports rosters and returns JSON; everything else is client-side.
 */

// ----------------------------------------------------------------------------- state
const state = {
  data: null,                 // roster JSON from the server
  files: { camper: null, activity: null },
  demoPreset: "",
  columnVisible: new Set(),
  search: "",
  filters: {},
  sort: { header: null, dir: 1 },
  view: { striping: true, highlightEmpty: false, placeholder: "—" },
};

const FILTER_PRIORITY = ["Cabin", "Program", "Session", "Rounds Assigned", "Swim Color", "SwimColor", "Camp Grade", "Grade", "Gender"];

// ----------------------------------------------------------------------------- helpers
const el = (id) => document.getElementById(id);
const isEmpty = (v) => v == null || String(v).trim() === "";

async function loadJson(url, options) {
  const res = await fetch(url, options);
  const text = await res.text();
  let body;
  try { body = JSON.parse(text); } catch (e) { body = { error: text || ("HTTP " + res.status) }; }
  if (!res.ok || (body && body.error)) {
    throw new Error(body && body.error ? body.error : ("HTTP " + res.status));
  }
  return body;
}

// ----------------------------------------------------------------------------- init
async function init() {
  // Demo presets
  try {
    const presets = await loadJson("/api/demo");
    const select = el("demo-select");
    presets.forEach((name) => {
      const opt = document.createElement("option");
      opt.value = name;
      opt.textContent = "Demo: " + name;
      select.appendChild(opt);
    });
  } catch (e) {
    console.warn("Could not list demo presets:", e.message);
  }
  el("demo-select").addEventListener("change", (e) => {
    state.demoPreset = e.target.value;
    // Choosing a demo clears any uploaded files so the source is unambiguous.
    if (state.demoPreset) clearFiles();
    updateRunButton();
  });

  // Import: drop zone + browse (multi-file, auto-routed by header)
  const dropZone = el("drop-zone");
  el("browse-btn").addEventListener("click", () => el("file-input").click());
  dropZone.addEventListener("click", (e) => {
    if (e.target === dropZone || e.target.tagName === "P" || e.target.tagName === "STRONG") {
      el("file-input").click();
    }
  });
  el("file-input").addEventListener("change", (e) => { handleFiles(e.target.files); e.target.value = ""; });
  ["dragenter", "dragover"].forEach((ev) =>
    dropZone.addEventListener(ev, (e) => { e.preventDefault(); dropZone.classList.add("drag"); }));
  ["dragleave", "drop"].forEach((ev) =>
    dropZone.addEventListener(ev, (e) => { e.preventDefault(); dropZone.classList.remove("drag"); }));
  dropZone.addEventListener("drop", (e) => {
    if (e.dataTransfer && e.dataTransfer.files) handleFiles(e.dataTransfer.files);
  });

  // Per-slot manual override (forces a file into a specific role)
  el("slot-camper").addEventListener("click", () => pickForRole("camper"));
  el("slot-activity").addEventListener("click", () => pickForRole("activity"));
  el("file-input-single").addEventListener("change", (e) => {
    const f = e.target.files[0];
    if (f && pendingRole) {
      state.files[pendingRole] = f;
      state.demoPreset = ""; el("demo-select").value = "";
      updateSlot(pendingRole); updateSlotTools(); updateRunButton();
    }
    pendingRole = null; e.target.value = "";
  });

  el("swap-slots").addEventListener("click", () => {
    const t = state.files.camper; state.files.camper = state.files.activity; state.files.activity = t;
    updateSlot("camper"); updateSlot("activity"); updateRunButton();
  });
  el("clear-files").addEventListener("click", () => { clearFiles(); updateRunButton(); });

  // Run + transition
  el("run-btn").addEventListener("click", runAnalysis);
  el("view-roster-btn").addEventListener("click", () => setMode("roster"));

  // Workflow panel (roster mode)
  el("open-workflow").addEventListener("click", () => toggleWorkflowPanel(true));
  el("close-workflow").addEventListener("click", () => toggleWorkflowPanel(false));
  el("workflow-backdrop").addEventListener("click", () => toggleWorkflowPanel(false));

  // Roster-view controls
  el("search").addEventListener("input", (e) => { state.search = e.target.value; renderTable(); });
  document.querySelectorAll("[data-drawer]").forEach((b) => b.addEventListener("click", () => openDrawer(b.dataset.drawer)));
  el("drawer-close").addEventListener("click", closeDrawer);
  el("opt-striping").addEventListener("change", (e) => { state.view.striping = e.target.checked; renderTable(); });
  el("opt-highlight").addEventListener("change", (e) => { state.view.highlightEmpty = e.target.checked; renderTable(); });
  el("opt-placeholder").addEventListener("input", (e) => { state.view.placeholder = e.target.value; renderTable(); });
  el("cols-all").addEventListener("click", () => setAllColumns(true));
  el("cols-none").addEventListener("click", () => setAllColumns(false));

  updateRunButton();
}

// ----------------------------------------------------------------------------- import sources
let pendingRole = null; // role a per-slot picker is currently choosing a file for

/** Headers that uniquely identify each roster type (lower-cased, exact-match). */
const ACTIVITY_SIGNALS = ["activity", "period", "location", "day", "division", "level"];
const CAMPER_SIGNALS = ["activity preferences", "enrolled sessions/programs", "camp grade", "swimcolor"];

/** Accepts dropped/browsed files, detects each one's role by header, and fills the slots. */
async function handleFiles(fileList) {
  const files = Array.from(fileList).filter((f) => /\.csv$/i.test(f.name));
  if (!files.length) return;
  state.demoPreset = ""; el("demo-select").value = "";

  const detected = [];
  for (const f of files) {
    try { detected.push({ file: f, ...(await detectRole(f)) }); }
    catch (e) { console.warn(e.message); detected.push({ file: f, role: "camper", activityScore: 0, camperScore: 0 }); }
  }

  if (detected.length >= 2) {
    // Strongest activity-signal file → activity slot; next → camper slot.
    const byActivity = detected.slice().sort((a, b) => b.activityScore - a.activityScore);
    state.files.activity = byActivity[0].file;
    state.files.camper = byActivity[1].file;
  } else {
    state.files[detected[0].role] = detected[0].file;
  }
  updateSlot("camper"); updateSlot("activity"); updateSlotTools(); updateRunButton();
}

/** Reads a file's header row and scores it as a camper vs activity roster. */
async function detectRole(file) {
  const cols = parseHeaderCols(await readHeaderLine(file)).map((c) => c.toLowerCase());
  const set = new Set(cols);
  const activityScore = ACTIVITY_SIGNALS.filter((s) => set.has(s)).length;
  const camperScore = CAMPER_SIGNALS.filter((s) => set.has(s)).length;
  return { activityScore, camperScore, role: activityScore > camperScore ? "activity" : "camper" };
}

/** Reads just the first line of a (possibly large) file. */
function readHeaderLine(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const text = String(reader.result);
      const nl = text.search(/\r?\n/);
      resolve(nl >= 0 ? text.slice(0, nl) : text);
    };
    reader.onerror = () => reject(new Error("Could not read " + file.name));
    reader.readAsText(file.slice(0, 8192));
  });
}

/** Splits a CSV header line and strips quotes/whitespace (headers may have stray spaces). */
function parseHeaderCols(line) {
  return line.split(",").map((s) => s.trim().replace(/^"|"$/g, "").trim());
}

function pickForRole(role) {
  pendingRole = role;
  el("file-input-single").click();
}

function clearFiles() {
  state.files = { camper: null, activity: null };
  updateSlot("camper"); updateSlot("activity"); updateSlotTools();
}

function updateSlot(which) {
  const slot = el("slot-" + which);
  const dot = slot.querySelector(".slot-dot");
  const name = slot.querySelector(".slot-name");
  const file = state.files[which];
  const label = which === "camper" ? "Camper roster" : "Activity roster";
  if (file) {
    dot.className = "slot-dot ok";
    name.textContent = label + " — " + file.name;
  } else {
    dot.className = "slot-dot idle";
    name.textContent = label + " — not selected";
  }
}

function updateSlotTools() {
  const any = state.files.camper || state.files.activity;
  el("slot-tools").classList.toggle("hidden", !any);
}

function updateRunButton() {
  const ready = state.demoPreset || (state.files.camper && state.files.activity);
  el("run-btn").disabled = !ready;
}

function selectedFeatures() {
  return Array.from(document.querySelectorAll("#feature-list input:checked")).map((cb) => cb.value);
}

// ----------------------------------------------------------------------------- run
async function runAnalysis() {
  const runBtn = el("run-btn");
  const status = el("run-status");
  runBtn.disabled = true;
  status.classList.remove("hidden");
  status.textContent = "Running analysis…";
  try {
    let data;
    if (state.files.camper && state.files.activity) {
      const [camper, activity] = await Promise.all([
        fileToBase64(state.files.camper),
        fileToBase64(state.files.activity),
      ]);
      data = await loadJson("/api/import", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ camper, activity, features: selectedFeatures() }),
      });
    } else if (state.demoPreset) {
      data = await loadJson("/api/demo?preset=" + encodeURIComponent(state.demoPreset));
    } else {
      throw new Error("Choose a demo or upload both CSV files first.");
    }
    onRosterReady(data);
    status.textContent = "Analysis complete.";
  } catch (err) {
    status.textContent = "✗ " + err.message;
  } finally {
    runBtn.disabled = false;
  }
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const r = reader.result;
      const comma = r.indexOf(",");
      resolve(comma >= 0 ? r.slice(comma + 1) : r);
    };
    reader.onerror = () => reject(new Error("Could not read " + file.name));
    reader.readAsDataURL(file);
  });
}

/** A successful run: render warnings + results, and prepare the roster view behind the scenes. */
function onRosterReady(data) {
  state.data = data;
  state.columnVisible = new Set(data.visible);
  state.filters = {};
  state.search = "";
  state.sort = { header: null, dir: 1 };
  el("search").value = "";

  renderWarnings(data.warnings || []);
  renderResults(data);
  el("card-results").classList.remove("hidden");

  // Build the (still hidden) roster view so "View roster →" is instant.
  buildFilterSidebar();
  buildColumnList();
  renderTable();
  updateStatus();
}

// ----------------------------------------------------------------------------- warnings (real)
function renderWarnings(warnings) {
  el("warnings-head").classList.remove("hidden");
  const list = el("warning-list");
  list.innerHTML = "";
  if (!warnings.length) {
    const ok = document.createElement("div");
    ok.className = "warning-none";
    ok.textContent = "✓ No warnings — every row imported cleanly.";
    list.appendChild(ok);
    return;
  }
  for (const w of warnings) {
    const details = document.createElement("details");
    details.className = "warning-item";

    const summary = document.createElement("summary");
    summary.innerHTML =
      '<span class="w-count">' + w.count + "</span>" +
      '<span class="w-text">' + escapeHtml(w.label) + "</span><span>⌄</span>";
    details.appendChild(summary);

    if (w.detail) {
      const d = document.createElement("p");
      d.className = "warning-detail";
      d.textContent = w.detail;
      details.appendChild(d);
    }
    if (w.samples && w.samples.length) {
      details.appendChild(buildSampleTable(w.headers || [], w.samples, w.count));
    }
    list.appendChild(details);
  }
}

function buildSampleTable(headers, samples, total) {
  const table = document.createElement("table");
  table.className = "warning-samples";
  if (headers.length) {
    const thead = document.createElement("thead");
    const tr = document.createElement("tr");
    headers.forEach((h) => { const th = document.createElement("th"); th.textContent = h; tr.appendChild(th); });
    thead.appendChild(tr);
    table.appendChild(thead);
  }
  const tbody = document.createElement("tbody");
  samples.forEach((row) => {
    const tr = document.createElement("tr");
    row.forEach((cell) => { const td = document.createElement("td"); td.textContent = cell; tr.appendChild(td); });
    tbody.appendChild(tr);
  });
  if (total > samples.length) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.colSpan = Math.max(headers.length, 1);
    td.textContent = "…and " + (total - samples.length) + " more";
    tr.appendChild(td);
    tbody.appendChild(tr);
  }
  table.appendChild(tbody);
  return table;
}

// ----------------------------------------------------------------------------- results (derived assertions)
/**
 * Derives a handful of honest, data-backed checks per enabled feature. These are summaries computed
 * from the imported columns (not a port of the Swing validators) — enough to demonstrate the
 * config→import→assert→view flow. Each check is gated on the columns it needs actually existing.
 */
function buildAssertions(data) {
  const headers = new Set(data.headers);
  const has = (h) => headers.has(h);
  const enabled = new Set((data.features || []).map((f) => f.id));
  const rows = data.rows;
  const count = (pred) => rows.filter(pred).length;
  const blocks = [];

  // Activity assignments
  if (enabled.has("activity")) {
    const checks = [];
    const roundCols = ["Round 1", "Round 2", "Round 3"].filter(has);
    if (roundCols.length) {
      const unassigned = count((r) => !roundCols.some((c) => !isEmpty(r[c])));
      checks.push(unassigned === 0
        ? { pass: true, text: "All campers are assigned to at least one activity." }
        : { pass: false, text: unassigned + " camper(s) are not assigned to any activity." });
      const full = count((r) => roundCols.every((c) => !isEmpty(r[c])));
      checks.push({ pass: full === rows.length,
        text: full + " of " + rows.length + " campers have all " + roundCols.length + " rounds filled." });
    }
    if (checks.length) blocks.push({ name: "Activity assignments", checks });
  }

  // Program inference
  if (enabled.has("program") && has("Program")) {
    const noProgram = count((r) => isEmpty(r["Program"]));
    blocks.push({ name: "Program inference", checks: [
      noProgram === 0
        ? { pass: true, text: "Every camper resolved to a program." }
        : { pass: false, text: noProgram + " camper(s) have no resolved program." },
    ]});
  }

  // Preference analysis
  if (enabled.has("preference")) {
    const checks = [];
    if (has("Preference Score")) {
      const nums = rows.map((r) => parseFloat(String(r["Preference Score"]).replace("%", "")))
        .filter((n) => !isNaN(n));
      if (nums.length) {
        const avg = nums.reduce((a, b) => a + b, 0) / nums.length;
        const pct = avg <= 1 ? Math.round(avg * 100) : Math.round(avg);
        checks.push({ pass: pct >= 70, text: "Average preference score: " + pct + "%." });
      }
    }
    if (has("Unrequested Activities")) {
      const unreq = count((r) => !isEmpty(r["Unrequested Activities"]));
      checks.push(unreq === 0
        ? { pass: true, text: "No campers have unrequested activities." }
        : { pass: false, text: unreq + " camper(s) have unrequested activities." });
    }
    if (checks.length) blocks.push({ name: "Preference analysis", checks });
  }

  // Swim validation — driven by the real swim-related import warnings
  if (enabled.has("swimlevel")) {
    const swimWarnings = (data.warnings || []).filter((w) => /swim/i.test(w.label));
    const total = swimWarnings.reduce((a, w) => a + w.count, 0);
    blocks.push({ name: "Swim validation", checks: [
      total === 0
        ? { pass: true, text: "Swim check: no conflicts flagged." }
        : { pass: false, text: total + " swim issue(s) flagged — see warnings above." },
    ]});
  }

  return blocks;
}

function renderResults(data) {
  const container = el("results-blocks");
  container.innerHTML = "";
  const blocks = buildAssertions(data);
  if (!blocks.length) {
    container.innerHTML = '<p class="muted">No result checks for the selected features.</p>';
    return;
  }
  for (const block of blocks) {
    const div = document.createElement("div");
    div.className = "result-block";
    const name = document.createElement("p");
    name.className = "result-name";
    name.textContent = block.name;
    div.appendChild(name);
    for (const c of block.checks) {
      const a = document.createElement("div");
      a.className = "assertion";
      const dot = document.createElement("span");
      dot.className = "dot " + (c.pass ? "pass" : "fail");
      dot.textContent = c.pass ? "✓" : "✗";
      a.appendChild(dot);
      a.appendChild(document.createTextNode(c.text));
      div.appendChild(a);
    }
    container.appendChild(div);
  }
}

// ----------------------------------------------------------------------------- mode / panel
function setMode(mode) {
  document.body.classList.toggle("mode-setup", mode === "setup");
  document.body.classList.toggle("mode-roster", mode === "roster");
  if (mode !== "roster") document.body.classList.remove("workflow-open");
  if (mode === "roster") renderTable(); // ensure sticky sizing recalcs when shown
}

function toggleWorkflowPanel(open) {
  document.body.classList.toggle("workflow-open", open);
}

// ----------------------------------------------------------------------------- sidebar filters
function deriveFilterColumns(data) {
  const candidates = [];
  for (const h of data.headers) {
    const values = new Set();
    for (const row of data.rows) {
      const v = (row[h] || "").trim();
      if (v) values.add(v);
    }
    const distinct = values.size;
    if (distinct >= 2 && distinct <= 20 && distinct < data.rows.length) {
      candidates.push({ header: h, values: Array.from(values).sort(naturalCompare) });
    }
  }
  candidates.sort((a, b) => {
    const pa = FILTER_PRIORITY.indexOf(a.header);
    const pb = FILTER_PRIORITY.indexOf(b.header);
    const ra = pa === -1 ? 99 : pa;
    const rb = pb === -1 ? 99 : pb;
    if (ra !== rb) return ra - rb;
    return a.values.length - b.values.length;
  });
  return candidates.slice(0, 8);
}

function buildFilterSidebar() {
  const container = el("filter-groups");
  container.innerHTML = "";
  const groups = deriveFilterColumns(state.data);
  if (!groups.length) {
    container.innerHTML = '<p class="muted">No obvious filter columns in this roster.</p>';
    return;
  }
  for (const group of groups) {
    const details = document.createElement("details");
    details.className = "filter-group";
    const summary = document.createElement("summary");
    summary.innerHTML = escapeHtml(group.header) + ' <span class="count">' + group.values.length + "</span>";
    details.appendChild(summary);
    const opts = document.createElement("div");
    opts.className = "filter-options";
    for (const value of group.values) {
      const label = document.createElement("label");
      const cb = document.createElement("input");
      cb.type = "checkbox";
      cb.addEventListener("change", () => onFilterToggle(group.header, value, cb.checked));
      label.appendChild(cb);
      label.appendChild(document.createTextNode(" " + value));
      opts.appendChild(label);
    }
    details.appendChild(opts);
    container.appendChild(details);
  }
}

function onFilterToggle(header, value, checked) {
  if (!state.filters[header]) state.filters[header] = new Set();
  if (checked) state.filters[header].add(value);
  else state.filters[header].delete(value);
  if (state.filters[header].size === 0) delete state.filters[header];
  renderTable();
}

// ----------------------------------------------------------------------------- columns
function buildColumnList() {
  const list = el("column-list");
  list.innerHTML = "";
  for (const h of state.data.headers) {
    const label = document.createElement("label");
    const cb = document.createElement("input");
    cb.type = "checkbox";
    cb.checked = state.columnVisible.has(h);
    cb.addEventListener("change", () => {
      if (cb.checked) state.columnVisible.add(h);
      else state.columnVisible.delete(h);
      renderTable();
    });
    label.appendChild(cb);
    label.appendChild(document.createTextNode(" " + h));
    list.appendChild(label);
  }
}

function setAllColumns(visible) {
  if (visible) state.data.headers.forEach((h) => state.columnVisible.add(h));
  else state.columnVisible.clear();
  buildColumnList();
  renderTable();
}

// ----------------------------------------------------------------------------- table
function orderedVisibleHeaders() {
  return state.data ? state.data.headers.filter((h) => state.columnVisible.has(h)) : [];
}

function matchesSearch(row, headers) {
  if (!state.search.trim()) return true;
  const q = state.search.toLowerCase();
  return headers.some((h) => (row[h] || "").toLowerCase().includes(q));
}

function matchesFilters(row) {
  for (const header in state.filters) {
    const allowed = state.filters[header];
    if (allowed.size > 0 && !allowed.has((row[header] || "").trim())) return false;
  }
  return true;
}

function computeRows(headers) {
  let rows = state.data.rows.filter((r) => matchesSearch(r, headers) && matchesFilters(r));
  if (state.sort.header) {
    const h = state.sort.header, dir = state.sort.dir;
    rows = rows.slice().sort((a, b) => dir * naturalCompare(a[h] || "", b[h] || ""));
  }
  return rows;
}

function renderTable() {
  if (!state.data) return;
  const headers = orderedVisibleHeaders();
  const table = el("roster-table");
  table.classList.toggle("striping", state.view.striping);
  table.classList.toggle("highlight-empty", state.view.highlightEmpty);

  const head = el("table-head");
  head.innerHTML = "";
  for (const h of headers) {
    const th = document.createElement("th");
    th.textContent = h;
    if (state.sort.header === h) {
      const arrow = document.createElement("span");
      arrow.className = "arrow";
      arrow.textContent = state.sort.dir === 1 ? "▲" : "▼";
      th.appendChild(arrow);
    }
    th.addEventListener("click", () => onSort(h));
    head.appendChild(th);
  }

  const body = el("table-body");
  const rows = computeRows(headers);
  const frag = document.createDocumentFragment();
  for (const row of rows) {
    const tr = document.createElement("tr");
    for (const h of headers) {
      const td = document.createElement("td");
      const value = row[h];
      if (isEmpty(value)) { td.classList.add("empty"); td.textContent = state.view.placeholder; }
      else { td.textContent = value; }
      tr.appendChild(td);
    }
    frag.appendChild(tr);
  }
  body.innerHTML = "";
  body.appendChild(frag);
  updateStatus(rows.length);
}

function onSort(header) {
  if (state.sort.header === header) state.sort.dir *= -1;
  else state.sort = { header, dir: 1 };
  renderTable();
}

function updateStatus(shownCount) {
  const countEl = el("status-count");
  if (!state.data) { countEl.textContent = "No roster loaded"; return; }
  const total = state.data.rows.length;
  const shown = shownCount != null ? shownCount : computeRows(orderedVisibleHeaders()).length;
  countEl.textContent = "Showing " + shown + " of " + total + " campers";
  const parts = [];
  if (state.data.warningCount) parts.push(state.data.warningCount + " warning(s)");
  if (state.data.errorCount) parts.push(state.data.errorCount + " error(s)");
  el("status-warnings").textContent = parts.join(" · ");
}

// ----------------------------------------------------------------------------- drawers
function openDrawer(name) {
  el("drawer").classList.remove("hidden");
  const titles = { columns: "Columns", view: "View settings" };
  el("drawer-title").textContent = titles[name] || "";
  ["columns", "view"].forEach((p) => el("panel-" + p).classList.toggle("hidden", p !== name));
  document.querySelectorAll("[data-drawer]").forEach((b) => b.classList.toggle("active", b.dataset.drawer === name));
}
function closeDrawer() {
  el("drawer").classList.add("hidden");
  document.querySelectorAll("[data-drawer]").forEach((b) => b.classList.remove("active"));
}

// ----------------------------------------------------------------------------- misc
function naturalCompare(a, b) {
  const na = parseFloat(a), nb = parseFloat(b);
  const aNum = !isNaN(na) && String(na) === String(a).trim();
  const bNum = !isNaN(nb) && String(nb) === String(b).trim();
  if (aNum && bNum) return na - nb;
  return String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: "base" });
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

init();
