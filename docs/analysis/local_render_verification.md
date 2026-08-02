# Local Render Verification — can Claude Code check a page before Kevin deploys?

**Status:** Proven working, 2026-08-01 — one manual prerequisite outstanding (a local test account)
**Created:** 2026-08-01
**Owner:** Kevin
**Baseline:** branch `refactor/modernize-architecture`, HEAD `26937e8`, local schema brought V073 → **V085**
**Related:** `docs/analysis/project_backlog.md` (T110, T111) · `docs/ichra_strategy.md` · `docs/deployment_runbook.md`

---

## The verdict

**Yes — once Kevin creates one local test account.**

Everything else was verified end to end in this session on a live local instance:

| Step | Result |
|---|---|
| `.\mvnw.cmd -P local clean package` | ✅ WAR built, 63.4 MB, `ssaPU` / `RESOURCE_LOCAL` |
| Local MySQL `beta_ssa` | ✅ reachable, brought to **V085** |
| App deploys and starts | ✅ Tomcat 10.1, 39 s startup, no errors |
| Unauthenticated page fetch | ✅ `GET /ams/Illustration` → `302` → `/ams/login` |
| Public page renders full HTML | ✅ `GET /ams/login` → `200`, 33,776 bytes of real markup |
| Login form target and fields | ✅ `POST AuthenticateUser`, `userName` + `userPassword` |
| Session cookie issued and carried | ✅ standard `JSESSIONID`, works with a curl cookie jar |
| ICHRA gate satisfiable | ✅ `isPspAdmin` short-circuits `IchraAccessResolver.isAvailable` |
| **Authenticated fetch** | ⛔ **not performed — requires a credential, see Prerequisites** |

**Why this matters.** Of the seven consecutive illustration defects that were declared fixed from code
reading and then failed a browser walk, **four were plain markup** — doubled headcount, an error banner on
a hub-card landing, an empty-result panel where a form belonged, three cards pointing at one URL. All four
are visible in an HTML fetch. Catching those locally moves production releases back to being a shipping
step.

**This session already proved the point incidentally**: probing the login endpoint with a deliberately
invalid username surfaced a real, live defect — an unhandled `NullPointerException` returning **HTTP 500
instead of "Invalid username or password"** (`AuthDAO.validUserName`, `AuthDAO.java:46`). That bug is in
production right now and no amount of code reading had found it. Logged as **T110**.

---

## Prerequisites — Kevin does these once, by hand

1. **Create a local test account.** A **PSP Admin (role 5)** account in the local `beta_ssa` database.
   PSP Admin is the cheapest option because `IchraAccessResolver.isAvailable` returns `true` immediately
   for a session carrying `isPspAdmin` — no `agency.ichra_enabled` flag needs flipping. (One PSP Admin
   already exists locally; a dedicated throwaway account is cleaner.) The account must have `is_active = 1`
   — `AuthenticateUser` rejects inactive users after password validation.
2. **Store its credentials outside the repo.** `C:\ssa\ssa.properties` is the established
   pattern and is already git-ignored by location. Never put them in the repo, in a doc, or in a prompt.
3. **Warm one county into `rating_area_rate_cache`, or accept empty-state-only verification.** The cache is
   **empty locally (0 rows)** and no HealthSherpa key is configured on this machine, so the warm path cannot
   run here. Without rates the illustration renders its *configured-but-empty* state, which is still worth
   checking — but no figures, no age bands, no affordability table. See "What is missing locally" below.
4. **Seed the two affordability constants if the threshold table is in scope** —
   `ICHRA_AFFORDABILITY_PCT_2026` and `FPL_ANNUAL_2026`. Both are **absent locally**; `DatabaseInitializer`
   seeds them on fresh installs only, so an existing database like this one never received them.
   Their absence makes the servlet fail closed with a named-constant message instead of a threshold table —
   which is correct behaviour, and is itself a renderable state worth asserting on.

---

## The exact commands

### 0. One-time: build

```bash
cd /c/Users/kevinmurphy.SUPERIORSTATE/IdeaProjects/ams && ./mvnw.cmd -P local clean package -DskipTests
```

Produces `target/ams-1.0.0-SNAPSHOT.war`. The `local` profile reads `C:\ssa\ssa.properties` via
`properties-maven-plugin` and filters `local.db.url` / `local.db.user` / `local.db.password` into
`persistence-local.xml`, which the antrun step copies to `persistence.xml`. **Nothing in the repo carries a
credential.**

### 1. Deploy to an isolated Tomcat instance

Do **not** deploy into `C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps` — that directory
holds a stale `ams` copy from March, the `Tomcat10` Windows service is stopped, and writing there needs
admin rights. Use a private `CATALINA_BASE` instead. It leaves Kevin's IntelliJ run config (port 8082) and
the system Tomcat (port 8080) completely untouched.

```bash
CH="/c/Program Files/Apache Software Foundation/Tomcat 10.1"; CB="$HOME/ams-verify-base"; rm -rf "$CB"; mkdir -p "$CB"/{conf,logs,temp,webapps,work}; cp "$CH"/conf/{server.xml,web.xml,context.xml,tomcat-users.xml,catalina.policy,catalina.properties,logging.properties} "$CB/conf/"; sed -i 's/port="8080"/port="8089"/; s/port="8443"/port="8444"/; s/redirectPort="8443"/redirectPort="8444"/g' "$CB/conf/server.xml"; cp /c/Users/kevinmurphy.SUPERIORSTATE/IdeaProjects/ams/target/ams-1.0.0-SNAPSHOT.war "$CB/webapps/ams.war"; echo "ready: $CB"
```

### 2. Start it

```bash
CH="/c/Program Files/Apache Software Foundation/Tomcat 10.1"; export CATALINA_HOME="C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1"; export CATALINA_BASE=$(cygpath -w "$HOME/ams-verify-base"); export JAVA_HOME="C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.16.8-hotspot"; export CATALINA_OPTS="-Xmx1024m"; "$CH/bin/catalina.bat" start
```

⚠️ **This command does not return in Git Bash** — the shell stays attached. Run it with a background
runner, or expect a timeout and simply move on; the server keeps running. First deploy takes **~40 seconds**
(TLD scanning). Wait for the connector before fetching:

```bash
until netstat -ano | grep -q ":8089 .*LISTENING"; do sleep 2; done; echo "up"
```

### 3. Authenticate and fetch

```bash
cd /tmp && rm -f cj.txt && curl -s -c cj.txt -b cj.txt -d "userName=$AMS_TEST_USER" -d "userPassword=$AMS_TEST_PASS" -o /dev/null -w "login=%{http_code} -> %{redirect_url}\n" "http://localhost:8089/ams/AuthenticateUser" && curl -s -b cj.txt -c cj.txt "http://localhost:8089/ams/Illustration" -o illustration.html -w "page=%{http_code} bytes=%{size_download}\n"
```

**A successful login redirects (302) to `ViewHome25` for a PSP Admin.** A 302 back to `login` means the
credentials were rejected. A **500 means the username does not exist at all** — that is T110, not a typo in
your command.

Read the captured markup with the `Read` tool, or assert on it directly.

### 4. Stop it

`shutdown.bat` does not work against this base (the shutdown port is not wired up in the copied
`server.xml`). Kill the listener:

```bash
PID=$(netstat -ano | grep ":8089 " | grep LISTENING | head -1 | awk '{print $NF}') && taskkill //PID "$PID" //F
```

---

## Paste-into-a-build-prompt block

> **Verify the render before you commit.**
>
> After changing any servlet or JSP on a page you can reach, build and fetch the rendered page rather than
> reasoning about it from source. Full recipe: `docs/analysis/local_render_verification.md`.
>
> 1. `./mvnw.cmd -P local clean package -DskipTests`
> 2. Deploy the WAR to a private `CATALINA_BASE` on port **8089** (recipe §1–2). Never write to
>    `C:\Program Files\...\Tomcat 10.1\webapps`. Wait ~40 s for the connector.
> 3. `POST http://localhost:8089/ams/AuthenticateUser` with `userName` / `userPassword` from the
>    environment, using a curl cookie jar. **Never hard-code, echo, or write a credential.** A 302 to
>    `ViewHome25` is success; a 302 back to `login` is a bad password; a **500 is an unknown username**
>    (defect T110), not your mistake.
> 4. `GET` the page under test with the same cookie jar. Save the HTML and **read it**.
> 5. **Assert on the markup, not on your intent.** State the expected string before you fetch, then check
>    it. Examples that would have caught real shipped defects:
>    - headcount appears **once**, not twice, when a band is left blank
>    - no `alert-danger` / error banner on a normal landing
>    - the input **form** is present, not an empty-result panel
>    - hub-card `href`s are **distinct** from one another
>    - the named-constant message appears when a constant is deliberately absent
> 6. Then stop the server (recipe §4).
>
> **This catches markup only.** CSS-cascade and layout defects are invisible here — see the boundary below.
> If your change is visual, say so plainly and ask for a browser walk instead of claiming verification.

---

## What this catches — and what it does not

### Caught by an HTML fetch

Everything that is a fact about the **markup the server produced**:

- Wrong or duplicated values (the doubled headcount from a blank band)
- An error banner rendered on a path that should not error
- The wrong *panel* — an empty-result state where an input form belongs
- Wrong or duplicated `href`s (three cards pointing at one URL)
- Missing or unexpected sections, wrong conditional branch taken
- A named-constant fail-closed message appearing, or failing to appear
- Server errors, stack traces, HTTP status codes
- **Anything a servlet decided.** If the defect is a `<c:if>` that went the wrong way, it is here.

### NOT caught — still needs a human browser walk

**These are invisible to `curl`, permanently, and no amount of assertion discipline changes it:**

- **CSS cascade.** The `d-flex !important` bug — a hidden template unhidden by a specificity collision — is
  in the markup *as hidden*. The HTML looks correct. Only a rendering engine applies the cascade.
- **Layout and geometry.** Edge-marker clipping at narrow widths. There are no widths in an HTML fetch.
- **Visual ordering.** A slider sitting above the figures it drives is correct in the DOM and wrong on
  screen. Source order and visual order are different things once CSS runs.
- Anything **JavaScript** builds after load — the fetch sees the pre-script document.
- Colour, contrast, spacing, overflow, z-index, responsive behaviour, print styles.
- Whether the page is **usable**, which is not a property of its markup.

**Three of the seven recent illustration defects were in this second list.** The honest framing: this recipe
would have caught **four of seven** — the four that kept getting re-shipped because they looked fine in
source. It is not a substitute for Kevin walking the page, and a run that says "verified" after an HTML
fetch has verified **markup**, not appearance. Say which one you mean.

---

## What is missing locally

| Thing | State | Consequence |
|---|---|---|
| `rating_area_rate_cache` | **0 rows** | The illustration cannot reach a populated result state locally |
| HealthSherpa API key | **not configured** (neither `ssa.properties` nor a `constant` row) | The warm path **cannot run locally at all** |
| `RATE_CACHE_PLAN_YEARS` | **absent** | Servlet forwards to the "not configured" empty state |
| `ICHRA_AFFORDABILITY_PCT_2026`, `FPL_ANNUAL_2026` | **absent** | Affordability fails closed with its named-constant message |
| `county_reference` | ✅ 254 rows (V076) | County lookup works |
| `zip_county` | ✅ 2,894 rows (V084/V085) | ZIP→county crosswalk works |
| `agency.ichra_enabled` | 0 of 9 agencies | Irrelevant if the test account is PSP Admin |

### Getting rates into the local cache — options, and a recommendation

**Not implemented in this run. Kevin decides whether test data gets manufactured.**

| Option | Cost | Risk |
|---|---|---|
| **A. Warm from HealthSherpa staging** | Configure a key locally, seed 3 constants, run one warm cycle | Real rates, real shape, real pagination. But it puts a **credential on the dev workstation** and spends live API calls. Staging data is real, not synthetic |
| **B. Export rows from a non-production source** | A `SELECT`/`INSERT` for one county from wherever the cache is already warm | Faithful data, no credential. **But no installation has ever warmed this cache** — production's is empty too, so there is currently nothing to export from |
| **C. Hand-built local fixture** | ~20 `INSERT` rows for one county, ages 21–64 | No credential, no API, instantly repeatable. Numbers are invented, so it verifies **rendering and arithmetic wiring**, never rate correctness |

**Recommendation: C, a hand-built fixture for Hopkins County (FIPS 48223), plan year 2026.**

The purpose of local render verification is to catch **markup defects** — a doubled headcount, a wrong
panel, a duplicated link. **None of those depend on the rates being real.** A fixture reaches a populated
result state with no credential on the workstation and no API spend, and it is deterministic, which matters
more for assertions than realism does. Option A remains the right choice the day someone needs to verify
rate *correctness* — but that is a different question from render verification, and conflating them is what
would put a live key on a dev box for no reason.

⚠️ **If a fixture is built, it must be obviously fake and never leave the local database.** Use round
numbers that could not be mistaken for quoted rates, and keep it out of the repo — a committed fixture is
one careless import away from being shown to an agent as a real figure.

---

## Environment facts worth not rediscovering

- **Local DB:** MySQL **8.0.30**, `127.0.0.1:3306`, schema **`beta_ssa`**, 231 tables. Credentials live in
  `C:\ssa\ssa.properties` as `local.db.url` / `local.db.user` / `local.db.password` — **outside the repo.**
- **`mysql.exe` / `mysqldump.exe` are not on `PATH`.** Full path:
  `C:\Program Files\MySQL\MySQL Server 8.0\bin\`. Pass the password via the `MYSQL_PWD` environment
  variable so it never appears on a command line or in a file.
- **JDK 17.0.16 (Temurin)** — matches `maven.compiler.release`.
- **Two Tomcats and two ports, neither of which you should use.** The IntelliJ run config
  (`.idea/runConfigurations/Shared_Run_Debug_Config__Tomcat_.xml`) targets **8082**, context `/ams`, with an
  IntelliJ-managed `CATALINA_BASE`. The system Tomcat 10.1 listens on **8080**, its `Tomcat10` service is
  **stopped**, and its `webapps/ams` is stale from March. The private-base approach on **8089** avoids both.
- **No Maven Tomcat/Cargo/Jetty plugin exists** — there is no `mvn tomcat:run` path. A container is required.
- **The WAR is `ams-1.0.0-SNAPSHOT.war`**, not `ROOT.war`. `pom.xml` sets no `finalName`;
  `CLAUDE.md`'s "final WAR named `ROOT.war`" is wrong. The release process renames it on attach.
- **`schema_info` is a view that each migration replaces.** Before this session it was a stub returning
  `1`; applying V074–V085 healed it and it now reports **V085 / 2026-08-01**. `SELECT MAX(version) FROM
  schema_version` is the more reliable local check.
- **`.gitignore` already covers everything this recipe produces** — `/target/`, `**/*.war`, `**/webapps/`.
  Keeping `CATALINA_BASE` outside the repo (`$HOME`, not the working tree) means **no `.gitignore` change is
  needed.** Do not add one.

---

## Notes

- The isolated `CATALINA_BASE` is disposable. Delete and recreate it whenever the WAR changes; hot redeploy
  was not tested and is not worth trusting for verification runs.
- First startup is ~40 s, dominated by TLD scanning. Subsequent starts against an already-exploded webapp
  are faster, but a **clean base is the reliable option** when the point is to check what a fresh deploy does.
- `EmfListener` starts the billing executor and the health scheduler at context init. Both are harmless
  locally, but a local run is a **real** application against a **real** local database — it will write
  `illustration_log` rows and anything else the exercised path writes. Verify against a database you are
  willing to dirty. There is a backup from this session; take another before anything destructive.
