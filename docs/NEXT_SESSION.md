Pokračujeme na Intelia Launcheru (`AGENTS.md` platí). Odpovídej česky, stručně. Tvrdý limit 250 000 tokenů na session,
cíl ~150k. Žádní agenti, soubory nečti celé, výstupy filtruj (pozor: `read_console_messages` v Browser pane vrací
celý data: URL stránky — nepoužívat).

Stav (2026-10-17): větev `claude/crystal-redesign`. D15 / Veil 3.0 schváleno. **B1 hotovo** (viz `docs/STATUS.md`):
jeden materiál v `ui/Veil.kt` (`veilTokens` + `VeilPreset`, krytí 64/80/74 %, s blur 48/66/60 %), sdílená depth
vrstva v `ui/Depth.kt` (`VeilDepth`, `VeilDepthSource`, `veilBackdrop`, `veilScrim`), napojeno v `HomeRoot`, Search,
drawer a `AppOptionsSheet`. Telefon: blur Home funguje (RenderEffect), cross-window blur na Phone (3) není. Měřeno:
blur přidává jank jen během přechodu (9,4 → 12,0 %), proto vypnut v úsporném režimu a při Omezit pohyb.
Neověřeno: světlý motiv, fallback na telefonu, Dense list nad Home, zařízení s cross-window blur.

Další krok — **B2 Home + Editor** (D15, prototyp `docs/design/veil3-prototype.html`, sekce „Pořadí implementace“):
1. Home: výchozí hodiny „Čisté“ jen pro nové instalace (`LauncherSettings.sanitized()` / výchozí hodnoty, žádná
   migrace existujících dat), popisky ve výchozím stavu vypnuté a nikdy se nepřekrývají.
2. Editor: Home zmenšená na 86 %, 48 dp horní lišta (zrušit, zpět/znovu, název, Hotovo) a spodní podnos (Přidat,
   Předlohy, Rozložení, Vzhled) jako Veil Light (`HomeEditor.kt` `EditorBar`, `BAR_HEIGHT`); lišta nesmí krýt horní hodiny.
3. Inspektory: umístění podle usazené pozice prvku po 86 % škále, na straně s víc místem; Home pod nimi zůstává ostrá
   (žádný `VeilDepthSource`).
Každý celek: testy → build → telefon (screenshoty, i světlý motiv) → malý commit + push. Studio (B4) a dědičnost (B5) ne.
Build/telefon: JAVA_HOME `C:/Users/Ondra/AppData/Local/lesnik-toolchain/jdk-17.0.20.1+1`, `-Dorg.gradle.jvmargs=-Xmx2g`,
adb v `lesnik-toolchain/sdk/platform-tools`, `MSYS_NO_PATHCONV=1`, záloha `intelia.xml`, `install -r`, před každým
`input` ověřit `topResumedActivity`.
Netracked: `docs/screens/v0.1.0-2026-10-17b.rar` (majitelův, nemazat ani necommitovat bez dotazu).
