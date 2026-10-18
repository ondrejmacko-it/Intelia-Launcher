# Intelia Launcher

[English](README.md) · **Čeština**

> **Tvůj telefon. Tvoje pravidla.** Rychlá, tichá, lokální domovská obrazovka pro Android, napsaná od nuly v Kotlinu
> a Jetpack Compose. Není to fork jiného launcheru.

**Stav: 0.1.0 Beta 1 — předběžné vydání.** Otestováno na jednom telefonu (Nothing Phone (3), Android 16). Počítej
s chybami; viz [Známá omezení](#známá-omezení) a [poznámky k vydání](docs/release/v0.1.0-beta.1/RELEASE_NOTES.md).

Žádné účty, žádný přístup k síti, žádná analytika, žádná práce na pozadí. Vše volitelné je vypnuté, dokud to nezapneš.

## Co umí

| Pilíř | Co dostaneš |
|---|---|
| **LENS** | Jeden ovládací prvek dole na ploše. Klepnutí = hledání, tah nahoru = seznam aplikací, podržení = ARC. |
| **ARC** | Podrž LENS, sjeď na slot, pusť. Několik pojmenovaných sad aplikací, FLOW příkazů a zkratek aplikací (zkratky jen když je Intelia výchozí plocha). Puštění zpět na LENS akci zruší. |
| **FLOW** | Pojmenované příkazy (např. `vlak`), které otevřou 1–4 aplikace v tvém pořadí. Psaní jen ukazuje náhled; FLOW se spustí tlačítkem **Spustit**, **Enterem** na přesném názvu nebo puštěním na jeho slotu v ARC. Nikdy během psaní. |
| **PULSE** | Tichá kapsle kolem fotoaparátu: nabíjení, blízký budík, hudba (s přístupem k oznámením), rychlé akce a gesta. Výchozí jen na ploše; všude jen se zapnutou volitelnou službou přístupnosti. Poloha kamery se zjistí sama nebo se kalibruje po 0,5 dp. |
| **Global ARC** | Se zapnutou službou PULSE: podrž oblast kamery nad libovolnou aplikací a sjeď na slot ARC. |
| **Hledání** | Lokální, bez ohledu na diakritiku (`kalkulack` najde *Kalkulačka*), řazené podle začátku, začátku slova, iniciál a podřetězce. |
| **Studio** | Všechna nastavení v pěti oblastech (Vzhled, Plocha, Ovládání, Akce a FLOW, Systém a data), s živým neinteraktivním náhledem a hledáním, které skočí přímo na řádek. |
| **Editor plochy** | Mřížka, volné nebo hybridní umístění, předlohy, inspektory položek (ikona, popisek, hodiny, datum, poloha LENS), zpět/znovu. |
| **Vzhled** | Světlý/tmavý/systémový motiv, sklo Intelia Veil, přednastavené akcenty nebo vlastní barva (s kontrolou kontrastu), tapety Intelie, sedm přibalených písem OFL, tvary ikon a popisky. |
| **Ovládání** | Gesta na ploše a LENS, gesta PULSE, síla haptiky, diagnostika hardwarových tlačítek a jejich přemapování, když je Intelia v popředí. |
| **Záloha** | Export a obnova všech nastavení jako jeden JSON soubor přes systémový výběr souborů. |
| **Jazyky** | Angličtina a čeština. Na Androidu 13+ lze jazyk zvolit pro aplikaci zvlášť (Studio › Systém a data › Jazyk). |

## Instalace

1. Stáhni APK ze stránky [Releases](https://github.com/ondrejmacko-it/Intelia-Launcher/releases) a ověř jeho SHA-256
   podle poznámek k vydání.
2. Až se Android zeptá, povol instalaci z prohlížeče nebo správce souborů a nainstaluj.
3. Otevři Intelii → Studio → Systém a data → **Výchozí plocha** (volitelné; zpět přepneš kdykoli v nastavení Androidu).

Vyžaduje Android 8.0 (API 26) nebo novější. Otestováno jen na Androidu 16.

### Balíčky

| Balíček | Co to je |
|---|---|
| `dev.intelia.launcher` | Distribuční build. Podepsaný release klíčem projektu (otisk certifikátu SHA-256 `5ddb08b2…dd99d3`, viz [SECURITY.md](SECURITY.md)). |
| `dev.intelia.launcher.lab` | Vývojový build (Lab). Vývojový build, nikdy se nepublikuje. Jeho starý debug klíč byl veřejně v historii Gitu — Lab APK od nikoho neinstaluj. |

Nainstalují se vedle sebe a navzájem se neaktualizují. Nastavení se nesdílí; přenes je Zálohou.

## Oprávnění

| Oprávnění | Proč | Bez něj |
|---|---|---|
| `SET_WALLPAPER` (běžné) | Uložení tapety Intelie jako systémové, jen po potvrzení. | Vše ostatní funguje. |
| `VIBRATE` (běžné) | Hmatová odezva ve zvolené síle. | Použije se systémová haptika. |
| Role výchozí plochy (volitelné) | Intelia jako domovská obrazovka; zkratky aplikací v ARC. | Intelia funguje jako běžná aplikace. |
| Služba přístupnosti *Intelia PULSE* (volitelné) | PULSE a Global ARC nad jinými aplikacemi, přemapování tlačítek. Nečte obsah obrazovky. | PULSE a tlačítka fungují jen na ploše. |
| Přístup k oznámením (volitelné) | Čtení aktivní mediální relace pro hudbu v PULSE. Žádné oznámení se nečte ani neukládá. | Bez hudby v PULSE. |

## Známá omezení

- Beta otestovaná na jediném zařízení (Nothing Phone (3)). Jiní výrobci, velikosti displeje a verze Androidu neověřeny.
- Zatím bez widgetů jiných aplikací, bez icon packů, bez cloudové synchronizace.
- Hardwarová tlačítka a PULSE mimo plochu vyžadují volitelnou službu přístupnosti; výrobci ji mohou omezovat.
- Quickstep (animace Recents a gest) zůstává systémový; launcher třetí strany ho nenahradí.
- Žádná tvrzení o výkonu a baterii, dokud nejsou změřena.

Úplný seznam: [docs/audit/UX_BUGS.md](docs/audit/UX_BUGS.md) a [baseline 0.1.0](docs/audit/BETA_0.1.0_BASELINE.md).

## Plán

Viz [docs/product/MASTER_PLAN.md](docs/product/MASTER_PLAN.md): Studio V5, Design Lab (tapety, hodiny, ikony, widgety),
schopnosti zařízení a Intelia Bridge, ovládání tlačítek, Action Engine / FLOW V2, scény a předvolby, onboarding V2.
Nic z plánu není slíbené, dokud to nevyjde.

## Sestavení

JDK 17+ a Android SDK platforma 37.1 (`local.properties` → `sdk.dir`).

```bash
./gradlew :app:testDebugUnitTest :app:assembleLab
```

`:app:assembleLab` sestaví optimalizovaný Lab (`dev.intelia.launcher.lab`); `scripts/dev.sh` nainstaluje debug build
na telefon přes USB. `:app:assembleRelease` se podepíše jen pokud existuje `.signing/release.properties` (nikdy se
necommituje); viz [DECISIONS D26](docs/DECISIONS.md).

## Hlášení chyb

Založ [issue](https://github.com/ondrejmacko-it/Intelia-Launcher/issues) s modelem telefonu, verzí Androidu, verzí
Intelie (Studio › Systém a data › O aplikaci) a kroky k reprodukci. Nepřikládej screenshoty se soukromými oznámeními
nebo zprávami.

## Přispívání

Nejdřív si přečti [AGENTS.md](AGENTS.md). Kód je pod Apache-2.0; žádný kód zkopírovaný z jiných launcherů.

## Licence a právní informace

- **Kód:** Apache License 2.0 — viz [LICENSE](LICENSE) a [NOTICE](NOTICE). Kód lze používat, upravovat a šířit, ale
  každá kopie musí zachovat licenci, NOTICE a copyright a označit provedené změny. Poskytováno **bez záruky**.
- **Název a logo:** „Intelia“, logo a názvy LENS / ARC / PULSE / FLOW licencované nejsou — viz
  [TRADEMARKS.md](TRADEMARKS.md). Forky musí mít vlastní název a application ID.
- **Oficiální buildy** jsou jen z Releases tohoto repozitáře a podepsané klíčem projektu; ověření viz
  [SECURITY.md](SECURITY.md).
- **Inspirace a komponenty třetích stran:** [CREDITS.md](CREDITS.md). Přibalená písma: SIL Open Font License 1.1.

Copyright 2026 The Intelia Launcher Authors.
