# AI_Agent_Complete_Requirements_Hinglish

# AI Agent & Notes App — Complete Bug Fixes, Features aur Implementation Requirements

> **AI Coding Agent ke liye important instruction:** Is document ke har point ko implementation scope ka hissa samjho. Koi requirement silently skip, simplify ya ignore mat karna. Pehle existing codebase aur architecture inspect karo, phir root cause fix karo. Existing working features ko break kiye bina changes implement aur test karo.
>
> **Explicit exclusions:** Ghost Writing / Ghost Text / AI Autocomplete aur Flashcards se related poora prompt, feature, UI, logic aur API implementation is specification se hata diya gaya hai. In features ko implement mat karna.

---

## 1. Overall Goal aur Engineering Standards

1. App ke saare listed serious bugs ko properly fix karna hai. Sirf temporary workaround ya superficial UI changes nahi chahiye.
2. Har feature ka implementation stable, accurate aur production-quality hona chahiye.
3. Existing working features ko preserve karna hai. Naye changes ki wajah se purane features break nahi hone chahiye.
4. Coding quality par special dhyaan do. Koi page, note, export, layout, interaction ya generated artifact incomplete, invalid ya broken nahi hona chahiye.
5. Available server/model ki coding capabilities ka proper use karo. Half-coded ya dummy implementation mat dena.
6. Large prompts, long notes, PDFs, images aur bade contexts ko reliably handle karo. Important content silently truncate, lose ya mix nahi hona chahiye.
7. Agent ke response ka unnecessary wait time kam karo, lekin correctness compromise mat karna.
8. UI long-running AI, export ya rendering operation ke dauran responsive rehni chahiye.
9. Kisi task ko complete tabhi show karna jab requested note/artifact actually create ya edit ho chuka ho aur open/preview kiya ja sake.
10. Har implementation ke baad relevant test cases run karo aur regressions check karo.

---

## 2. Agent Chat — Note Attach Karna aur Notes Search Karna (New Feature)

Abhi agent chat ke composer/input box mein jo **Note icon** diya hua hai, woh properly work nahi kar raha. Us icon se notes attach nahi hote. Is feature ko fix karke note search bhi add karna hai.

### 2.1 Chat se note attach karna

- Chat input/composer ke Note icon ko fully functional banao.
- Icon tap karne par ek proper note-selection interface open hona chahiye.
- User apne existing notes ko browse karke kisi note ko current chat mein attach kar sake.
- Attached note(s) clearly chat composer mein visible hone chahiye.
- Message send karne se pehle user attached note ko remove bhi kar sake.
- Sirf attachment chip/UI show mat karna; selected note ka actual content aur relevant assets agent ko available hone chahiye.

### 2.2 Notes ke liye search

- Note-selection screen/interface mein search field add karo.
- Search note ke **title aur content dono** mein kaam karni chahiye.
- Search sirf currently visible note cards tak limited nahi honi chahiye; poori available notes collection mein search ho.
- User type kare to results useful tareeke se update hon.
- Empty query, no-results state aur large notes collection ko properly handle karo.
- Search result se note select karke easily attach kiya ja sake.

### 2.3 Attached note par AI se koi bhi task karwana

Note attach hone ke baad user agent se us note par koi bhi supported task karwa sake, jaise:

- Note ka summary banana.
- Note ka content explain karna.
- Note se HTML page ya HTML artifact banana.
- Note se slides banana.
- Note ko organize, rewrite, transform ya user ke instruction ke according process karna.

**Important:** Agent ko attached note ka actual content read aur use karna hoga. Sirf attachment acknowledge karke imaginary content generate nahi karna. Current request ke liye attached note ko source context ke roop mein use karo. Agar user baad mein koi naya source deta hai, toh us naye task ke context ko correctly handle karo.

---

## 3. PDF Export — Images, Text aur Pagination Fix

PDF export mein serious bugs hain. Kabhi images render nahi hoti, kabhi wrong page par chali jaati hain aur kabhi export blank dikhata hai. Export pipeline ko properly fix karo.

1. Note mein jo images hain, woh exported PDF mein correctly render honi chahiye.
2. Note ke content ka intended order aur placement preserve hona chahiye.
3. Image ko jis page par rakha gaya hai, export mein bhi woh expected page par hi aaye.
4. Agar note mein sirf ek image hai aur note single page ka hai, toh image usi first page par export ho. Empty/untitled first page bana kar image ko second page par mat bhejna.
5. Images wale notes export karne par blank pages ya completely blank PDF nahi aani chahiye.
6. Text, images aur supported note elements sab PDF mein correctly render hone chahiye.
7. Multiple images aur long images ko multi-page PDF mein correctly handle karo.
8. Pagination predictable honi chahiye. Content clip, misplaced, missing ya unexpectedly next page par shift nahi hona chahiye.
9. PDF layout editor ke page layout aur page breaks se maximum possible match karna chahiye.
10. Export ke baad page order aur content placement verify karo.

### PDF export ke mandatory test cases

- Sirf ek image wala note.
- Pehle text, phir image.
- Pehle image, phir text.
- Multiple images wala note.
- Ek bahut long image.
- Multiple pages mein spread hone wala long note.
- Different image sizes aur alignments ke saath mixed text + images.

---

## 4. Note Editor — Complete Editing Experience Upgrade

Current note editor mein kaafi usability aur layout bugs hain. Isko ek modern, stable aur flexible editor ki tarah rebuild/upgrade karo. Image editing/arrangement ka experience advanced editor (jaise Alight Motion mein elements arrange karte hain) jitna intuitive ho, lekin note-writing functionality compromise nahi honi chahiye.

### 4.1 Image add, resize aur position controls

- Image add karne par woh poore note ko cover na kar de.
- Image add karne ke baad bhi note mein likhne ke liye enough space available rahe.
- Image ke liye ye controls hone chahiye:
  - Image ko chhota ya bada karna.
  - Image ko left/right move karna.
  - Image ko left, center ya right align karna.
  - Page/note ke andar image ko required position par place karna.
- Image resizing aur moving intuitive aur precise honi chahiye.
- Text image ke upar ya image ke background mein type nahi hona chahiye.
- Agar image small hai aur left/right/center mein placed hai, toh layout allow kare toh uske sides mein text likhne ki jagah available rahe.
- Image ke upar aur neeche clear writing space honi chahiye.
- Image ke neeche, page layout allow kare toh, kam se kam approximately do lines likhne ki jagah available rahe.
- Image cursor placement, text selection, tapping ya further writing ko block na kare.
- User image ko note ke top, middle ya bottom mein add kare, har jagah image ke pehle, baad aur suitable sides mein likhne ka option available rahe.
- Image aur text elements ek doosre ko unnecessarily overlap ya block na karein.

### 4.2 Page-aware layout aur pagination

- Image add karne par content ko pages mein properly distribute karo.
- Image ki wajah se poora page unusable nahi hona chahiye.
- Editor mein page boundaries clearly samajh aani chahiye.
- User ko pata chalna chahiye ki kaunsi image kis page par jayegi aur text kis page par place hoga.
- PDF export se pehle page-wise layout samajhne ke liye page-aware editing/preview experience do.
- Long images ya image-heavy notes mein user note ke bilkul end tak scroll kar sake.
- Multiple images ya ek long image add karne par scrolling end tak pahunchne se block nahi honi chahiye.
- Scrolling smooth rahe; image element ya nested container ki wajah se scroll trap na ho.

### 4.3 Text blocks / Copy Box ke bugs

Current copy-box/text-block behavior ko fix karo:

- Text blocks typing, editing, selecting aur deleting ke dauran stable aur accurate rahen.
- Text block remove karne par woh unexpectedly disappear na ho aur surrounding content corrupt na ho.
- Block remove karne par commas ya doosre punctuation marks galat tareeke se suddenly appear/expose na hon.
- Type karte waqt starting ke words text box ke top mein ghusne, hide hone ya clip hone nahi chahiye.
- Text block edit/delete karne par text unexpectedly kisi doosre format/block mein convert na ho.
- Ek area ka text delete karne se unrelated text ya doosre elements modify nahi hone chahiye.
- Block remove/edit karne ke baad user uske neeche tap karke writing continue kar sake.
- Cursor deletion/editing ke baad bhi accessible aur correct position par rahe.
- User editor mein aisi situation mein stuck na ho jahan kahin tap karke text insert hi na kar sake.
- Text block remove karne par uske baad ka content sahi position par rahe.

### 4.4 Note title editing

- Note title ko properly select, edit aur save kiya ja sake.
- Title edit karne se note body disturb, jump ya disappear nahi honi chahiye.
- Title editing reliable honi chahiye.

### 4.5 Editor reliability

- Text aur image elements independently select aur edit kiye ja sakein.
- Ek element ko edit/delete karne se doosra element accidentally modify nahi hona chahiye.
- Image move, resize, delete ya rearrange karne par baaki note content preserve rahe.
- Long notes, multiple images aur multi-page content mein editor usable rahe.

---

## 5. AI Agent — Live Progress, Large Context aur Response Speed

### 5.1 Agent ka live progress show karna

Agent jab kaam kar raha ho, user ko live pata chalna chahiye ki woh kya kar raha hai. User ko aisa nahi lagna chahiye ki agent stuck hai ya loop mein fas gaya hai.

- Meaningful task stages live show karo, jaise:
  - Request ko understand karna.
  - Source/attached file read karna.
  - Task plan karna.
  - Code ya artifact create/edit karna.
  - Render karna.
  - Final output verify karna.
- Coding task ke dauran current coding step aur work progress ka useful status show karo.
- Jahan appropriate aur safe ho, current implementation step ya code-writing activity ka readable summary show karo.
- Progress task ke saath update hota rahe.
- Fake progress, repeated messages ya misleading completion claims mat dikhana.
- Clearly show karo ki agent processing mein hai, waiting hai, error face kar raha hai ya complete ho gaya hai.
- Jahan appropriate ho, operation stop/cancel karne ka option do.

### 5.2 Large context handling

- Large user messages, long notes, PDFs, images aur multi-part instructions ko reliably process karo.
- User ke context ka important portion silently drop/truncate nahi hona chahiye.
- Long conversation mein correct source aur task relationship preserve rahe.
- Agar content ko chunks mein process karna pade, toh chunks ka order aur meaning preserve karke final result combine karo.
- Ek previous task ka content kisi naye unrelated task mein mix mat karo.
- Agar source ka content read nahi ho pa raha, toh clearly batao. Apni taraf se imaginary content mat banao.

### 5.3 Agent response speed

- Unnecessary delays reduce karke response time improve karo.
- Agent ke kaam ke dauran app UI responsive rahe.
- Long-running AI/rendering operations main UI ko block na karein.
- Operation fail ho ya bahut der le, toh useful error/retry state show karo.

---

## 6. Agent Context Understanding — Unrelated Tasks Mix Nahi Hone Chahiye

Agent purane request ka context naye unrelated task mein mix kar deta hai. Context tracking aur source selection ko fix karo.

1. Har new request ko current message, selected attachments aur sirf relevant conversation context ke basis par samjho.
2. Previous task ka subject new unrelated task mein carry forward mat karo.
3. Current bug ka example:
   - User ne student ke score ko curved graph mein show karne ko kaha.
   - Agent ne actual curved graph banane ke bajay ek poor slide bana di, jisme graph tha hi nahi; sirf English text mein graph ka naam/description likha tha.
   - Chat mein response bhi aadha hi visible tha.
   - Uske baad user ne Indian Army ke baare mein likhi hui image di aur uska HTML page banane ko kaha.
   - Agent ne Indian Army page mein pehle wale student/score ka context mix kar diya aur supplied image ko bhi inappropriate tareeke se insert kar diya.
4. Required behavior:
   - Student-score graph request par actual curved graph render karo, suitable data, labels aur visual representation ke saath.
   - Sirf “graph” word ya text description likhkar actual graph ka replacement mat do.
   - Complete output chat ya appropriate preview mein visible hona chahiye.
   - Indian Army HTML request par Indian Army source aur current instructions hi use karo. Student-score content include mat karo.
   - User image provide kare toh automatically image ko artifact mein insert mat karo. Samjho ki image ko reference/source ke roop mein use karna hai, page mein embed karna hai, ya dono.
   - Image ko user ke request ke according hi use karo.
5. In contexts ko clearly separate rakho:
   - Current task instructions.
   - Earlier conversation context.
   - Attached notes/files/images.
   - Generated output.
6. Agar source ya attachment ka intended use genuinely unclear ho, toh guess karne ke bajay ek short clarification pucho.

---

## 7. Slides, HTML Pages aur Artifact Generation

### 7.1 Slides ka correct generation aur rendering

- Agent chat screen par slides aur generated artifacts correctly aur completely render hone chahiye.
- Preview aadha, clipped ya unreadable nahi hona chahiye.
- Slide previews readable aur properly navigable hon.
- User graph, chart, diagram ya koi visual element maange toh actual visual create karo.
- Visual ke naam ka text likhkar actual graph/chart/diagram ka replacement mat do.
- User ka content, data, visual structure aur subject preserve karo.
- Completion show karne se pehle generated slides ko render karke verify karo.

### 7.2 User-provided source se HTML page banana

Agar user PDF, image/photo, note ya koi doosra source provide karta hai aur usse slides, HTML artifact ya HTML page banane ko kehta hai:

- Sabse pehle provided source read aur understand karo.
- Source ke basis par beautiful, well-explained HTML website/page banao.
- User ne slides maangi hain toh slides bhi banao.
- App mein proper preview do jahan user HTML page aur slides inspect kar sake.
- Content provided source aur user instructions ke faithful hona chahiye.
- Previous unrelated task ka content add mat karo.
- Source image ko automatically HTML page mein insert mat karo, jab tak user explicitly na kahe ya requested design mein image embed karna clearly required na ho.
- Agar image mein text/information hai, toh relevant information accurately extract karke use karo. Original image ko reference ke roop mein use kiya ja sakta hai bina use page mein embed kiye.
- Final page visually polished, readable, well-structured aur responsive honi chahiye.

### 7.3 Create MD / Create Slide quick actions

App screen par **Create MD** aur **Create Slide** quick actions hain.

- User inmein se kisi par tap kare toh bot English mein reply karke puche ki user kis topic/subject/content par banana chahta hai.
- Prompt clear ho aur user ko topic ya source provide karne ke liye guide kare.
- Tap karte hi unrelated ya empty artifact generate mat karna.

### 7.4 Artifact completion aur preview

- Generated HTML, slides aur doosre supported artifacts ka usable preview available hona chahiye.
- Empty, incomplete, broken ya unrendered artifact ko success ke roop mein show mat karo.
- Completion se pehle output validate karo.

---

## 8. AI ke Through Note Create aur Edit Karna

Intermittently AI model note create ya edit nahi karta. Kabhi empty note create hota hai aur kabhi requested edit apply nahi hota. User ko kaam karwane ke liye model switch karna padta hai. Is issue ko reliably fix karo.

1. User note create karne ko kahe toh requested content ke saath actual note create hona chahiye, empty note nahi.
2. User existing note edit karne ko kahe toh correct note mein requested changes apply hon.
3. Silent failure mat hone do aur edit kiye bina success claim mat karo.
4. Existing note ka content preserve karo, jab tak user explicitly replace/remove karne ko na kahe.
5. Confirm karo ki note actually save ho chuka hai aur reopen karne par saved content available hai.
6. Model/API error ko gracefully handle karo. Model switch karna normal workaround nahi hona chahiye.
7. Creation/editing fail ho toh clear error aur retry option show karo.
8. Supported models ke across note creation aur editing consistent honi chahiye.

---

## 9. Alarm aur Timer System — Advanced UI aur Functionality

Alarm aur timer features ko substantially improve karo.

### 9.1 Alarm controls aur status

- Alarm ke liye advanced aur polished UI banao.
- Sidebar par jo red blinking alarm indicator/light hai, usko Alarm section ke andar se control kiya ja sake.
- User Alarm screen mein jaakar relevant active alarm ya uske indicator ko stop/off kar sake.
- Sidebar aur Alarm screen ke beech alarm status synchronized aur accurate rahe.
- Alarm dismiss ya off karne ke baad uska red blinking indicator active nahi rehna chahiye.

### 9.2 Note-linked alarms

- Jis note par alarm set hai, us note ke card par alarm details show honi chahiye.
- Note card par clearly pata chale ki alarm kis time scheduled hai.
- Note card aur Alarm section ke alarm details synchronized rahen.
- Note se linked alarm ko identify, edit, disable ya dismiss karna easy ho.

### 9.3 Alarm ke liye device files select karna

- Alarm set karte waqt jo device files show/select hoti hain, unka UI advanced aur clear banao.
- File selection interface easy to navigate ho.
- Search option add karo, jisse user required device file quickly find kar sake.
- File names aur selected file ka state clearly visible ho.

### 9.4 Easy timer setup

- Current timer set karne ka process confusing hai; ise redesign karo.
- Timer setup ek bahut simple aur easy-to-understand (“easy peasy”) style mein hona chahiye.
- Duration select karna obvious aur quick ho.
- Selected duration aur timer running state clearly show ho.
- Start, pause, resume, reset aur stop jaise required controls clear hon.
- Timer interface responsive ho aur unnecessary delay na ho.

---

## 10. Voice Input aur Microphone Experience

Chat box ke microphone/voice input ko improve karo.

1. Chat box ka mic tap karne par wahi integrated voice-input experience open ho jo voice agent bhi use karta hai.
2. Mic use karte waqt alag se blue-color wala awkward screen/open state nahi aana chahiye; voice experience integrated aur consistent ho.
3. Voice input tab tak active/listening rahe jab tak user khud stop na kare.
4. User ke manually stop karne par hi listening stop honi chahiye, app ke unexpected behavior ki wajah se nahi.
5. Mic active hone par ek premium-quality animation banao.
6. Animation clean, smooth aur polished ho; excessive neon ya typical “AI neon” type design nahi chahiye.
7. Voice listening state clearly visible honi chahiye, taaki user ko pata rahe mic active hai ya nahi.

---

## 11. Note Read Mode — Advanced Search

Note ke Read Mode ke andar search option add karo.

- Read Mode mein search icon add karo.
- Search icon tap karne par current note ke content ke andar search ho.
- Search experience advanced ho, Chrome browser ke find-in-page search jaisa.
- Search result count show ho, jaise current match / total matches.
- Previous aur next match par jaane ke liye up/down arrows hon.
- Current matched word/phrase note mein highlight ho.
- Search results ke beech navigate karna easy ho.
- Search text clear/close karne ka option bhi ho.
- Long notes mein search accurate aur responsive rahe.

---

## 12. Network Error Handling — Offline State par App Crash Nahi Hona Chahiye

App mein jo features internet/network par depend karte hain, agar user unhe bina data/internet ke open kare toh app crash nahi honi chahiye.

- Network-dependent feature open karne se pehle/operation ke dauran connectivity state handle karo.
- Internet unavailable ho toh clear **Network Error / No Internet Connection** information show karo.
- App crash, freeze ya blank screen nahi honi chahiye.
- User ko retry karne ka option do.
- Network wapas aane par feature normal work kar sake.
- Error state understandable ho; technical stack trace user ko mat dikhao.

---

## 13. Main Screen Sidebar Animation

Main screen ka sidebar close/open karne par abhi poor animation aata hai.

- Existing awkward/janky sidebar animation fix karo.
- Sidebar open/close par smooth, polished, iOS-like side pop-up animation create karo.
- Animation natural aur responsive ho.
- Animation ke dauran content jump, flicker ya overlap nahi hona chahiye.
- Sidebar open/close interaction fast aur consistent rahe.

---

## 14. Landscape Mode Support

- App ke saare major features landscape mode mein bhi properly work karne chahiye.
- Sirf screen rotate hona enough nahi; layouts aur controls landscape ke according adapt hon.
- Notes, editor, chat, alarms, timer, search, split screen, small window, HTML/slides preview aur supported tools landscape mein usable rahen.
- Buttons, text, toolbars ya content clipped/hidden nahi hone chahiye.
- Orientation change par user ka current work, typed text aur editing state lose nahi hona chahiye.

---

## 15. Split Screen — Two Notes Ek Saath Open Karna (New Feature)

App mein split-screen feature add karo, jisse user ek saath do notes open karke kaam kar sake.

1. User do notes ko split-screen layout mein open kar sake.
2. Dono notes ek hi screen par alag sections/panes mein visible hon.
3. Notes ko small window mode mein bhi open kiya ja sake.
4. User ek note read kar raha ho ya write kar raha ho, dono situations mein note ko small window/split-screen mein open karne ka option ho.
5. Dono panes/toolbars independent hon.
6. Upar wale note ka toolbar aur actions upar wale note par hi kaam karein.
7. Neeche wale note ka toolbar aur actions neeche wale note par hi kaam karein.
8. Ek pane mein typing, formatting, scrolling ya editing karne se doosra pane accidentally affect nahi hona chahiye.
9. Dono notes ka content independently save aur update ho.
10. Split-screen layout responsive ho aur available screen size ke according adjust ho.
11. Landscape mode mein bhi split-screen usable hona chahiye.

---

## 16. Small Window / Mini Window — Native Android Multi-Window Implementation

**Task:** Current “Small Window” aur “Mini Overlay” implementation ko Native Android Multi-Window / Freeform Mode par shift/fix karo.

### Current bug

- Current overlay `SYSTEM_ALERT_WINDOW` use karta hai, jisme native OS controls available nahi hain.
- “Small Window” option incorrect Intent flags ki wajah se full app hi open kar deta hai.
- Custom floating overlay native Android window ki tarah behave nahi karta.

### Required implementation

1. `AndroidManifest.xml` mein `MainActivity` ke liye `android:resizeableActivity="true"` set karo.
2. Activity mein `onMultiWindowModeChanged` aur `onConfigurationChanged` override/handle karo, taaki UI resizing gracefully handle ho.
3. Small Window ke liye `SYSTEM_ALERT_WINDOW` use mat karo. OS Window Manager ko Freeform mode mein Activity place karne do.
4. Vivo OriginOS multi-window adaptation guidelines follow karo.
5. `WindowInsets` correctly handle karo.
6. Layout small window sizes ke liye responsive hona chahiye.
7. Custom “mini overlay” implementation jo floating window ko manually mimic karta hai, use remove karo.
8. Close, expand aur resize jaise native window controls Android OS provide kare.
9. Multi-window mein new Activity instance launch karne ke liye zaroorat ke according `FLAG_ACTIVITY_MULTIPLE_TASK` support karo.
10. Small Window open karne par full-screen app launch hone wala bug fix karo.
11. Device/OS par Freeform support available na ho toh gracefully handle karo; crash ya broken state nahi honi chahiye.

**References to verify during implementation:** Android Freeform Window API aur Vivo OriginOS Multi-window Developer Guide. In references ki current official documentation check karke device compatibility verify karo.

---

## 17. Language Matching aur Conversation Language

Agent ko user ki language ke according reply karna chahiye.

1. Agar user conversation English mein start karta hai, agent bhi English mein reply kare.
2. Agar user Hindi mein conversation start karta hai, agent Hindi mein reply kare.
3. Agar user Hinglish mein baat kar raha hai, agent Hinglish mein reply kare.
4. Agar user explicitly kahe ki ab kisi doosri language mein baat karo (jaise Hinglish ya English), toh uske baad requested language use karo.
5. Language change ko conversation mein correctly maintain karo.
6. User ki language preference ke against unnecessary language switching mat karo.

---

## 18. Implementation, Testing aur Final Verification Checklist

Implementation complete karne ke baad har section ko verify karo. Sirf code likhna completion nahi hai.

### Notes aur Editor

- [ ] Chat composer ka Note icon notes attach karta hai.
- [ ] Note picker mein title + content search work karti hai.
- [ ] Attached note ka actual content agent ko milta hai.
- [ ] Attached note se summary, explanation, HTML aur slides jaise tasks work karte hain.
- [ ] Image resize/move/align controls work karte hain.
- [ ] Image ke aas-paas aur before/after text likha ja sakta hai.
- [ ] Page boundaries aur page-wise layout clear hain.
- [ ] Long notes/images ke end tak scroll hota hai.
- [ ] Copy-box/text-block deletion aur cursor bugs fix hain.
- [ ] Note title edit/save hota hai.
- [ ] AI note create aur edit karta hai; empty note ya silent failure nahi hota.

### PDF aur Artifacts

- [ ] PDF mein images render hoti hain.
- [ ] Single-image note first/expected page par export hota hai.
- [ ] PDF blank nahi hoti aur pagination correct hai.
- [ ] Slides complete aur correctly render hoti hain.
- [ ] Requested graph actual visual graph ke roop mein render hota hai.
- [ ] HTML page source ke according banta hai.
- [ ] HTML/slides ka in-app preview usable hai.
- [ ] Unrelated previous context naye artifact mein mix nahi hota.
- [ ] Source image bina permission ke automatically embed nahi hoti.

### Agent aur App UI

- [ ] Agent meaningful live progress show karta hai.
- [ ] Large context reliably handle hota hai.
- [ ] Response time aur UI responsiveness acceptable hain.
- [ ] Create MD / Create Slide tap par bot English mein topic/source puchta hai.
- [ ] Language matching aur explicit language switching work karti hai.
- [ ] Offline network-dependent feature crash nahi karta; clear network error show hota hai.
- [ ] Sidebar animation smooth hai.
- [ ] Landscape mode mein major features work karte hain.

### Alarm, Timer, Voice aur Multi-Window

- [ ] Alarm indicator Alarm section se off/dismiss ho sakta hai.
- [ ] Note cards par linked alarm ka time show hota hai.
- [ ] Alarm device-file picker mein search available hai.
- [ ] Timer setup simple aur clear hai.
- [ ] Chat mic integrated voice experience use karta hai.
- [ ] Voice listening user ke manually stop karne tak active rehti hai.
- [ ] Mic animation premium aur non-neon hai.
- [ ] Read Mode search mein result count, highlights aur up/down navigation hai.
- [ ] Two-note split screen mein dono toolbars independent hain.
- [ ] Small Window native Android multi-window/freeform implementation use karta hai.
- [ ] Custom `SYSTEM_ALERT_WINDOW`-based mini overlay remove kiya gaya hai.

aur jaha jaha files load hoti hain vaha per loading chale kyuki har jagah files crash karba rhi hai app lag karne lagta hai agar html viewer me thoda bada import kar liya to app crash agar my docs me kar liya to crash file Editor me kar liya to crash ye sab bhi fix karo 
---

## 19. Final Instructions to the Coding Agent

- Pehle current project architecture, existing code aur related s
creens inspect karo.
- Har bug ka root cause identify karke fix karo.
- Existing data aur user notes ko safe rakho; migration required ho toh backward compatibility ka dhyaan rakho.
- UI design modern, clean, premium aur consistent ho; unnecessary neon effects use mat karo.
- Har feature ko actual functionality ke saath implement karo. Sirf button/icon add kar dena feature completion nahi hai.
- Error handling, loading states, empty states, accessibility, responsive layout aur state management ka dhyaan rakho.
- Android lifecycle, orientation changes, process recreation aur multi-window behavior ko properly handle karo.
- Tests run karo, relevant devices/screen sizes par verify karo aur regressions fix karo.
- Agar kisi requirement ke liye platform/device limitation ho, toh us limitation ko clearly document karo aur graceful fallback implement karo. Fake support claim mat karo.
- Final report mein clearly batao: kya implement hua, kaunse bugs fix hue, kya test kiya gaya aur koi limitation abhi baaki hai.

**Scope reminder:** Ghost Writing / Ghost Text / AI Autocomplete aur Flashcards feature is project request ka hissa nahi hain. Inhe implement nahi karna.
