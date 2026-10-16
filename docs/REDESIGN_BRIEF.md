# Redesign brief — Crystal Matte Minimalism (zadání majitele, 2026-10-16)

Zdroj pravdy pro redesign. Zkrácený přepis zadání majitele; nic z požadavků nevynecháno.
**Fáze A (design) musí skončit schválením majitele, než začne fáze B (implementace Android UI).**

## Git stav k 2026-10-16
- PR #2 sloučeno do `main` jen po `5a96934` (merge `f26f62f`).
- `claude/ink-rework` (`f63cf46`) má **18 commitů, které v `main` nejsou** (D9 PULSE všude, posuvníky, průhledný pruh, prstenec 3 ze středu…).
- `claude/crystal-redesign` = nová pracovní větev z `f63cf46`, obsahuje vše. Redesign pokračuje zde. Nemergovat bez souhlasu.

## Filozofie
MAXIMUM MINIMALISM. MAXIMUM CAPABILITY. Intelia je neviditelná, když ji nepotřebuji, a neuvěřitelně schopná, když ji použiji.
Nezaměňovat zjednodušení UI za odstranění schopností. Nezaměňovat kreativitu za komplikovanost.

## Audit (první průchod, screenshoty z telefonu 2026-10-16)
Příčina celku: každá plocha ukazuje **všechny své možnosti naráz** a každý prvek má vlastní dekoraci (kruh, nit, popisek, podklad). Chybí jedna hierarchie a jeden materiál.
- **ARC**: tři soustředné čárkované kružnice + popisky APLIKACE/FLOW/ZKRATKY přes obrazovku + nit od LENS + bílé podložky ikon + obří nadpis vybrané položky nahoře („ZKRATKY · PUSŤ A OTEVŘE SE“ / „Spořič obrazovky“). Působí jako kruh, ne oblouk. Ikona vybrané zkratky v prstenci 3 překrývá popisek „FLOW“.
- **PULSE menu**: panel přes ~half obrazovky; budík, baterie, média, slider hlasitosti, Vibrace, Svítilna, Snímek, Zamknout, Oznámení, Rychlé nast., Wi-Fi, Internet, Bluetooth, Zvuk, Displej, Sbalit. Klon rychlého nastavení, opakuje systémové údaje (baterie, budík).
- **PULSE idle**: terakotový prstenec baterie + tečka kolem kamery — opakuje stavový řádek.
- **Studio**: jeden dlouhý scroll (Vzhled, Plocha, ARC, PULSE, Pohyb, Výchozí plocha, Skryté, O aplikaci) pod permanentním náhledem ~300 dp; seznamy aplikací s ↑/✕ inline.
- Home v klidu je v pořádku (hodiny, datum, Essentials kolem LENS), jen Essentials působí jako dock a podtržítko pod LENS je navíc.
- Terakota je všude — nemá být povinnou identitou.

## Požadavky
### Design systém (Crystal Matte Minimalism)
Krystalická přesnost (hrany, proporce, jemné světelné přechody — ne kreslené krystaly), matné povrchy s decentním rozptylem (ne šedé obdélníky), velmi subtilní translucence, čistá geometrie, prémiová typografie, přirozený pohyb, max. čitelnost.
Inspirace (ne kopie): Nothing OS (disciplína), Niagara (minimalismus, ergonomie), MT Capsule (dynamická interakce), nativní Android.
NE: AI slop, neony, zářící kruhy, náhodné gradienty, velké skleněné karty, podklad pod každým prvkem, přehnaně zaoblené dashboardy, čárkované prstence, zbytečná typografická hierarchie, moc fontů, velké navigační panely, permanentní textové instrukce, animace při každém návratu na plochu.
Definovat: barvy, typografie, velikosti ikon, spacing, radiusy, materiály, kontrast, motion, haptika, stavy, přístupnost. Výchozí systémové/tlumené tóny; akcent např. studená modrá, stříbrná nebo uživatelův. Nativní Compose, žádný permanentní blur.

### Home
Systémová tapeta, edge-to-edge, hodiny + datum, LENS dole, volitelně oblíbené, volný prostor pro gesta.
Onboarding: **Pure** (jen LENS) / **Essentials** (pár aplikací kolem LENS, nesmí působit jako dock) / **Custom** (vlastní aplikace, pozice, rozsah). Systémové ikony nebo icon pack. Vše vypnutelné.

### LENS + ARC
Jedno gesto, **jeden viditelný nízký oblouk** nad LENS, víc sad akcí. Žádné soustředné kruhy, nápisy vrstev, nit, podložky ikon. Jemné zvýraznění, název jen během výběru, mikropohyb + haptika, návrat na LENS = zrušit.
Víc než 5 akcí bez zobrazení všech: prozkoumat **A** přepínatelné vrstvy jednoho oblouku (další pásmo vzdálenosti), **B** stabilní sady s pevnými pozicemi, **C** vlastní lepší řešení. Přesně popsat výběr, přepnutí sady, potvrzení, zrušení, chybějící cíl. Ergonomii ověřit na telefonu.

### PULSE 2.0 — hlavní jádro
- **Idle**: nic viditelného (žádná baterie, žádný budík, žádná záře), jen neviditelná dotyková oblast kolem kamery.
- **Live**: velmi kompaktní kapsle navazující na kameru, šířka podle obsahu, auto návrat do idle (hudba, časovač, vybrané oznámení, nabíjení jen na přání, vlastní FLOW událost).
- **Tap**: relevantní obsah (hraje hudba → kompaktní media) jinak vlastní sada rychlých akcí. Žádný výpis systémových nastavení.
- **Hold**: **Global ARC** — stejná architektura akcí jako LENS, vlastní geometrie a konfigurace; ověřit ergonomii horního gesta (žádný tah přes celý displej).
- **Expanded**: jeden kontextový blok + vybrané aplikace/FLOW + pár akcí.
- Vlastní akce: aplikace, FLOW, Search, veřejný shortcut, média, svítilna, screenshot (podporovaná systémová akce), zvukový režim, systémový panel, vlastní karta. Gesta: tap, double tap, hold, swipe vlevo/vpravo (+ další podporovaná).
- **Jeden Action Engine** pro LENS, ARC, FLOW, PULSE (např. FLOW `vlak` přiřaditelný do Search, dolního i horního ARC, hold PULSE…). Malá rozšiřitelná vrstva, ne velký framework.
- Respektovat Android: co nejde přepnout, jen otevřít panel; overlay/AccessibilityService volitelné; média/oznámení jen s oprávněním; nikdy nepředstírat provedení; minimum soukromých dat.

### Studio / Nastavení
Progressive disclosure: první úroveň = kategorie se souhrnem (Vzhled, Domovská plocha, Gesta a ARC, PULSE, FLOW, Systém a pokročilé — počet může být jiný). Detaily uvnitř.
Vzhled: téma, akcent, fonty a hodiny, ikony (systémové/monochrome/pack), průhlednost a materiály, animace. Home: Pure/Essentials/Custom, aplikace, umístění LENS, hodiny a datum, gesta. ARC: sady, akce, pořadí, výběrové gesto, haptika, citlivost. PULSE: povolení, stav služby, kalibrace, gesta, akce, události, chování kapsle, vzhled. FLOW: seznam, editor, akce, přiřazení.
Živý náhled jen u vzhledu, kompaktní/rozbalitelný nebo přímá editace na Home. Zachovat všechny konfigurace nebo migrovat s testy.

### Motion a haptika
Plynulé, přirozené, krátké; elastické jen kde pomáhá; haptika podle významu. PULSE se rozbaluje z kamery, ARC navazuje na prst, Search navazuje na LENS. Žádné permanentní animace v idle, minimum blur, Reduced Motion, fallbacky.

### Přístupnost a ovládání
Jedna ruka, levá i pravá ruka, velké písmo, TalkBack, klávesnice, haptika, systémové navigační oblasti, různé tapety/kontrast, světlý i tmavý režim, telefony bez výřezu, omezení overlaye nad citlivými dialogy. Žádné náhodné spuštění; každé složité gesto má alternativu; ergonomie > efekt.

## Postup
### Fáze A — design review (pak STOP a schválení)
- A1 krátký audit (viz výše, doplnit Search/Drawer/FLOW/PULSE nad cizí aplikací).
- MT Capsule (`com.pryshedko.mtisland`): veřejné info + bezpečné prohlédnutí na telefonu, **nic v ní neměnit**. Srovnání MT Capsule / současný PULSE / PULSE 2.0 včetně slabin MT Capsule. Nekopírovat kód, grafiku, animace ani layout.
- A2 2–3 opravdu odlišné směry (např. Quiet Crystal / Matte Ambient / Minimal System — rozdílné principy materiálu, rozvržení, pohybu), vybrat a zdůvodnit.
- A3 obrazovky: Home Pure, Home Essentials, LENS aktivní, ARC výběr, ARC přepnutí sady, PULSE idle, PULSE hudba, PULSE expanded, PULSE Global ARC nad cizí aplikací, Studio přehled, Studio PULSE, Studio ARC; + Search a FLOW pro konzistenci.
- A4 interaktivní prototyp (Claude Design nebo izolovaný HTML/JS mimo Android) se skutečnými stavy a přechody, ne koláž.
- A5 specifikace: rozměry, barvy, typografie, komponenty, motion, haptika, interakce, realizovatelnost.

### Fáze B — po schválení
1 design systém a komponenty → 2 Home + LENS → 3 ARC → 4 PULSE → 5 Studio → 6 Search/FLOW/Drawer → 7 regrese a výkon.
Každá etapa: commit, testy, build, kontrola na telefonu, screenshot před/po.
Regrese na telefonu: Home a Recents, LENS, ARC otevření/výběr/zrušení, FLOW explicitně, PULSE idle/expanded/v cizí aplikaci, média, klávesnice, nastavení a persistence, TalkBack, kontrast, tmavé/světlé, rotace, výkon. Výkon měřit na release (debug = jen debug). Telefon bez výřezu není k dispozici — jen logicky/emulátor, fyzický test neoznačovat za provedený.

### Zachovat
FLOW, ARC přiřazení, oblíbené, vzhled, kalibrace PULSE, zkratky, přístupnost, ostatní moduly. Změna formátu dat = migrace + testy.

### Kritéria
Minimalistický, přehledný, powerful, originální, konzistentní, plynulý, přizpůsobitelný, nativní.

## Odložené drobnosti z předchozí session
- Majitel: TalkBack na posuvnících po `0ed5e3d`; verdikt na průhledný pruh menu PULSE (pravděpodobně zanikne redesignem).
- Výkon na release buildu; telefon bez výřezu jen zapsat.
