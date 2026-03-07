# Custom Landing Page — Content Authoring Guide

You are writing HTML content for a custom landing page in the SSA Activity Management System (AMS). The HTML you produce will be injected into a wrapper page that already provides the document structure, header bar, login modal, and core libraries. Your output is **only the inner content** — never a full HTML document.

---

## Technical Constraints

### What the wrapper already provides
The wrapper page (`customLanding25.jsp`) supplies:
- Full `<!DOCTYPE html>`, `<html>`, `<head>`, `<body>` tags
- **Bootstrap 5.3** CSS and JS (`bootstrap.min.css`, `bootstrap.bundle.min.js`)
- **Bootstrap Icons 1.11** (`bootstrap-icons.css`)
- **DM Sans** font (weights 400, 500, 600, 700) via Google Fonts
- A **fixed header bar** at the top (height ~56px) with the PSP's logo on the left and a Login button on the right. The header background color and text/button color are **PSP-configurable** via Settings (defaults: background `#0d5681`, text `#ffffff`). Your content does not control these colors — they are set by the PSP admin.
- A **login modal** triggered by the Login button
- `box-sizing: border-box` on all elements
- `body { font-family: 'DM Sans', sans-serif; margin: 0; padding: 0; }`

### What your HTML content must NOT include
- No `<!DOCTYPE>`, `<html>`, `<head>`, or `<body>` tags
- No `<link>` or `<script>` tags for Bootstrap CSS/JS (already loaded)
- No `<link>` for DM Sans (already loaded)
- No `<script>` blocks of any kind — **all `<script>` tags are stripped by the server's HTML sanitizer on save**. Any JavaScript you include will be silently removed.
- No navigation bar or header — the wrapper provides one
- No login form — the wrapper provides a login modal

### What your HTML content CAN include
- `<style>` blocks (scoped via class names to avoid conflicts with wrapper styles)
- `<link>` tags for **additional** Google Fonts beyond DM Sans (e.g., a serif display font)
- Any Bootstrap 5 classes and Bootstrap Icons (`<i class="bi bi-..."></i>`)
- Standard HTML sections, divs, images, links, forms

### Injection point
Your content is inserted inside:
```html
<div class="landing-body" style="padding-top: 56px;">
    <!-- YOUR CONTENT GOES HERE -->
</div>
```
The `padding-top: 56px` accounts for the fixed header bar. Your first section (typically the hero) sits immediately below the header. If you want a hero that fills the viewport, use `min-height: calc(100vh - 56px)` or `min-height: 90vh`.

---

## Request a Quote Integration

The AMS has a public **Request a Quote** servlet at the relative path `RequestQuote`. This is a standalone form page that collects prospect information and creates an opportunity in the system.

### How to link to it
Use a simple relative anchor link:
```html
<a href="RequestQuote">Request a Quote</a>
```

**Do NOT use** absolute URLs, leading slashes, or JavaScript navigation. The relative path `RequestQuote` works correctly from the landing page context.

### What the quote form collects
- First name, last name (required)
- Preferred contact method: email or phone (whichever selected becomes required)
- Email address, phone number
- Company name (required)
- Number of employees
- Services of interest (checkboxes from the PSP's active Lines of Service)
- Additional info (free text)

### Quote form design
The RequestQuote page has its own complete styling (same header bar style, card-based form, Bootstrap 5). It is a separate page, not a modal or embedded form. Treat it as a navigation destination — when the user clicks "Request a Quote", they leave the landing page and go to the quote form.

### CTA best practice
Include at least two prominent calls-to-action linking to `RequestQuote`:
1. **Hero section** — primary CTA button (e.g., "Get Your Free Quote", "Request a Quote")
2. **Bottom CTA section** — repeat the call-to-action before the footer

Example:
```html
<a href="RequestQuote" class="primary-btn">Request Your Free Quote</a>
```

---

## Design Guidelines

### Brand colors
Use CSS custom properties for consistency. Define them in your `<style>` block:
```css
:root {
    --primary: #0d5681;    /* AMS primary — matches header bar */
    --secondary: #87a948;  /* AMS secondary — olive/green accent */
}
```
The PSP may request their own brand palette. When provided, adapt these defaults. Note: the header bar color is separately configurable by the PSP admin in Settings — your content does not need to match it. Design your content's own color scheme independently.

### Typography
- **Body text:** DM Sans (already loaded). Use for paragraphs, labels, navigation.
- **Display/heading font:** Optionally load one additional serif or display font from Google Fonts for headings (e.g., `Instrument Serif`, `Playfair Display`, `Merriweather`). Keep it to ONE additional font.
- Heading hierarchy: Use `h1` only once (hero headline). Use `h2` for section titles, `h3` for card titles.

### Layout principles
- **Max content width:** 1200px–1400px with `margin: 0 auto` and horizontal padding
- **Section padding:** 4rem–8rem vertical padding between major sections
- **Mobile-first responsive:** Use CSS Grid or Flexbox. Include `@media (max-width: 768px)` breakpoints
- **Card-based design:** Use rounded corners (`border-radius: 12px–16px`), subtle shadows, hover effects

### Recommended sections (in order)
1. **Hero** — headline, subtitle, CTA buttons, optional stats/visual element
2. **Trust bar** (optional) — compliance badges, years in business, key differentiators
3. **Services** — grid of service cards with icons, titles, descriptions
4. **Call to action** — contrasting background section with quote CTA
5. **Footer** — company info, service links, contact details

### Animations
Since `<script>` blocks are stripped, all animations must work via **CSS only** or use the **wrapper's built-in scroll observer**.

**Built-in scroll-triggered fade-in (provided by the wrapper):**
The wrapper page includes an `IntersectionObserver` that watches for elements with class `ss-fade` or `fade-in`. When they scroll into view, it adds `ss-visible` or `visible` respectively. To use this:

1. Define the CSS transition in your `<style>` block:
```css
.ss-fade {
    opacity: 0;
    transform: translateY(24px);
    transition: opacity 0.65s ease, transform 0.65s ease;
}
.ss-fade.ss-visible {
    opacity: 1;
    transform: translateY(0);
}
```
2. Add class `ss-fade` to any element you want to animate on scroll.

The wrapper also provides **smooth scrolling** for all anchor links (`href="#..."`) within the landing content.

**CSS-only entrance animations** using `@keyframes` work for hero sections and elements that should animate on page load (not scroll-triggered):
```css
@keyframes reveal {
    from { opacity: 0; transform: translateY(25px); }
    to { opacity: 1; transform: translateY(0); }
}
.hero-text { animation: reveal 0.9s ease-out both; }
```

- Keep animations subtle — translateY(20–30px) fades, 0.3–0.6s duration
- Do NOT include any `<script>` blocks for animation — they will be stripped

### Icons
Use Bootstrap Icons for service icons and UI elements:
```html
<i class="bi bi-shield-check"></i>
<i class="bi bi-heart-pulse"></i>
<i class="bi bi-building"></i>
```
Alternatively, use HTML entities or emoji for decorative service icons in cards.

---

## Content Guidelines

### Tone
Professional but approachable. The audience is employers and HR managers evaluating benefits administration providers. Lead with value propositions and savings, not technical jargon.

### Required elements
- Company name prominently in the hero or nearby
- At least one `href="RequestQuote"` call-to-action
- Contact information (phone number, address) in the footer
- Copyright notice with current year

### Avoid
- **`<script>` tags** — they are stripped by the server sanitizer and will not execute. Use CSS-only animations and the wrapper's built-in scroll observer instead.
- Stock photo placeholder URLs (use CSS gradients, icons, or patterns instead)
- External JavaScript libraries beyond what the wrapper provides
- Fixed positioning (conflicts with the wrapper's fixed header)
- `z-index` values above 999 (the header bar uses `z-index: 1000`)
- IDs that could conflict with the wrapper: `loginModal`, `landing-header`, `landing-body`

---

## Example Reference

Below is the structure of a working landing page. Use this as a reference for the expected output format and quality level. The HTML fragment starts with an optional font `<link>`, then a `<style>` block, then HTML sections. **No `<script>` block** — the wrapper handles scroll animations and smooth scrolling automatically.

```
<link href="https://fonts.googleapis.com/css2?family=Instrument+Serif..." rel="stylesheet">

<style>
    :root { --navy: #0B2540; --teal: #007C8A; --olive: #6B8F3C; ... }

    /* Hero uses CSS keyframes (no JS needed) */
    .ss-hero { min-height: calc(100vh - 56px); ... }
    .ss-hero-text { animation: ssReveal 0.9s ease-out both; }
    @keyframes ssReveal { from { opacity:0; transform:translateY(25px); } to { opacity:1; transform:translateY(0); } }

    /* Scroll-triggered elements use ss-fade (wrapper adds ss-visible on scroll) */
    .ss-fade { opacity: 0; transform: translateY(24px); transition: opacity 0.65s ease, transform 0.65s ease; }
    .ss-fade.ss-visible { opacity: 1; transform: translateY(0); }

    /* Service cards grid */
    .ss-cards-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 1.5rem; }

    /* Responsive */
    @media (max-width: 768px) { ... }
</style>

<!-- Hero (CSS keyframe animation, no ss-fade needed) -->
<section class="ss-hero">
    <div class="ss-hero-inner">
        <div class="ss-hero-text">
            <h1>Headline here</h1>
            <a href="RequestQuote" class="ss-btn-primary">Request Your Free Quote</a>
        </div>
    </div>
</section>

<!-- These use ss-fade — wrapper's observer makes them visible on scroll -->
<div class="ss-diff-bar ss-fade">...</div>
<section class="ss-services" id="ss-services">
    <div class="ss-section-head ss-fade">...</div>
    <div class="ss-cards-grid">
        <div class="ss-card ss-fade">...</div>
        <div class="ss-card ss-fade">...</div>
    </div>
</section>
<section class="ss-cta">
    <div class="ss-cta-inner ss-fade">
        <a href="RequestQuote" class="ss-btn-white">Get Your Free Quote</a>
    </div>
</section>
<div class="ss-footer">...</div>
```

---

## Output Format

When asked to create a landing page, produce a **single HTML fragment** that can be pasted directly into the AMS custom landing page settings field. The output should be the complete, ready-to-use content — not a partial snippet or wireframe.
