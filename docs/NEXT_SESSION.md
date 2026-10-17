Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~150k. Žádní agenti, soubory nečti celé, výstupy filtruj (pozor: `read_console_messages` v Browser pane vrací
celý data: URL stránky — nepoužívat).

Stav (2026-10-17): větev `claude/crystal-redesign`. Fáze A Veil 3.0 + revize 1 (hutnější Focus vrstva, sdílené rozostření Home) — D15.
briefu „Veil 3.0“: prototyp `docs/design/veil3-prototype.html` (artefakt https://claude.ai/artifact/UCthmYs5TCqSjsdSFbh9EF),
záznam D15 (PROPOSED). Produkční kód beze změny, build ani telefon se tuto session nespouštěly.
Ověřeno: `node --check` skriptu prototypu OK; stránka se vykreslila v prohlížeči (Home + panel voleb). Neověřeno:
proklikání všech scén prototypu, telefonní šířka stránky.

Další krok: nejdřív se zeptej majitele na verdikt k prototypu (schváleno / změny). Po schválení fáze B podle D15
a pořadí v prototypu (sekce „Pořadí implementace“), každý celek: testy → build → telefon → malý commit + push:
1. Tokeny Veil 3.0 v `ui/Veil.kt` (hrana nahoře světlá → dole akcent, presety jako varianty, krytí 64/82/93 %).
2. Home: výchozí hodiny „Čisté“ jen pro nové instalace (`sanitized()`, žádná migrace existujících dat), popisky bez překryvu.
3. Editor: plocha 86 %, 48 dp lišta + spodní podnos (`HomeEditor.kt` `EditorBar`, `BAR_HEIGHT`).
4. Studio 5 sekcí + hledání v nastavení (`ui/Studio.kt`), sdílené definice voleb s inspektory.
5. Dědičnost Globální / Vlastní ↺ v inspektorech (`HomeInspector.kt`).
Build/telefon: viz předchozí postup (JAVA_HOME lesnik-toolchain jdk-17, `-Dorg.gradle.jvmargs=-Xmx2g`, záloha
`intelia.xml`, `install -r`, před každým `input` ověřit `topResumedActivity`).
Netracked: `docs/screens/v0.1.0-2026-10-17b.rar` (majitelův, nemazat ani necommitovat bez dotazu).
