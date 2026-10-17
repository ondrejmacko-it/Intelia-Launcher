Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~150k. Žádní agenti, soubory nečti celé, výstupy filtruj (pozor: `read_console_messages` v Browser pane vrací
celý data: URL stránky — nepoužívat).

Stav (2026-10-17): větev `claude/crystal-redesign`. Veil 3.0 fáze A + revize 1 a 2 — D15 **SCHVÁLENO** majitelem.
Prototyp `docs/design/veil3-prototype.html` (artefakt https://claude.ai/artifact/UCthmYs5TCqSjsdSFbh9EF). Produkční
kód beze změny, build ani telefon se nespouštěly. Ověřeno v desktop prohlížeči: 9 scén × tapeta tmavá/světlá/barevná
× UI tmavé/světlé × Omezit pohyb (materiály mají podklad, inspektor nepřekrývá prvek, Search má fokus), skutečné
tažení slideru velikosti hodin min→max, psaní „rano“ v Search + Esc. Neověřeno: telefonní šířka stránky, blur,
oprávnění a výkon na Androidu.

Další krok: fáze B podle D15 a pořadí v prototypu (sekce „Pořadí implementace“), každý celek: testy → build →
telefon → malý commit + push:
1. Tokeny Veil 3.0 v `ui/Veil.kt` (hrana nahoře světlá → dole akcent, presety jako varianty, krytí bez blur Light 64 / Dense 80 / Focus 74 %, s blur 48/66/60 %; materiálová třída vždy základ + hustota).
2. Home: výchozí hodiny „Čisté“ jen pro nové instalace (`sanitized()`, žádná migrace existujících dat), popisky bez překryvu.
3. Editor: plocha 86 %, 48 dp lišta + spodní podnos (`HomeEditor.kt` `EditorBar`, `BAR_HEIGHT`).
4. Studio 5 sekcí + hledání v nastavení (`ui/Studio.kt`), sdílené definice voleb s inspektory.
5. Dědičnost Globální / Vlastní ↺ v inspektorech (`HomeInspector.kt`).
Build/telefon: viz předchozí postup (JAVA_HOME lesnik-toolchain jdk-17, `-Dorg.gradle.jvmargs=-Xmx2g`, záloha
`intelia.xml`, `install -r`, před každým `input` ověřit `topResumedActivity`).
Netracked: `docs/screens/v0.1.0-2026-10-17b.rar` (majitelův, nemazat ani necommitovat bez dotazu).
