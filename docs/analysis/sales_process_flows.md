# Sales Process Flow and Feature Exploration

## Generating and Managing Proposals

### Overview
The system has the ability to create and track sales opportunities.  These opportunities 
allow for the creation of prospects that are assigned to the opportunities.  When an
opportunity is created, it can use an existing prospect (who may have been assigned to 
previous opportunities-note there may be existing prospects that don't were not assigned 
to any previous opportunities but still exist) or create a new prospect for that opportunity.
A proposal may be created for any prospect in the system.  Multiple proposals can be created
for the same prospect at different points in time.

### Proposal Visibility Improvements
For certain prospects it may be relevant to differentiate proposals.  It would be good to
know which proposal not only belong to the prospect, but to which opportunity they were 
produced.  It would be good to know if any of the past proposals were already applied for.
Such visibility should be in any view that looks at a specific opportunity.

In the activity view of the opportunities.  It would be good to have the list of any/all
proposal created for this opportunity and visibility to whether any of those proposals have
been either applied for, or gone all the way through into a setup.  It would also be good 
to be able to toggle such a view to include all proposals, not just ones tied to that 
opportunity and see the same information.

### Proposal Generation
Currently a user that has an agent role can create a proposal.  Additionally a psp user with
agent role can also create proposals on behalf of a agent from another agency other than
the home psp agency.  This approach is good and functioning.

### New Client Sales

## Most Common / Anticipated Sales Process

It is expected a majority of sales will flow from Opportunity (Prospect) --> Proposal --> 
Application --> Setup --> Completed.  In general the system current system design allows
for this well.

There may be a situation however, where it is unnecessary to require the application be 
completed online.  This can happen if a customer sends a pdf of all the data, and it 
doesn't seem necessary to key all the information into the application, when they are
going to key all the information into Summit already.  If an API in teh future can push
application data into a new employer / benefit creation in Summit, that may change.  This 
essentially leads to the need for the creation of a New Setup.

## The New Setup Feature
This would be a button on the home page (there is already a button to create an opportunity, 
a renewal, and, separately via the log button, a ticket) to create a new setup and  
bypass the application.  So the shortcut button would need to allow the user to enter
some basic items 1) identify or create the prospect (basic info: company name, contact name and email)
, 2) create a proposal identifying the los and enhancements desired and the rate (quick 
view needed here to do all of this, 3) assume those los and enhancements are selected on the
application (we'll use wording on setup 2 view to clarify this), 4) create a new setup
activity based on those selections and import the proper task sequences based on those
selections.