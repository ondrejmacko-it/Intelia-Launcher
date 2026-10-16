# Prompt pro další session — Redesign, fáze A (design review)

Zkopíruj celý blok níže jako první zprávu nové session (pracovní složka = tento repozitář).

---

Pokračujeme na **Intelia Launcheru** (`AGENTS.md` platí). **Odpovídej česky.** Rozpočet 200k tokenů, cíl ~120k, žádní zbyteční agenti, soubory nečti opakovaně celé.

**Zadání je v `docs/REDESIGN_BRIEF.md`. Přečti ho celé jako první, je to zdroj pravdy.** Redesign „Crystal Matte Minimalism“: ARC jako jeden oblouk se sadami, PULSE 2.0 jako jádro (idle nic, live kompaktní, tap kontext, hold Global ARC), jeden Action Engine, Studio s progressive disclosure.

Git: pracuj na větvi `claude/crystal-redesign` (vznikla z `claude/ink-rework` `f63cf46`, obsahuje 18 commitů, které v `main` nejsou; PR #2 do `main` bylo sloučeno jen po `5a96934`). Nemergovat do `main`, nic destruktivního. Na začátku `git fetch` a ověř stav.

Dělej **jen fázi A**:
1. Doplň audit: screenshoty Search, Drawer, FLOW a PULSE nad cizí aplikací (Home, ARC, PULSE menu a Studio už jsou popsané v briefu).
2. MT Capsule (`com.pryshedko.mtisland`): veřejné info + prohlédnutí na telefonu, **nic neměnit**. Pak srovnání MT Capsule / PULSE / PULSE 2.0.
3. 2–3 odlišné vizuální směry, jeden doporuč a zdůvodni.
4. Interaktivní HTML prototyp (Artifact) s 12 obrazovkami z briefu (A3) a skutečnými přechody: ARC výběr a přepnutí sady, PULSE idle → live → expanded → Global ARC, Studio přehled → detail.
5. Specifikace (A5) zapiš do `docs/DESIGN_SYSTEM.md`, rozhodnutí do `docs/DECISIONS.md`.
6. **STOP: předlož majiteli ke schválení.** Android UI zatím neměň.

Telefon: Nothing Phone (3), Android 16, 1080×2392. adb `C:\Users\Ondra\AppData\Local\lesnik-toolchain\sdk\platform-tools\adb.exe` (v Git Bash `MSYS_NO_PATHCONV=1`), JDK `C:\Users\Ondra\AppData\Local\lesnik-toolchain\jdk-17.0.20.1+1`; cesty ověř. Před každým `input` ověř `topResumedActivity`. Lab (`dev.intelia.launcher.lab`) je výchozí HOME, 3 prstence. LENS 540,2193 (ARC: `input motionevent DOWN`, MOVE nahoru, UP). Menu PULSE klepnutím na 540,68. Studio podržením plochy (swipe 540 1100 540 1100 900). Služby jen číst, nastavení cizích aplikací neměnit.

Na konci: commit a push do `claude/crystal-redesign`, `docs/STATUS.md` jen s ověřenými fakty, nový prompt v `docs/NEXT_SESSION.md`.
