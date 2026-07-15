# SWBD / PremiumPath

**Type:** Agency / GA demand · **Status:** Live — SSA's **first white-label agency**.

## Summary

Southwestern Benefit Designers (SWBD), operating the **"PremiumPath"** brand — a general agency (GA)
whose needs have driven SSA's white-label and multi-agency-hierarchy build-out. **`premiumpath.net`
is live** (branded portal + agency-domain email, set up on the **apex/root** — see
`docs/runbooks/agency_white_label_domain_onboarding.md`).

**Positioning:** SSA is the white-label ICHRA/QSEHRA engine for SWBD's contracted agent network;
SWBD's agents add their own markup on proposals.

## People

- **Forrest Huggins** — SWBD owner.
- **Annette Bechtold** (Forte Consulting, Atlanta) — made the introduction.
- **Daniel Cruz / Presidio Healthcare** — possible involvement (to research / confirm).

## Status & history

- Discovery meeting **2026-07-13** with Forrest Huggins; key ask: **producing-agent count** (sizes the opportunity).
- First meeting positive; follow-up call scheduled.
- `premiumpath.net` onboarded as the **first live white-label agency domain** (apex/root pattern).

## AMS work this drives

- **White-label portal + email** — V068 host landing, V069 per-agency email (`EmailIdentityResolver`), white-label proposal/application wrapper.
- **Agent markup** — V066 (`proposal_price_adjustment`) + V067 (per-agency enable).
- **GA → sub-agency hierarchy** — V070 (parent link) + V071 (quote tokens).
- **Open decision #39** — GA→sub-agency rate-assignment model (live constraint check vs. copy-at-creation) exists *because* SWBD needs a bounded sub-agency rate model.
- **Onboarding runbooks** — `docs/runbooks/agency_white_label_domain_onboarding.md` and the two agency-facing one-pagers were built from the premiumpath onboarding.

## Open questions / follow-ups

- Presidio Healthcare research (a Fable prompt was prepared).
- List-bill compliance question.
- Ranked meeting scenarios.
- Producing-agent count — the discovery-meeting ask; needed to size the opportunity.
