# Runbook — Rotating the `ams_app` MySQL Password

The `ams_app` MySQL user is the write account the Tomcat datasource authenticates as. Rotating its password
touches **three** places that must all hold the **identical** value, or Tomcat fails to start with
`Access denied for user 'ams_app'`.

## ⚠️ The #1 gotcha: `ams_app` has TWO account rows

`ams_app` exists as two separate MySQL accounts that happen to share a name:

| Account row              | Used by                                                        |
|--------------------------|-----------------------------------------------------------------|
| `'ams_app'@'localhost'`  | Socket CLI connections (`--socket=/var/run/mysqld/mysqld.sock`) |
| `'ams_app'@'127.0.0.1'`  | The Tomcat JNDI datasource (connects over TCP to 127.0.0.1)    |

`ALTER USER` on one row does NOT touch the other. **Always rotate both together.** The classic failure mode:
you rotate `@localhost`, your manual socket test (`mysql -u ams_app -p`) passes, but Tomcat — which connects
via TCP as `@127.0.0.1` — still has the old password and fails. Confirm both rows exist first:

```
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u root -p \
  -e "SELECT user, host FROM mysql.user WHERE user='ams_app';"
```

## ⚠️ The #2 gotcha: password character set

Use **letters and digits only**. Avoid every one of these — each breaks a different layer:

- `!` — bash history expansion inside double-quoted `-e "..."` commands (statement silently mangles/fails)
- `@ + / =` — quoting/parsing ambiguity across MySQL and copy/paste
- `" & < >` — break the XML attribute in `context.xml` (`password="..."`) unless entity-escaped
- `# $ \` and spaces — properties-file and shell hazards

A plain alphanumeric password is safe in all three targets (MySQL, XML attribute, properties file) with zero
escaping.

## The three places that must match

1. **MySQL** — both account rows (above).
2. **`/etc/tomcat10/context.xml`** — the JNDI `<Resource ... password="..."/>` (the datasource; this is what
   gates Tomcat startup). Value goes **inside the double quotes**.
3. **`/etc/tomcat10/ssa.properties`** — line `DB_PASSWORD=` (the app's own DB calls). Value goes **raw,
   unquoted**, right after the `=`. (`DB_USER=ams_app` stays unchanged.)

Note: Tomcat reads `/etc/tomcat10/context.xml` directly — there is no overriding `META-INF/context.xml` in the
WAR and no `/etc/tomcat10/Catalina/localhost/ROOT.xml`. Verified 2026-07-17.

## Procedure

Pick a memorable **alphanumeric-only** password. Below it is written `<PW>`.

**1. Set both MySQL rows (one statement so they can't drift):**
```
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u root -p \
  -e "ALTER USER 'ams_app'@'localhost' IDENTIFIED BY '<PW>'; ALTER USER 'ams_app'@'127.0.0.1' IDENTIFIED BY '<PW>'; FLUSH PRIVILEGES;"
```
(Prompts for the **root** MySQL password, not ams_app's.)

**2. Edit `/etc/tomcat10/context.xml`** — set the datasource `password="<PW>"` (keep the quotes).

**3. Edit `/etc/tomcat10/ssa.properties`** — set `DB_PASSWORD=<PW>` (no quotes). Leave `DB_USER=ams_app`.

**4. Verify both files hold the identical value BEFORE restarting** (`cat -A` reveals trailing spaces / stray
chars; `$` marks the true line end):
```
sudo sed -n '/password=/p' /etc/tomcat10/context.xml | cat -A
sudo sed -n '/^DB_/p' /etc/tomcat10/ssa.properties | cat -A
```
Confirm the value is byte-identical between the two and that `DB_PASSWORD` is not blank.

**5. Restart and verify:**
```
sudo systemctl restart tomcat10
sleep 20
sudo tail -n 30 /var/log/tomcat10/catalina.out | grep -iE 'access denied|Global data loaded'
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/
```
Success = **no** `access denied`, a `✅ Global data loaded` line, and `curl` returns `200`.

**6. Independent credential test (optional, isolates DB side from config side).** Tests the TCP path Tomcat
actually uses:
```
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql -h 127.0.0.1 -P 3306 -u ams_app -p'<PW>' \
  beta_ssa -e "SELECT CURRENT_USER();"
```
Should print `ams_app@127.0.0.1`. (Single-quote the `-p'<PW>'` so the shell leaves it alone.)

## Post-rotation cleanup

- Delete any stale backup files that contain the OLD plaintext password, e.g.
  `sudo rm /etc/tomcat10/ssa.properties.bak.*`. Check first:
  `ls -la /etc/tomcat10/ssa.properties*`. Note these `.bak` files are often world-readable (`-rw-r--r--`),
  worse than the live file's `-rw-r-----`.
- If the new password was ever displayed on screen (shared terminal, screenshot, pasted into a chat), treat it
  as a throwaway and rotate once more to an un-shown value using this same procedure.

## Related

- Never store the password in git, migration scripts, or any tracked file. It lives only in
  `/etc/tomcat10/context.xml` and `/etc/tomcat10/ssa.properties` (both outside the repo, root-owned).
