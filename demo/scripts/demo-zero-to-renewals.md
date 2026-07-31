# Demo: Zero to Renewals

## Overview
Show a prospect or conference audience: "Starting from a clean system, here's how fast
you can import your existing Summit data and have renewals ready to manage."

## Target Audience
Conference attendees, existing Summit customers evaluating AMS migration

## Domains
- demo.superiorstate.biz (PSP admin perspective)

## Prerequisites
- Admin must be logged in to access /ReSeedDb
- DEPLOYMENT_KEY must be known (from ssa.properties on demo server)
- Summit demo CSVs ready: demo/summit-import/J1-J7 files
- Browser tab open to demo.superiorstate.biz

## Credentials
- **PSP Admin:** (set during initial setup — verify before recording)
  - Planned: jmartinez@superiorstate.net / demo123
- **DEPLOYMENT_KEY:** (from /var/lib/tomcat10/conf/ssa.properties on demo server)

---

## Scene 1: "Clean Slate" (~15 sec)

**Goal:** Show the system starting fresh — no data, no activities.

### Steps:
1. Navigate to `https://demo.superiorstate.biz/ReSeedDb`
   - If not logged in, login first as PSP Admin
2. Enter the DEPLOYMENT_KEY in the confirmation field
3. Click "Reset Database"
4. Wait for redirect to ViewHome25
5. **Pause on the empty dashboard** — no activities, no to-dos, no time entries
6. Hover over the Admin dropdown in navbar — show the available tools

### Narration Notes:
> "Here's a brand-new AMS instance. No data, no configuration — just a clean canvas.
> Everything you need to get started is right here in the admin menu."

---

## Scene 2: "Import Your Summit Data" (~30 sec)

**Goal:** Show how easy it is to bring existing Summit data into AMS.

### Steps:
1. Click Admin dropdown → "Import Data" (or navigate to `https://demo.superiorstate.biz/SummitImport`)
2. **Step 1 - Upload:**
   - Upload `J1_Employers.csv` to the Employer field
   - Upload `J2_Employees.csv` to the Employee J2 field
   - Upload `J3_EmployeeStatus.csv` to the Employee J3 field
   - Upload `J4_Benefits_CDH.csv` to the Benefits CDH field
   - Upload `J7_Benefits_COBRA.csv` to the Benefits COBRA field
   - Upload `J5_BenefitPlanYears.csv` to the Plan Years field
   - Click "Upload" / "Next"
3. **Step 2 - Configure:**
   - Show the plan types detected from the import
   - Configure renewal months for each plan type (e.g., COBRA = January, FSA = July)
   - Click "Import" / "Next"
4. **Step 4 - Results:**
   - Pause on the summary: 2 employers, 9 employees, benefits loaded
   - Highlight: "That's your entire Summit book of business, imported in seconds."

### Narration Notes:
> "If you're already on Summit, migration is painless. Upload your J-series export files,
> configure your renewal calendar, and click import. Your employers, employees, and benefits
> are all here — ready to work with."

### File Paths (for upload_file tool):
- `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\demo\summit-import\J1_Employers.csv`
- `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\demo\summit-import\J2_Employees.csv`
- `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\demo\summit-import\J3_EmployeeStatus.csv`
- `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\demo\summit-import\J4_Benefits_CDH.csv`
- `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\demo\summit-import\J5_BenefitPlanYears.csv`
- `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\demo\summit-import\J7_Benefits_COBRA.csv`

---

## Scene 3: "Configure Renewal Sequences" (~20 sec)

**Goal:** Show how to set up standardized renewal workflows for different plan types.

### Steps:
1. Navigate to Admin dropdown → "Sequences: How We Do Stuff"
   (or `https://demo.superiorstate.biz/SequenceBuilder25`)
2. Show the Sequence Builder page — currently empty (clean system)
3. **Create a COBRA Renewal Sequence:**
   - Click "New Sequence" (or equivalent button)
   - Name: "COBRA Annual Renewal"
   - Type: Renewal
   - Add tasks (drag-and-drop or form):
     1. "Send renewal notice to employer" (due: -60 days)
     2. "Collect updated census data" (due: -45 days)
     3. "Submit rates to carrier" (due: -30 days)
     4. "Confirm rates with employer" (due: -14 days)
     5. "Activate renewed coverage" (due: 0 days)
   - Save the sequence
4. **Create a CDH/FSA Renewal Sequence:**
   - Click "New Sequence"
   - Name: "FSA Plan Year Renewal"
   - Type: Renewal
   - Add tasks:
     1. "Review plan document updates" (due: -45 days)
     2. "Send open enrollment materials" (due: -30 days)
     3. "Process enrollment changes" (due: -14 days)
     4. "Verify deduction schedules" (due: -7 days)
   - Save the sequence

### Narration Notes:
> "Now let's define how renewals should be handled. The Sequence Builder lets you create
> standardized task workflows. Here's a COBRA renewal sequence with five steps, each with
> a deadline relative to the renewal date. We'll do the same for FSA."

---

## Scene 4: "Renewals Ready to Go" (~20 sec)

**Goal:** Show renewals appearing with imported data and sequences attached.

### Steps:
1. Navigate to PSP Home (`https://demo.superiorstate.biz/ViewHome25`)
   - **NOTE:** Renewals may need to be generated from the imported data.
     Check if there's a "Generate Renewals" action or if they appear automatically
     after import + sequence configuration.
2. If renewals need to be created manually:
   - Click "Add Activity" in navbar → select "Renewal" tab
   - Select employer: "Meridian Group Benefits"
   - Select plan type: "COBRA"
   - Set renewal date
   - The COBRA Renewal Sequence should auto-attach
3. Click into the renewal to show the Activity Detail page:
   - **Left column:** Checklist with the 5-task COBRA sequence, all with due dates
   - **Center column:** Employer info populated from Summit import (Patricia Walsh contact)
   - **Right column:** Empty history (brand new)
4. Navigate to "Upcoming Renewals" view (navbar button or admin menu)
   - Show the renewal timeline with both employers' renewals scheduled

### Narration Notes:
> "Back on the home page, our renewals are ready. Each one has the sequence we just built
> automatically attached as a checklist. Contact information from Summit is already here.
> The Upcoming Renewals view gives you a calendar of everything on deck."

---

## Scene 5: "Day-to-Day Management" (~15 sec)

**Goal:** Show how daily work flows through the system.

### Steps:
1. From the renewal activity detail page:
   - Check off the first to-do: "Send renewal notice to employer"
   - The task moves to completed state
2. Add a note to the renewal:
   - Click "Add Note" or the notes area
   - Type: "Sent renewal packet via email to Patricia Walsh"
   - Save
3. Show the right column updating with history entry
4. Quick glance at the PSP Home — the to-do count updates
5. (Optional) Show the time clock starting/stopping for the task

### Narration Notes:
> "Working through renewals is intuitive. Check off tasks as you go, add notes for your records,
> and the full history is captured automatically. Your team always knows what's been done
> and what's next."

---

## Recording Instructions

### Using GIF Creator (short clips per scene):
```
For each scene:
1. gif_creator → start_recording
2. computer → screenshot (initial frame)
3. Execute the scene steps
4. computer → screenshot (final frame)
5. gif_creator → stop_recording
6. gif_creator → export (download: true, filename: "scene-N-name.gif")
7. gif_creator → clear
```

### Using OBS (full continuous recording):
1. Start OBS recording before Scene 1
2. Claude drives the browser through all 5 scenes
3. Stop OBS recording after Scene 5
4. Post-edit: add title cards between scenes, add voiceover

---

## Open Questions (verify before recording)
1. What are the actual PSP Admin credentials on demo.superiorstate.biz?
2. What is the DEPLOYMENT_KEY for the demo server?
3. After Summit import, do renewals auto-generate or must they be created manually?
4. Does the Sequence Builder have a "New Sequence" button on a clean system, or do
   we need to use a different entry point?
5. Can sequences be auto-attached to renewals by plan type, or is it manual?
