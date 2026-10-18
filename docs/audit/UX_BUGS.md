# UX bugs and gaps — 0.1.0-beta.1

Reproducible issues found up to 2026-10-19. Severity: **S1** blocks use · **S2** wrong / confusing · **S3** polish.
Not fixed in the beta unless marked. Older verified notes live in `docs/STATUS.md`.

| # | Sev | Where | Repro | Expected | Actual |
|---|---|---|---|---|---|
| U1 | S2 | Studio › Systém a data › Služba PULSE; Studio main „PULSE …“ | `adb install -r` the Lab, open Studio › Systém a data while `dumpsys accessibility` lists Intelia PULSE as bound | „Zapnuto“ | „Vypnuto: PULSE a tlačítka fungují jen na ploše“, main row „PULSE Jen na ploše“. May be the `uiautomator dump` rebind race (known) — re-check without a dump before fixing |
| U2 | S3 | Studio › Systém a data | Open the page | Group titles differ from the page title | Page „SYSTÉM A DATA“ contains a group „DATA A SYSTÉM“ |
| U3 | S3 | Studio search | Search „jazyk“ / „language“ | The new Language row is found | Row is not in `StudioIndex` yet |
| U4 | S3 | Dock | Put 5+ apps in the LENS row on a narrow screen / 150 % text | Compact or two-row dock | Targets shrink towards 48 dp (STATUS S4.4) |
| U5 | S2 | Haptics | Phone without amplitude control | Slider explained | Only four stops shown; continuous slider untested anywhere (STATUS D24) |
| U6 | S2 | PULSE / Global ARC | OEM kills the accessibility service | Clear state + re-enable path | Not tested on other OEMs |
| U7 | S3 | Studio sheet | Transitions | Measured smooth | No `gfxinfo` run since M0 |

Not verified at all (no evidence either way): TalkBack speech, Reduced Motion path, light theme on device, English UI
on device, font scale > 150 %, tablets / foldables, Android < 16.
