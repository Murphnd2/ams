# Business Process Outsourcing (BPO) - High Level Concept

---
## 1. Types of Installations
### a. Master PSP
This is the installation that manages / tracks all other installations.  It is my current business,
and it will need to be able to control distributions, updates, patches, scripts performed on any
distributed installation.
### b. Standard PSP
These will be the majority of the installations, they will (most) all be third party administrators
(tpas) / plan service providers (psps) that utilize DataPath administrative services, more specifically,
the Summit benefit administration platform.
### c. BPO Provider
These will be select installations of the distributed system that have the ability to be assigned,
view, and act upon delegated Tasks/ToDos.  Currently, the only likely candidate for this install
is Accelergent, a subsidiary of DataPath located in India.  They already perform such services,
but have no streamlined manner to keep track of the various PSPs.  This is a key sales point in my
goal to have this project sold/licensed to DataPath Inc.

---
## 2. Initial Distribution Method and Seeding
### a. Distribution Types
There will only be one distributed system to all entities.  Roles will be used to differentiate
access / features between the various user types. 

### b. Initial Setup / Distribution
There is only expected to be two install types of the application, the Master is already installed
on my production server and it is unlikely another would need to be initialized from scratch.  The Standard PSP and the BPO
Provider.  Any new distribution currently requires a deployment key to be initialized for use.  It
is expected that this could be used to differentiate what seeding is called for a given installation.
- One possible method to differentiate, if the key begins with 'BPO', it will perform the initialization of a BPO Provider,
all other valid keys will create a Standard PSP installation.

## 3. BPO Provider Requirements
The current application has yet to be developed to provide the methods and processes necessary to
function.  It is expected they will have their own landing page after authentication, but that such 
a page can be designed similar in many ways, and use many of the features of the pspHome25.jsp
landing page.  A potential layout:
- Left Column: Same as PSP, a listing of CheckLists that are user specific.
- Right Column: Master listing of all ToDos delegated to that BPO filtered to those assigned
to that BPO User (also able to unfilter as see all open BPO ToDos)
---
**Potential Right Column Layout**:

| ToDo Name | PSP Name | Due Date |
|-----------|----------|----------|
This would then be sortable by any of the 3 columns by clicking on the column header.  Clicking
on the ToDo Name would bring the BPO User to a ToDo detail screen similar to the activityDetail25.jsp
screen used by PSP Users.

## 4. Getting a BPO to be visible to a PSP
A list exists in the taskManager25.jsp page, that shows any/all BPOs available to delegate a task
to.  But how does that list get populated?  There needs to be a mutual agreement that 1) the PSP wishes
for that BPO to be available to them in the list and 2) that BPO is willing to take on that PSP.
- Note: It is assumed that the BPO is authorized to be in the list from the Master PSP's standpoint,
just by sake of creating an install for that BPO.

The likely path for this is to have the PSP Admin have a "Request BPO" functionality.  Perhaps all
that is needed is for the PSP Admin to enter a valid URL of the BPO to initiate the request.  They
enter a valid URL which perhaps checks against some table in the Master PSP's site (a hidden web page),
if valid the "Submit Request" button becomes active.  Upon clicking, it logs a BPO record in that PSP's
BPO table that is in a pending state (so not in their list yet). 

With that perhaps it then takes them to a page hosted on the BPO's database:
https://BestBpoEver.com?requestingUrl=https://BestPspEver.com to log a request in the BPOs table of
"Administered PSPs".  This request then sits with the BPO Admin for approval.  Upon approval, they
post back to the PSP site to authorize the use of the BPO in their ToDos.

## 5. Expanded/Revised Concept of Delegation to BPO
Currently, the system in manageTask25.jsp allows vendor sourcing to be "internal", "sourced",
or "vendor only".  I wish to revisit the current interpretation of these buttons.  Right now "sourced"
would mean that the task has been designated as one that creates a ToDo that could be checked complete 
by either the PSP User or the user of an assigned BPO.  "vendor only" means the ToDo is locked and 
can only be completed by the BPO.

The new version of the 3 button system would differ slightly.  "internal" would remain as is.  "sourced"
would become "source but verify", and "vendor only" would stay basically the same.

What that means is that if a Task is setup to be delegated in either way (just not "internal"), it 
means INITIALLY that any ToDo generated from that Task is locked and the PSP User can't check it completed.  However, if the
Task was delegated as "source but verify", the act of the BPO User marking the ToDo "completed" wouldn't
close the ToDo, it would unlock the ToDo and require the PSP User to close it by checking it off in
their view of the ToDo list (closing the ToDo).  The PSP User would also need the ability to "revert"
the ToDo back to the BPO in some cases.

This will require some logic changes to what the flags mean and do in the manageTask25.jsp.

## 6. Data Structure Changes Anticipated
The Task object is likely sound as it is currently structured.  The ToDo list will likely need a few
updates / expanded fields.
- BpoCompleted: This will be true after a Bpo User has "checked" of the ToDo
- Unique GUID: Every todo is created with one.  It is the id used to sync between the PSP installation
and the BPO installation.
- NotesList: A one to many relationship to the existing Notes object.  This would allow communication between 
the PSP and the BPO in a log.  For the BPO User this log would be similar to the note/history section 
of activityView25.jsp.  I think this note data is held on the BPO database, but the PSP site will
have a link embedded in the ToDo list area, and for a delegated task would use the BPO's url, the
ToDo's unique GUID, to go to a page on the BPO site that will show the note history for that ToDo, and
to be able to add to it.  Ideally, this listing of notes would be able to visually differenitate
between PSP entries and BPO entries. So some created by identification on the note would provide that.

## 7. Syncing ToDos between PSP and BPO systems.
There has to be some mechanism created between the two sites to check status, for the BPO system
to know when new ToDos exist to import, for the PSP system to know when a BPO user has flagged one
of their delegated ToDos as completed, for the BPO system to know when a PSP user has reverted a 
completed ToDo back to the BPO.  I have little insight on this technical process, but would image
the list of approved BPO's and their URL's on the PSP site, and the list of approved PSP's and their
URL's on the BPO's site would know how to talk to each other.

## 8. Long Term Vision Questions
- Can entire Activity objects (Renewal, Setup, Ticket, Opportunity) be delegated to a BPO?
- Should sales features be provided to the BPO?  They create their custom LOS and pricing strategy
and then create appropriate application modules to flow into a Setup for them?
- Should we put in Ticket functionality for the BPO view of the system right away?

