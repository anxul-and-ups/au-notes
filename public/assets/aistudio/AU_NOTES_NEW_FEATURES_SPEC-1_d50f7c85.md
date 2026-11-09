# AU Notes — New Features & Bug-Fix Specification

> **Agent ke liye instructions (pehle padho):**
> 1. Neeche ke har item ko ek-ek karke implement karo. **Koi bhi point skip mat karo.**
> 2. Har item ke end me **Acceptance Criteria** diye hain — jab tak wo pass na ho, item "done" mat bolo.
> 3. Kaam ke end me ek **checklist table** do: har ID ke samne `Done / Partial / Not done` aur kyun.
> 4. Build verify na ho sake to honestly bolo. Jhooth mat bolo ki "build pass".
> 5. Sirf **changed files** ka zip do.
> 6. Language: UI Hinglish/English mix jaisa abhi hai, wahi rakho.

---

## Index

| ID | Module | Title | Type |
|----|--------|-------|------|
| A | AU Agent | Chat + Voice ek unified session | Feature |
| B | AU Agent | Voice screen cleanup | UI |
| C | AU Agent | Chat uploads, document analysis, image-to-note | Feature |
| D | AU Agent | Prompt-writing (short/long adaptive) | Behaviour |
| E | AU Agent | Long-input → organised file (zero-skip) | Behaviour |
| F | AU Agent | Slides maker (Gemini-style) + export | Feature |
| G | AU Agent | Image generation, graphs, shapes/diagrams | Feature |
| H | Navigation | Back-press folder bug | Bug |
| I | Notes | Stylish-names duplicate / title numbering | Bug |
| J | Editor | PDF mode options + button position | UI |
| K | My Documents | Pinch-zoom viewer | Feature |
| L | Editor | Image pinch-zoom / drag bug | Bug |

---

## A. AU Agent — Chat aur Voice ek hi Session

### Required behaviour
- Chat mode aur Voice mode **ek hi conversation/session** share karein.
- Agar chat khuli hai aur user **voice mode open** karta hai → voice agent ko **usi chat ka poora context** pata ho (purane messages, uploaded files, topic).
- Voice me jo bola gaya aur agent ne jo jawab diya, wo **usi chat thread me transcript ki tarah save** ho.
- Voice se wapas chat par aane par sab continuous dikhe.

### Implementation hint
Ek single `ConversationRepository` / shared `ChatViewModel` rakho; voice aur chat dono usi se read/write karein. Alag-alag history store **nahi** banani.

### Acceptance Criteria
- [ ] Chat me "mera naam X hai" bolo → voice me poochho "mera naam?" → sahi jawab.
- [ ] Voice conversation chat thread me dikhti hai.
- [ ] Koi duplicate/separate history nahi.

---

## B. Voice Mode — UI Cleanup

- Voice screen ke andar jo **keyboard option/button** hai use **remove** karo.
- Chat screen ke **upar wala voice option remove** karo.
- Voice button **chat send button ke bilkul paas** rahe (mic ↔ send ek hi jagah, text likhne par send, khali par mic/voice).

### Acceptance Criteria
- [ ] Voice screen me keyboard button nahi.
- [ ] Chat top bar me voice icon nahi.
- [ ] Voice entry sirf send button ke paas.

---

## C. Chat — Uploads, Document Analysis, Image-to-Note

### C1. Uploads voice agent ko bhi pata hon
Chat me jo bhi file/image upload ho, voice mode me agent **uske baare me jaanta ho** (shared context — see A).

### C2. Auto-analyse documents
- User chat me koi **document (PDF, DOCX, TXT, MD, etc.)** upload kare, aur kuch na likhe → agent **seedha analyse** kare aur bataye: andar kya hai, summary, key points.
- Bina extra prompt ke bhi kaam kare.

### C3. Image → Note
- User image bheje aur kahe "**note me ye bhi laga de**" → image note me **sahi position/size** me insert ho (corrupt/stretched nahi).
- Note ke andar wo image baad me editor me resize/drag ho sake (L dekho).

### Acceptance Criteria
- [ ] PDF upload → bina prompt auto-summary.
- [ ] Upload ke baad voice mode me us file ke baare me puchho → sahi jawab.
- [ ] "Note me laga de" → image note me aa jaye aur dikhe.

---

## D. Prompt-Writing Behaviour (Short vs Long)

Jab user kahe "mere liye prompt likh do":
- Pehle **user ki need samjho** (concept kitna bada/chhota hai).
- **Chhoti need** → **short, crisp prompt**; concept 2–3 lines me clear.
- **Badi/complex need** → **long, structured prompt** (role, context, task, constraints, output format).
- Agent khud decide kare short vs long; user ko lamba explanation na de jab short se kaam chal jaye.

### Acceptance Criteria
- [ ] Simple request → short prompt.
- [ ] Complex request → structured long prompt.

---

## E. Long Input → Organised File (Zero-Skip Rule)

Jab user lamba/chaotic text bheje aur bole "isse organise karke **.md / .txt / etc.** me do":

- Agent **kuch bhi skip na kare**. **Ek-ek point** capture ho.
- Process:
  1. Input ko parse karke **saare points ki internal numbered list** banao.
  2. Unhe sections me organise karo (headings, sub-points).
  3. Output ke end me **coverage check**: "Input me N points the, output me N included."
  4. Koi cheez fit na ho to **"Misc / Others"** section me daalo — drop nahi.
- Requested format (md/txt/…) me file generate ho aur download/save ho sake.
- Meaning change mat karo; sirf structure/spelling clean karo.

### Acceptance Criteria
- [ ] Test: 30 random points do → output me 30 ke 30.
- [ ] Requested file format me file milti hai.

---

## F. Slides Maker (Gemini-style)

### Required behaviour
- User kisi topic par bole "slides bana do" → agent **ready slides** banaye (title slide, content slides, summary).
- **Slide count** user bole to wahi, warna agent decide kare.
- **Background colour/theme AI khud choose kare** — topic ke hisab se **sundar, consistent palette** (gradient/solid, readable contrast). Har slide par same theme.
- Slide me: title, bullets, optional image/graph/diagram (see G).
- Slides **preview + swipe** karke dekhne layak ho, aur edit-able ho (text change).

### Export
- **PDF export** (saari slides ek PDF me).
- **Images export** (PNG/JPG) — **ek click me saari slides gallery me save** (4–5 ho ya zyada).
- Export ke baad confirmation toast: "5 slides gallery me save ho gayi".

### Implementation hint
Slides ko Compose me render karo, `Bitmap` me draw karke `MediaStore` se gallery me save; PDF ke liye `PdfDocument` use karo (page size 16:9).

### Acceptance Criteria
- [ ] "X par 5 slides" → 5 slides, consistent theme.
- [ ] One-tap "Save all to gallery" sab images save karta hai.
- [ ] PDF export me sabhi slides, sahi order me.

---

## G. Image Generation, Graphs & Shapes

### G1. Image generate
- User likhe "imagine / iska image generate kar" → chatbot **image generate** kare aur chat me dikhaye.
- Image ko save/share/note me add karne ka option.
- Image-generation model API key settings se configurable ho; fail hone par saaf error message.

### G2. Graphs / Charts
- User koi PDF/concept samjhaye aur phir kahe "iska graph/diagram bana do" → agent **clean, labelled graphs** bana sake (bar, line, pie, flowchart, etc.).
- Graph readable ho: title, axis labels, legend, clean background.
- Data wahi use ho jo document/concept me tha — **numbers invent nahi karne**.

### G3. Shapes / Elements
- User bole "is element/structure ka shape/diagram banao" → agent **shapes/diagram** (atom, cell, circuit, flowchart, etc.) clean tarah se bana sake (SVG ya image model se).
- Output chat me dikhe aur slides/notes me use ho sake.

### Acceptance Criteria
- [ ] "Imagine a sunset" → image aati hai.
- [ ] Document ke data par graph bante hain, numbers sahi.
- [ ] Shape/diagram request kaam karti hai.

---

## H. Bug — Back Press Folder Redirect

### Problem
Agar user kisi folder (e.g. `Codes`) ke andar ka note padh raha hai aur **back** dabaye, to wo seedha **main folder/home** par chala jata hai.

### Expected
Back dabane par **usi folder me wapas** aaye jahan se note khola tha (folder scroll position bhi preserve ho).

### Fix guide
- Note open karte waqt `folderId` navigation argument me pass karo / back stack me folder screen rakho.
- `popBackStack()` use karo; `navigate(home)` jaisa hard reset **nahi**.
- Search ya recent se khola ho to usi source par wapas.
- Same behaviour: editor, read mode, aur system back + toolbar back dono me.

### Acceptance Criteria
- [ ] Codes folder → note → back → Codes folder.
- [ ] Nested folder me bhi sahi.
- [ ] System back aur UI back dono same.

---

## I. Bug — Stylish Names Duplicate / Title Numbering

### Problem
Stylish names copy karne par title ka format aur duplicates ki problem; lagta hai names **double save** ho rahe hain.

### Required
1. Title numbering format:
   ```
   Stylish names
   Stylish names(1)
   Stylish names(2)
   ```
2. **Duplicate save nahi** hona chahiye: ek tap/copy = ek hi note. Double-click, recomposition, ya listener double-fire se double save na ho (debounce / single-shot guard).
3. Existing duplicate notes ka cause dhoondo (kahin `save()` do jagah se to nahi call ho raha).

### Acceptance Criteria
- [ ] 3 baar copy → `Stylish names`, `Stylish names(1)`, `Stylish names(2)`.
- [ ] Ek copy = exactly ek note (DB me verify).

---

## J. Write Mode — PDF Options & Button Position

### Required
- PDF section me se **saare options hata do**, sirf **2 rakho**:
  1. **Single Page** (preview)
  2. **A4** (preview)
- Ye dono **sirf preview ke liye** hain.
- **Export par behaviour:**
  - User PDF export kare → **preview ke sath open** ho.
  - Agar **Single Page** selected → single long page PDF.
  - Agar **A4** selected → A4 pages me split.
- Options ke **neeche wale 3 buttons** bahut niche chale gaye hain → unhe **upar laao** (visible area me, keyboard/nav bar se door).

### Acceptance Criteria
- [ ] Sirf 2 options dikhte hain.
- [ ] Export selected mode ke hisab se hota hai.
- [ ] 3 buttons screen par aaram se visible/tap-able (chhoti screens par bhi).

---

## K. My Documents — Pinch Zoom

- My Documents me saved files/images ko **gallery jaisa 2-finger pinch zoom** do.
- Features:
  - Pinch zoom in/out (min 1x, max ~5x)
  - Zoom ke baad **pan/drag**
  - **Double-tap** zoom toggle
  - Zoom ke baad bhi smooth, no lag

### Implementation hint
Compose: `detectTransformGestures` / `Modifier.transformable` + `graphicsLayer`; zoomed hone par pan bounds clamp karo, aur HorizontalPager swipe zoom ke time disable karo.

### Acceptance Criteria
- [ ] 2-finger zoom in/out smooth.
- [ ] Zoomed state me drag.
- [ ] Double-tap zoom.

---

## L. Note Editor — Image Zoom / Drag Bug

### Problem
Note editor me inserted image par:
- 2-finger se zoom in/out **sahi nahi** hota.
- Finger se **drag** nahi hota.

### Required
- Image par **pinch-to-resize** (smooth, aspect ratio maintain).
- **Single-finger drag** se image move ho.
- Gestures parent scroll (note ScrollView/LazyColumn) se **conflict na karein**: image touch par parent scroll temporarily disable (`requestDisallowInterceptTouchEvent` ya Compose me pointer-input consume).
- Min/max size limits, aur size/position **save** ho (reopen par same).

### Acceptance Criteria
- [ ] Pinch se resize smooth.
- [ ] Drag se move.
- [ ] Note scroll aur image gestures ek dusre ko block nahi karte.
- [ ] Save → reopen → same size/position.

---

## Final Deliverables Checklist (agent ko ye end me bharna hai)

| ID | Status (Done/Partial/Not done) | Files changed | Notes |
|----|-------------------------------|---------------|-------|
| A | | | |
| B | | | |
| C | | | |
| D | | | |
| E | | | |
| F | | | |
| G | | | |
| H | | | |
| I | | | |
| J | | | |
| K | | | |
| L | | | |

**Rules:**
- Honest build status likho (verified / unverified).
- Sirf changed files zip me do.
- Kisi ID ko "Done" tabhi likho jab uska Acceptance Criteria source-level par check ho chuka ho.
