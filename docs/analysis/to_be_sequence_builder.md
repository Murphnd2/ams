# Sequence Builder

---

## Purpose Statement

The sequence builder exists to allow users / PSPs to develop standardize lists of items 
that are required to be performed when ever such an "object" is implemented/activated.

This is achieved by creating multiple tasks in the desired order and allocating them
to a tasksequence.  There are two types of tasksequences utilized in this project, the
recurringtasklist - which is operating as desired currently, and the requiredchecklist 
which is the focus of this particular project.

## Intended Outcome

The goal of this project is to make the requiredchecklist feature able to scale easily 
to other improvements / enhancements expected in the overall deployment.  As it is 
delivered currently, it functions for the purpose of my specific business and how it 
is currently running, but has been patched together with various table relations that
aren't necessarily the most clear, logical or expandable.  The goal should be to be able 
to migrate to a more unified approach to these processes, as they are key to delivering 
value to the other PSPs that will be the audience we are trying to sell to.

## Current State

### Renewals
When this platform was developed, the focus was on dealing with the data/items that 
exist in the DataPath summit system (what i currently identify/call benefits), where 
in general, for any given benefit type, there may be a list of required tasks that 
should be put in place when any renewal is created that involves such benefits.  These 
various items i would get from an export in the DataPath Summit system, and populate 
the plantype table.

### Setups
Then there is the Setup, which is basically the implementation of new services for an 
existing or new client.  This process is generally outside of DataPath, so structurally 
I created separate tables and names for things we sold.  Here there is no import, in
this case I decided to manually create types of items i would sell and when they were 
flagged to be associated with the Setup it would call the appropriate task template 
tied to that item, so it was a more manual identification.  The templatepurpose table 
was created/utilized to accomplish this.

### Tickets
Tickets were the last to be developed, but it made sense that if there was a Ticket
activity created, and it was a very common type of ticket request, it would be good 
and wise to make it easy to respond with the right answer/process every time that type 
of issue presented itself.  However, tickets vary wildly in what they require, so
tickets themselves can be designated to a pre-defined ticket category/subcategory, or
they can just have a free text entry to describe the situation, making the process 
flexible to accomodate any situation.  I then tied the ticket subcategory back to
the plantemplate table, i believe, because then the plan template was tied back to the
required sequences.  

---

### Future State
After working on this project, certain high level concepts have emerged, and the project
isn't that far away, i don't believe, from being scalable.  Some general thoughts.

This is intended to allow a business (psps mostly) to serve its customers (sales
prospects, clients, employees of clients, agencies, agents of agencies, bpos, etc...)

**Serving** a customer can mean a number of things, but currently all acts of serving 
a customer fall into an Activity object.  The Opportunity tracks necessary work for
prospective clients, the Setup tracks necessary work for new clients (or new services
to existing clients), the Renewal tracks necessary work for existing services that 
must be repeated at some regularity (currently the system assumes only annually, but 
that should be something that can be set), the Ticket tracks necessary work to resolve
an issue encountered by any of the customers defined previously.  If there is a new
type of work that becomes identified, it would in theory just become a different 
Activity type in the system.

Activities are completable.  They are unique to that customer and cover a defined set
of items.  These items come in different names, but generally, for Opportunities the 
item(s) would be the sale, and the sale might have various sale_types that include 
proposals, that drive
what should be done for that opportunity.  If it contains these sale types --> do this... 

For Setups, the items would be the LOS and Enhancements applied for by the prospect.
These are separate tables, and for pricing purposes are combined in the servicemodule 
table, but the reality is, each los and each enhancement identified in a Setup 
could/would drive unique tasks needing to be completed.  

For Tickets, the items would be "issues", issues are categorized to help for analysis, 
but the whole point of the activity is to resolve the issue.  Issues that recur, would 
likely have a set list of tasks that require completion, the tasksequence is the answer.
The ticketCategory and ticketSubCategory was established to help consolidate issue 
labeling, but then were tied back to the templatepurpose so that they could then
generate requiredtasklists.

For Renewals, the items would be the "benefits", but this may be too specific a term.  It was done, because all we
did was look at what benefits existed in Summit, and knew that for any given benefit
type (plantype table), there would be likely a required list of tasks for them.

This is where some scalability items come into play.  I think it would be good to 
allow the system to be more capable than just handling renewing benefits from the 
Summit table, and it applies more broadly than just Renewal activity types.

If we think of these items more generally as "ServiceItems" (note I have a 
serviceitem table currently, but it is not for this purpose), they all are covered 
by that umbrella term.  A sales opportunity that is tracking a certain "sale type", 
could be called a service item.  A setup that is tracking the implementation of a 
new los or enhancement would be tracking "service items", renewal that are tracking
the tasks associated with certain benefits are "service items", tickets that are 
tracking the work for certain issues are "service items".

So for any given PSP, the types of Opportunity sale_types might vary, the types of 
Lines of Service or Enhancements they offer may vary, what benefits are managed at
Renewal will be mainly tied to the benefits of the DataPath summit system, but in
reality could be more than just this, perhaps they also sell services that aren't
supported by Datapath, and have a second or third SaaS type solution that covers
things DataPath doesn't.  It might be nice to have a table that allows custom 
benefits to be setup and then maybe a link to "provider data" to be able to link or
capture that data, but not dedicate the benefit structure to just Datapath items.

Even right now with DataPath data, i have one benefit table, that deals with two
DataPath benefit tables, the CDH and the PremiumBilling.  Because ids can overlap
i negate the unique id on the premium billing table to consolidate them in one table
anyway.

So, from a simplified "To Be" standpoint.  I can perceive that the system manage
"Activities" for a "Customer" that contain certain "Service Items".  "Service Items"
then will have a required list of tasks to do when they are attached to the activity.

Conceptually, the requiredtasklist would be a one for one relation for every service item
that exists.  Conceptually tickets are the only one that is a little outside this, where
probably only ticket service items that have been flagged by the psp as recurring
would likely have these lists, but every other one, i believe probably should.  As such
we'd never suppress a service item unless it was no longer something they ever do.  For 
instance a LOS in no longer sold.

How do we migrate and build a solution that looks at servicing customers under this
heirarchy?

