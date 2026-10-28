# AU Notes --- ACCOUNTING Feature Specification (Milk Dairy Hisab)

## 1. Feature ka purpose

AU Notes app ke andar **ACCOUNTING** naam ka naya feature add karna hai.
Iske andar pehla module **Milk Dairy Hisab** hoga, jisse user roz subah
aur shaam dairy par bheje gaye doodh ki quantity aur amount record kar
sake.

User ka hisaab 10-day periods mein hota hai: - Date 1 se Date 10 - Date
11 se Date 20 - Date 21 se month ke aakhri din tak

Month ka aakhri period hamesha us month ki actual last date par khatam
hoga. Jaise 28, 29, 30 ya 31 --- jo bhi us month ki last date ho.

**Important:** Ye sirf ek temporary table nahi hai. Har month ka page
aur uska saara data permanently save rehna chahiye, taaki user purane
months khol kar dekh sake aur kisi bhi month ka PDF export kar sake.

## 2. Existing project ko preserve karna

-   Existing AU Notes app ka UI/design aur doosre features unchanged
    rahen.
-   ACCOUNTING ko existing app mein ek naya feature/module ki tarah add
    karo.
-   Poore app ko rewrite mat karna; sirf required files aur integration
    points mein targeted changes karna.
-   App Kotlin + Jetpack Compose Android app hai, isliye feature native
    Android UI mein ho --- WebView/HTML page ke roop mein nahi.
-   Existing navigation, theme aur design language ko follow karo.
-   Offline data entry aur saved records internet ke bina bhi kaam karne
    chahiye.
-   AI ka use totals ko explain/summarize karne ke liye ho sakta hai,
    lekin basic hisaab AI ya internet par depend nahi hona chahiye.

## 3. ACCOUNTING screen aur first launch behaviour

1.  App mein **ACCOUNTING** naam ka entry point add karo.
2.  User pehli baar ACCOUNTING open kare, to app device ki current local
    date se current month aur year identify kare.
3.  Pehli screen automatically current month ki ho. User ko manually
    month select karne ki zaroorat na pade.
4.  Example: agar user 28 October 2026 ko pehli baar feature kholta hai,
    to **October 2026** ka page khule.
5.  Current date ko live device date se determine karo; date hardcode
    mat karna.
6.  Month title clearly dikhe, jaise `October 2026`.
7.  Previous Month aur Next Month controls hon, taaki user purane aur
    future months dekh sake.
8.  Current month par wapas aane ke liye `Current Month` /
    `Aaj ka Mahina` shortcut diya ja sakta hai.
9.  Next Month open karne par us month ka page available ho. Agar us
    month mein abhi koi entry nahi hai, to blank dates ke saath empty
    page dikhe; existing data overwrite na ho.
10. Month change karne par har month ka apna alag data load ho. Ek month
    ka data doosre month mein mix nahi hona chahiye.

## 4. Milk Dairy Hisab --- table ka exact structure

Uploaded `MILK_EXAMPLE.md` mein table ka base structure ye hai:
`DATE | QUANTITY | AMOUNT | QUANTITY | AMOUNT`. Ismein pehla
Quantity/Amount pair subah ke liye aur doosra Quantity/Amount pair shaam
ke liye use hoga. fileciteturn0file0L5-L10

Screen par table ko is tarah samjha aur label kiya jaye:

  -------------------------------------------------------------------------
  Date         Subah      Subah      Shaam      Shaam      Daily      Daily
            Quantity Amount (₹)   Quantity Amount (₹)      Total      Total
                (KG)                  (KG)              Quantity Amount (₹)
                                                            (KG) 
  ------- ---------- ---------- ---------- ---------- ---------- ----------
  1              4.0        200        4.5        247        8.5        447

  2              2.0        150        3.4        284        5.4        434

  3              ---        ---        ---        ---          0          0
  -------------------------------------------------------------------------

**Important UI requirement:** - Har calendar date ki apni row ho. - Date
column mein month ki actual dates `1, 2, 3 ...` order mein hon. - User
ne example mein dates ka order galti se idhar-udhar likha ho sakta hai;
app dates ko hamesha chronological order mein dikhaye. - Pehla quantity
aur amount pair **Subah** ka hoga. - Doosra quantity aur amount pair
**Shaam** ka hoga. - Table horizontal space zyada leti ho, to phone par
readable native layout use karo: horizontally scrollable table ya har
date ka compact expandable row/card. Labels clear rehne chahiye;
morning/evening values kabhi mix na hon. - Month ke saare din
visible/accessible hon, aur neeche totals ka summary section ho. -
Quantity KG mein aur amount ₹ mein record ho. - Decimal quantity support
ho, jaise `4`, `4.5`, `3.25`. - Amount rupees mein decimal values
support kar sakta hai agar zaroorat ho. - Blank field ka matlab abhi
entry nahi hui; blank ko user ki permission ke bina fake value se fill
mat karna. - Date ke liye alag se manually row create karna zaroori na
ho; month ke saare dates automatically generate hon.

## 5. Daily entry aur editing

Har date ke liye user ye values enter/edit kar sake:

### Subah

-   Subah Quantity (KG)
-   Subah Amount (₹)

### Shaam

-   Shaam Quantity (KG)
-   Shaam Amount (₹)

Rules: - User sirf subah, sirf shaam, ya dono sessions ki entry kar
sake. - Kisi ek field ko blank chhodne se doosre session ki entry block
na ho. - Entry save hote hi local database mein persist ho, ya clear
Save action se save ho; screen band karne par unsaved data silently lose
nahi hona chahiye. - Month switch karne, app close/reopen karne, ya
phone restart ke baad saved values wapas load hon. - User purani date ki
entry baad mein edit kar sake. - Edit karne par daily, 10-day aur
monthly totals dobara calculate hon. - User se confirm kiye bina kisi
saved record ko delete/overwrite mat karna. - Agar delete action diya
jaye, to confirmation zaroor ho. - Data entry ke dauran decimal keyboard
aur suitable numeric input use karo. - Negative quantity/amount ko
default roop se reject karo; correction ke liye user existing entry edit
kar sakta hai.

## 6. 10-day period structure aur automatic totals

Har month ko in periods mein divide karo:

### Period 1: Date 1--10

-   Dates 1 se 10 tak ki entries ka total.
-   Subah quantity total.
-   Subah amount total.
-   Shaam quantity total.
-   Shaam amount total.
-   Combined quantity total.
-   Combined amount total.

### Period 2: Date 11--20

-   Dates 11 se 20 tak bilkul isi tarah totals.

### Period 3: Date 21--Month End

-   Date 21 se month ki actual last date tak totals.
-   Month ki last date 28, 29, 30 ya 31 ho sakti hai.
-   Period ko fixed 10 din maan kar month ke end ke baad ki dates kabhi
    generate mat karna.

**Examples:** - February 2026: Periods `1–10`, `11–20`, `21–28`. -
February 2028: Periods `1–10`, `11–20`, `21–29` (leap year). - April
2026: Periods `1–10`, `11–20`, `21–30`. - October 2026: Periods `1–10`,
`11–20`, `21–31`.

Har period ka heading aur summary clearly dikhna chahiye, jaise
`1–10 October Total`, `11–20 October Total`, `21–31 October Total`.

Period totals automatically update hon jab bhi us period ki kisi date ki
entry add ya edit ho.

## 7. Calculation rules --- AI aur arithmetic

Hisaab exact hona chahiye. Addition aur totals ko deterministic
Kotlin/database logic se calculate karo; AI ko arithmetic ka single
source of truth mat banao.

Har date ke liye:

-   **Daily Total Quantity** = Subah Quantity + Shaam Quantity
-   **Daily Total Amount** = Subah Amount + Shaam Amount

Har 10-day period ke liye: - Subah Quantity Total = period ki sabhi
Subah Quantity entries ka sum. - Subah Amount Total = period ki sabhi
Subah Amount entries ka sum. - Shaam Quantity Total = period ki sabhi
Shaam Quantity entries ka sum. - Shaam Amount Total = period ki sabhi
Shaam Amount entries ka sum. - Period Total Quantity = Subah Quantity
Total + Shaam Quantity Total. - Period Total Amount = Subah Amount
Total + Shaam Amount Total.

Poore month ke liye: - Monthly Subah Quantity = poore month ki Subah
Quantity entries ka sum. - Monthly Subah Amount = poore month ki Subah
Amount entries ka sum. - Monthly Shaam Quantity = poore month ki Shaam
Quantity entries ka sum. - Monthly Shaam Amount = poore month ki Shaam
Amount entries ka sum. - **Monthly Total Quantity** = Monthly Subah
Quantity + Monthly Shaam Quantity. - **Monthly Total Amount** = Monthly
Subah Amount + Monthly Shaam Amount.

Blank values ko totals mein zero contribution maana ja sakta hai, lekin
UI mein blank entry aur explicitly entered `0` ko alag pehchanna
possible rehna chahiye.

### AI ka role

-   `AI Total` / `Calculate Hisab` button diya ja sakta hai.
-   Button press karne par app saved entries ko use karke period aur
    monthly summary prepare kare.
-   AI summary ko simple Hinglish mein explain kar sakta hai, jaise:
    kitna KG doodh gaya, subah/shaam ka breakdown aur total amount.
-   Final numbers app ke verified calculations se aayen; AI se numbers
    invent ya independently recalculate karwa kar final total mat banao.
-   AI unavailable ho, quota khatam ho ya internet na ho, tab bhi totals
    normal kaam karte rahen.
-   AI ko user ki entries ko khud se edit, delete ya overwrite karne ki
    permission na ho.
-   Agar AI koi summary deta hai, to us summary ke saath actual
    period/month aur calculation ka source clear ho.

## 8. Month-end behaviour aur next month

-   Har month ki last date system calendar se calculate ho.
-   Month-end par current month ka total us month ki last date tak
    include ho.
-   February, leap year, 30-day aur 31-day months sab sahi handle hon.
-   Naya month aane par next month ka separate page automatically
    available ho.
-   Naye month ki entries purane month ki entries ko replace ya reset na
    karen.
-   App current date ke basis par current month open kare, lekin user
    previous/future month browse kar sakta ho.
-   Month switch karne se pehle current month ki saved entries preserve
    hon.
-   Month-end total automatically available ho; user ko manually last
    date set karne ki zaroorat na ho.
-   Koi automatic data deletion, monthly reset ya old month cleanup nahi
    hona chahiye.

## 9. Data persistence aur protection --- bahut important

**Data erase nahi hona chahiye.** Ye feature daily accounting record ke
liye hai.

-   Entries ko persistent local database (preferably existing project
    architecture ke compatible Room/database layer) mein save karo.
-   Sirf Compose state, temporary list, in-memory object ya screen state
    mein data rakhna enough nahi hai.
-   Har record ko month/year aur exact date ke saath uniquely identify
    karo.
-   Subah aur shaam ki values ek hi date ke record mein separate fields
    ke roop mein ya equivalent reliable structure mein store hon.
-   App close/reopen, process death, device restart, navigation aur
    month switching ke baad records available hon.
-   Existing database ho to usi architecture ko follow karo aur safe
    schema migration use karo.
-   Database version change ke dauran destructive migration ya aisa code
    use mat karo jo purane records delete kar de.
-   Kisi feature update ke baad purane milk records ko silently clear
    mat karna.
-   Save/edit ke baad totals database ke latest saved data se refresh
    hon.
-   PDF export karne se database data delete ya modify na ho.
-   Future mein backup/restore add kiya jaye to usse records transfer
    karne ka safe tareeqa mile; filhaal core requirement local
    persistence aur zero automatic deletion hai.
-   Agar storage failure aaye to user ko clear error dikhao; success
    message tabhi do jab data successfully save hua ho.

## 10. Monthly PDF export

Har month ke page par **Export PDF** button hona chahiye.

PDF mein ye information ho: 1. Heading: `Milk Dairy Hisab` 2. Month aur
year. 3. Daily table: - Date - Subah Quantity (KG) - Subah Amount (₹) -
Shaam Quantity (KG) - Shaam Amount (₹) - Daily Total Quantity (KG) -
Daily Total Amount (₹) 4. Period 1 (1--10) ka summary. 5. Period 2
(11--20) ka summary. 6. Period 3 (21--month end) ka summary. 7. Final
monthly totals: - Subah quantity aur amount - Shaam quantity aur
amount - Combined monthly quantity - Combined monthly amount

PDF requirements: - Month ki har date include ho, chahe us din entry
blank ho. - PDF long month ke liye multiple pages support kare; rows
truncate ya disappear nahi hon. - Table headings har relevant page par
repeat ho sakein. - Text aur numbers readable hon; columns cut na hon. -
`₹` aur decimal values sahi render hon. - Export ki hui file actual
device storage/Android document picker ke through user-accessible
location mein save ho. - Export complete hone par user ko file name aur
save location/path dikhayein. - Suggested filename:
`Milk_Hisab_October_2026.pdf`. - Export karte waqt current month ka data
hi export ho; purane month se switch karne par usi selected month ka PDF
bane. - Export fail ho to clear error message do aur user ke saved
accounting data ko untouched rakho.

## 11. Suggested screen layout

### ACCOUNTING home

-   Heading: `ACCOUNTING`
-   Feature card/button: `Milk Dairy Hisab`
-   Zaroorat ke mutabiq future mein doosre accounting modules add karne
    ke liye extensible structure, lekin abhi unnecessary extra modules
    mat banao.

### Milk Dairy Hisab month screen

-   Previous Month arrow
-   Month + Year
-   Next Month arrow
-   `Current Month` shortcut
-   Period 1 summary (1--10)
-   Period 2 summary (11--20)
-   Period 3 summary (21--month end)
-   Date-wise entry table/list
-   Monthly totals card
-   `AI Total` / `Hisab Summary` action
-   `Export PDF` action

Mobile screen par saari values readable hon. Agar complete table ek
screen width mein fit nahi hoti, to horizontal scrolling ya compact date
cards use karo. Subah aur shaam ke fields visually aur text labels se
clearly separate rahen.

## 12. Acceptance tests --- implementation complete tabhi maana jaye

1.  First install/open par current local month khulta hai.
2.  Har month mein actual calendar ke mutabiq sahi number of dates hoti
    hain.
3.  Har date par Subah Quantity, Subah Amount, Shaam Quantity aur Shaam
    Amount separately enter kiye ja sakte hain.
4.  Example row: Subah `4 KG / ₹200`, Shaam `4.5 KG / ₹247` se daily
    total `8.5 KG / ₹447` banta hai.
5.  Date 1--10 ka period total sahi aata hai.
6.  Date 11--20 ka period total sahi aata hai.
7.  Date 21 se month end tak ka period total sahi aata hai.
8.  February 2026 mein 28 dates aur February 2028 mein 29 dates hoti
    hain.
9.  April mein 30 aur 31-day month mein 31 dates hoti hain.
10. Month ki entries edit karne par daily, period aur monthly totals
    update hote hain.
11. App restart ke baad saved data wapas aata hai.
12. Previous/Next Month browse karne par har month ka apna data dikhta
    hai.
13. Future month kholne se current ya previous month ka data overwrite
    nahi hota.
14. AI/internet unavailable hone par bhi all arithmetic totals work
    karte hain.
15. Monthly PDF mein daily entries, all three period summaries aur final
    monthly total shamil hote hain.
16. PDF multi-page month ko truncate nahi karta aur user file ko device
    par access kar sakta hai.
17. Feature add karne se existing AU Notes ke notes, editor, exports aur
    doosre features break nahi hote.
18. Koi automatic month-end reset ya saved data deletion nahi hota.

## 13. Implementation instructions for coding AI

Implementation se pehle existing project structure inspect karo: -
Existing navigation/screens - Existing database/entities/DAO/repository
pattern - Existing PDF/export utilities - Existing theme/components

Uske baad sirf required changes karo. Existing architecture se match
karne wale naye screen, data model, database migration, repository aur
PDF export integration add karo. Existing functionality ko duplicate ya
replace mat karo.

Code likhne ke baad: - Kotlin compilation errors check karo. -
Calendar/month-boundary calculations test karo. - Daily, 10-day aur
monthly totals verify karo. - Persistence aur PDF export test karo. - Jo
files change hui hain unki list aur har change ka purpose batao. - Agar
kisi existing implementation ki wajah se exact integration unclear ho,
to guess karke poora app rewrite karne ke bajaye pehle relevant code
inspect karo.

**Final priority order:** data safety and persistence → correct
calculations and date boundaries → separate morning/evening entries →
monthly navigation → complete PDF export → optional AI summary.
