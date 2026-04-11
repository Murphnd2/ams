# Outlook Add-in — Static Assets

This directory contains the public-facing files for the "Log to AMS" Outlook Web Add-in.

| File            | Purpose                                                        |
|-----------------|----------------------------------------------------------------|
| `manifest.xml`  | Office Add-in manifest (sideload URL served from production)   |
| `taskpane.html` | Taskpane UI — auth, activity picker, log action                |
| `icon-16.png`   | 16×16 toolbar icon (navy tile, white "A")                      |
| `icon-32.png`   | 32×32 ribbon icon (navy tile, white "A", green accent stripe)  |
| `icon-80.png`   | 80×80 add-in store icon (same design, larger)                  |

## Icon Files

The three PNG icons are brand-matched tiles:
- Background: `#0d5681` (SSA navy), rounded corners on 32/80
- Glyph: bold white "A"
- Accent: `#87a948` (SSA green) stripe across the bottom (32/80 only)

### Regenerating the icons

To rebuild from scratch (e.g., after a brand color change), run the
PowerShell generator from the repo root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/generate-outlook-icons.ps1
```

The script uses `System.Drawing` — no extra tooling required.

The icons are referenced from the manifest at:

```
https://superiorstate.biz/outlook/icon-16.png
https://superiorstate.biz/outlook/icon-32.png
https://superiorstate.biz/outlook/icon-80.png
```

## Sideloading the Add-in

Once deployed to production:

1. Open Outlook Web (`outlook.cloud.microsoft/mail/`)
2. Click **…** (More actions) → **Get Add-ins** → **My add-ins**
3. Under "Custom add-ins", choose **Add a custom add-in** → **Add from URL**
4. Enter: `https://superiorstate.biz/outlook/manifest.xml`
5. Click any email to see the **Log to AMS** button in the reading pane toolbar

## Auth Setup

Each user must first be linked in AMS via the **Outlook Link Manager**
(`/OutlookLinkManager`, PSP Admin only). Without a link row, the add-in
will show "Account not linked" on first open.
