package net.superiorstate.ams.data.template;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.sql.Date;
import java.util.List;

public abstract class Q {
    public static final String KEYMAN_OFFICER_COMP = "185,000";
    public static final String KEYMAN_1_PER_OWNER_COMP = "150,000";

    public static final String DOC_PATH = "https://superiorstate.net/tpo/docs/";
    public static final String ONLINE_APPLICATION_DATA = "https://docs.google.com/spreadsheets/d/1NCyRHecjEvUu_ctBRVmegP5vp96ve2HQt4OeytFrYxo/edit#gid=1661726138&range=A6";
    public static final String DOC_SUMMIT_ER_CDH_GUIDE = DOC_PATH + "summit/erCDH.pdf";
    public static final String DOC_SUMMIT_ER_COBRA_GUIDE = DOC_PATH + "summit/erCobra.pdf";
    public static final String DOC_SUMMIT_QUICK_COBRA = "";
    public static final String DOC_SUMMIT_QUICK_CDH="";
    public static final String DOC_SRA = DOC_PATH + "formSRA.pdf";
    public static final String DOC_CLAIM_FORM = DOC_PATH + "formClaim.pdf";
    public static final String DOC_ELIGIBLE_EXP = DOC_PATH + "formEligible.pdf";
    public static final String DOC_BANK_FEE = DOC_PATH + "formFeeEFT.pdf";
    public static final String DOC_BANK_CDH = DOC_PATH + "appEREFT.pdf";
    public static final String DOC_EE_DIR_DEP = DOC_PATH + "formEEEFT.pdf";
    public static final String DOC_INFO_PAYMENT = DOC_PATH + "infoPayment.pdf";
    public static final String DOC_INFO_FAQ = DOC_PATH + "infoFAQ.pdf";
    public static final String JOT = "https://form.jotform.com/";
    public static final String FORM_COBRA_TAKEOVER = JOT + "212594164491055";
    public static final String FORM_EE_DIRECT_DEP = JOT + "212714102254038";
    public static final String FORM_BANK_CDH = JOT + "212714328469056";
    public static final String SUMMIT_INTRO_COBRA = "" +
            "<p><b>Announcing Our Benefit Portal Upgrade</b></p>" +
            "<p>Superior State is excited to announce that we are upgrading our benefits portal to Summit. " +
            "Summit is a comprehensive, cloud-based benefits administration platform that will enable us " +
            "to serve you more efficiently than ever before.</p>" +
            "<p><b>HOW TO ACCESS</b></p>" +
            "<p>Summit differs from the myRSC portal in how it is accessed.  Whereas the myRSC site is a single " +
            "site for employee participant users and employer HR users that uses ROLES to differentiate what can " +
            "be accessed, Summit has a portal for HR users and a separate portal for employee users.  They are:" +
            "<ul><li>HR User Site: <a href=\"https://superiorstate.summitwith.us\" target=\"_blank\">https://superiorstate.summitwith.us</a></li>" +
            "<li>Employee Site: <a href=\"https://superiorstate.summitfor.me\" target=\"_blank\">https://superiorstate.summitfor.me</a></li></ul></p>";

    public static String summitLogin(String username, String password){
        return "<p>So let's start by getting your access accomplished.  To access the HR site: <ul>" +
                "<li>Go to <a href=\"https://superiorstate.summitwith.us\" target=\"_blank\">https://superiorstate.summitwith.us</a></li>" +
                "<li>Username: " + username +"</li>" +
                "<li>Password: " + password +"</li></ul>" +
                "</p>";
    }

    public static final String SUMMIT_OUTRO_CDH = "" +
            "<p>When you first access the site, it is recommended you update your security information, including changing your password," +
            " and updating your security questions.  To do this, we will want to edit your profile which can be accessed by clicking on the" +
            " head-looking icon to the right of your name near the top center of your screen.  A pop up screen to EDIT PROFILE will present" +
            " itself where you can EDIT PASSWORD and update SECURITY QUESTIONS.</p>" +
            "<p><b>NEXT STEPS</b></p>" +
            "<p>We think the next best step is to set up a web training.  We will cover the important aspects of the website to make sure " +
            "you are familiar, to include: <ul>" +
            "<li>Reviewing and/or updating your benefit offering</li>" +
            "<li>Reviewing and/or updating your demographic information</li>" +
            "<li>Adding new hires</li>" +
            "<li>Updating employee demographics</li>" +
            "<li>Reviewing participant status</li>" +
            "<li>Running reports</li></ul>" +
            "A detailed user guide has been created, but it is recommended that we go through a brief web training. " +
            "Please let me know what times might work for you in the upcoming weeks and we will get you more familiar.</p>" +
            "<p>The detailed user guide can be found at <a href=\"https://superiorstate.net/documents/SummitCDH.pdf\" target=\"_blank\">" +
            "https://superiorstate.net/documents/SummitCDH.pdf</a></p>";
    public static final String SUMMIT_OUTRO_COBRA = "" +
            "<p>When you first access the site, it is recommended you update your security information, including changing your password," +
            " and updating your security questions.  To do this, we will want to edit your profile which can be accessed by clicking on the" +
            " head-looking icon to the right of your name near the top center of your screen.  A pop up screen to EDIT PROFILE will present" +
            " itself where you can EDIT PASSWORD and update SECURITY QUESTIONS.</p>" +
            "<p><b>NEXT STEPS</b></p>" +
            "<p>We think the next best step is to set up a web training.  We will cover the important aspects of the website to make sure " +
            "you are familiar, to include: <ul>" +
            "<li>Reviewing and/or updating your benefit offering</li>" +
            "<li>Reviewing and/or updating your demographic information</li>" +
            "<li>Adding new hires</li>" +
            "<li>Updating employee demographics</li>" +
            "<li>Adding / modifying employee coverages</li>" +
            "<li>Logging qualifying events</li>" +
            "<li>Running reports</li></ul>" +
            "A detailed user guide has been created, but will likely be confusing until we’ve gone through the web training. " +
            "Please let me know what times might work for you in the upcoming weeks and we will get you more familiar.</p>" +
            "<p>The detailed user guide can be found at <a href=\"https://superiorstate.net/documents/SummitCOBRA.pdf\" target=\"_blank\">" +
            "https://superiorstate.net/documents/SummitCOBRA.pdf</a></p>";

    public static String getSummitTransition(String uname, String pword, int type){
        if(type==1)
            return SUMMIT_INTRO_COBRA + summitLogin(uname,pword) + SUMMIT_OUTRO_CDH;
        return SUMMIT_INTRO_COBRA + summitLogin(uname,pword) + SUMMIT_OUTRO_COBRA;
    }

    public static final String FSA_ADVERT_2023 = "" +
            "<p>Your POP plan renewal is a perfect time to consider expanding your 125 plan to include" +
            " <b><span style=\"color:red\">(F)lexible (S)pending (A)ccounts.</span></b></p>" +
            "<p><span style=\"background:black; color:white\"><b>&nbsp;Why FSAs?&nbsp;</b></span></p>" +
            "<p><b>FSAs save your employees between $0.25 - $0.35 on every dollar they spend on their family’s healthcare.</b></p>" +
            "<p>Do you have employees:<ul>" +
            "<li>With no <span style=\"color:blue\">Dental Insurance</span>?</li>" +
            "<li>With kids in <span style=\"color:blue\">Daycare</span>?</li>" +
            "<li>With kids needing <span style=\"color:blue\">Braces</span>?</li>" +
            "<li>Requiring routing medication (ex: <span style=\"color:blue\">Diabetic</span>)?</li>" +
            "<li>Considering <span style=\"color:blue\">Lasik</span> surgery?</li></ul>" +
            "<p>The addition of an <b><i>FSA</i></b> plan <b><i>will save</i></b> such an employee anywhere between <b><i>$500 - $2,000</i></b>.</p>" +
            "<p>For a more comprehensive list of the types of expenses reimbursable under an FSA program, click this " +
            "<a href=\"https://superiorstate.net/tpo/docs/formEligible.pdf\" target=\"_blank\">LINK</a>.</p>" +
            "<p><span style=\"background:black; color:white\"><b>&nbsp;FSAs Pay for Themselves&nbsp;</b></span></p>" +
            "<p>FSAs also <b>save you, the employer</b> sponsor, <b>by reducing</b> the total <b>matching payroll taxes</b> you pay the government." +
            "  A common example, an employee with a child in daycare sets aside $5,000 for their annual costs.  " +
            "<i>The employer FICA match is now reduced by $382.50 (7.65% of the $5,000) for just this one participant.</i></p>" +
            "<p>Thus, the added costs of including FSAs are generally less than the total savings generated by the program. " +
            "You can save money to offer your employees a plan that saves them thousands!</p>" +
            "<p><span style=\"background:black; color:white\"><b>&nbsp;Next Steps&nbsp;</b></span></p>" +
            "<p>As a current client, you have access to preferred pricing for our FSA services.  Your fees would now be:<ul>" +
            "<li>Updating your plan documents: $100 One-Time</li>" +
            "<li>Updated non-discrimination testing fee: $350 Annually (replaces current pop plan fee)</li>" +
            "<li>FSA Participant Fee: $4/month no card, $5/month with card</li></ul></p>" +
            "<p>To setup an FSA plan, or if you have any other questions regarding how such a plan might fit with your " +
            "company, please just let me know with a quick reply to this email.  We'll setup a phone call to work through whatever " +
            "is necessary.</p>";

    public static final String WELCOME_RENEWAL_GENERAL = "" +
            "<p><b>Getting Started</b></p>" +
            "<p>Hello, and thank you for being a Superior State customer.  This email is " +
            "to inform you that we are beginning the renewal process for one or more of your benefits.  I will " +
            "be your contact throughout this process.  If you have any questions please let me know.</p>";

    private static final String WELCOME_POP = "" +
            "<p><b>It's Time to Renew</b></p>" +
            "<p>Our records indicate you established a Section 125 premium pre-tax plan with us in the past. " +
            "In order to maintain compliance, each year your company must pass NON-DISCRIMINATION testing. " +
            "According to our information, your plan is renewing and it is time to run tests on your current plan arrangement.</p>" +
            "<p>Your company will be receiving an invoice for our services via email in the next day or two.  As soon as we receive payment " +
            "we will begin the process of collecting information in order to run your tests.  If you don't receive the invoice in the next few days, please " +
            "let us know and we will resend.  The invoice will allow online payment via credit/debit card or bank draft.  In order to " +
            "receive/maintain the discounted rate, we require payment to be made online.</p>";

    public static final String WELCOME_RENEWAL_POP = "" +
            WELCOME_POP +
            "<p><b><u>TIME TO CONSIDER FSAs?</u></b></p>" +
            FSA_ADVERT_2023;
    public static final String HSA_ADVERT = "" +
            "<p>Are you getting enough from your current HSA vendor?  Superior State offers Health Savings Accounts with " +
            "features and services that provide a unique and valuable solution to employers and their employee participants. " +
            "Most all vendors (including us) offer FDIC insured bank accounts with a debit card, however not all offer:<ul>" +
            "<li>INVESTMENTS</li>" +
            "<li>TAX FORM ASSISTANCE</li>" +
            "<li>PORTABILITY</li>" +
            "<li>RECORD-KEEPING SERVICES & FEATURES</li>" +
            "<li>COMPLIANCE SERVICES AND SUPPORT</li></ul>" +
            "A little further explanation of each item follows below.</p>" +
            "<p><b>INVESTMENTS</b><br/>" +
            "Our HSA account holders can voluntarily move their funds, or some portion of, into various mutual funds for the " +
            "opportunity for greater returns on their savings.  Our account offers the ability to pick and choose their funds and amounts" +
            " on their own, or they can choose from various models of pre-planned investment strategies that meet their needs.</p>" +
            "<p><b>TAX FORM ASSISTANCE</b><br/>" +
            "Did you know that when your employees open their HSA, they must now file a full form 1040 and include Form 8889, a form " +
            "specific to their HSA activity, with that 1040?  It is required, you can no longer file and form EZ or simpler version of " +
            "your taxes.  We provide our account holders a pre-filled form 8889 for their tax filing purposes which can help ease the added " +
            "tax filing burden that comes with these accounts.  All vendors mail the required 1099 (distribution) and 5498 (contribution) forms, but " +
            "it is unlikely they will provide the necessary form 8889 that goes with the 1040.</p>" +
            "<p><b>PORTABILITY</b><br/>" +
            "Did you get an HSA through your insurance provider?  It probably was quite convenient to setup and get going when you moved to the " +
            "HSA style plan.  However, what happens when you might want to change carriers?  Do you still have that easy connection?  Superior State's " +
            "HSA offering is completely independent of your choice of insurance carrier, and can be maintained when you switch from one carrier to " +
            "another, or even if you end your insurance offering altogether.</p>" +
            "<p><b>RECORD-KEEPING</b><br/>" +
            "When HSA account holders file their form 8889, they are likely making declarations that the funds they removed from their HSA that " +
            "year were for eligible reasons, and hence they shouldn't be taxed.  Well, if your return is audited, you'll need proof.  This could be a number of years " +
            "after they file their return.  So, how are people keeping their records?  In a shoebox?  That's fine for some, but maybe there is an " +
            "easier way.  Our HSA provides for the detailed record-keeping that comes with an HSA.  Our users can access it in a number of ways.  They can" +
            " use it as a self-service web portal or phone app where they are taking a quick picture of their receipts.  Others can send in information via " +
            "mail or fax to Superior State and we will log and upload the documentation.  Either way, the documentation is now secured and organized online, should " +
            "they ever need access to it.  AND IT'S PORTABLE</p>" +
            "<p><b>COMPLIANCE SERVICES</b><br/>" +
            "Do your employees know everything that's eligible for HSA withdrawals?  Do they know what they shouldn't use it for?  Who do they call if they have a question? " +
            "Superior State answers these types of questions every day.  We also provide CLAIM CERTIFICATION as part of our record keeping services.  If an employee submits documentation " +
            "to us, we log it in their records and can also certify that the documentation provided shows that it is an eligible expense.  Thus, such documentation " +
            "meets IRS standards of proof (in the event of an audit).</p>" +
            "<p>Should you have an interest in learning more about our Health Savings Account offering.  Please just let me know.  It costs $15 per account to open and $5 per month " +
            "per account to maintain.  We can bill you the employer or can draw the funds form the HSA accounts themselves, your option.</p>";
    public static final String WELCOME_RENEWAL_POP_1 = "" +
            WELCOME_POP +
            "<p style=\"color:blue\"><b><u>CAN SUPERIOR STATE HELP WITH YOUR HSA?</u></b></p>" +
            HSA_ADVERT;

    public static final String WELCOME_RENEWAL_POP_2 = "" +
            "<p><b>It's Time to Renew</b></p>" +
            "<p>Our records indicate you established a Section 125 premium pre-tax plan with us in the past. " +
            "In order to maintain compliance, each year your company must pass NON-DISCRIMINATION testing. " +
            "According to our information, your plan is renewing and it is time to run tests on your current plan arrangement.</p>" +
            "<p>Your company should have received an invoice for our services via email recently.  As soon as we receive payment " +
            "we will begin the process of collecting information in order to run your tests.  If you can't locate the invoice, please " +
            "let us know and we will resend.  The invoice will allow online payment via credit/debit card or bank draft.  In order to " +
            "receive/maintain the discounted rate, we require payment to be made online.</p>" +
            "<p>If you have any questions, please let me know.  We look forward to assisting you in maintaining your compliance.</p>";

    public static final String WELCOME_SETUP_FSA = "" +
            "<p><b>FSA Implementation</b></p>" +
            "<p>Thank you for applying for Superior State's Flexible Spending Account (FSA) administration services. " +
            "This is just a quick introductory note to let you know that we have received your online application and are beginning " +
            "work on your implementation.</p>" +
            "<p>The next step in the process is to get agreements signed between both of our companies.  You will receive, within " +
            "the next two business days, via email our service agreement.  It will be able to be e-signed for your convenience.  " +
            "It will be sent to you as the designated contact in the application, however, if you are not the appropriate signing authority " +
            "it will allow you to <b>delegate the agreement</b> to an appropriate company officer.  You will likely also receive an initial " +
            "invoice for payment of any Setup / Annual fees quite soon.</p><p>We are looking forward to helping you implement " +
            "these tax-favored benefits to your employees.  If you have any questions along the way, please let me know.</p>";


    public static final String WELCOME_NEW_POP="" +
            "<p><b>Online Application Received</b></p>" +
            "<p>This note is to let you know we have received your online application and are beginning work on " +
            "setting up your (P)remium (O)nly (P)lan, or POP for short.  The POP plan setup will consist of two compliance " +
            "items, 1) getting your plan established via the creation of Plan Documents and 2) performing non-discrimination testing " +
            "(note, this testing will occur each year at renewal).</p>" +
            "<p>In order for us to proceed, we require two things.  First, execution of a service agreement between our two companies.  Next, " +
            "payment of initial fees.  You will receive both items via email.  If the contract needs to be executed by a different individual, " +
            "you will be able to forward the agreement to the appropriate contact.  Once those items are addressed, we will move on to test your plan. " +
            "After successful testing results (or adjustments in some cases), we will send your plan documents for signature, which will complete your implementation.</p>";

    public static final String WELCOME_NEW_HSA="" +
            "<p><b>HSA Application Received</b></p>" +
            "<p>This note is to let you know we have received your online application and are beginning work on setting up" +
            " your Health Savings Account (HSA) services.</p>" +
            "<p>HSAs are individually owned bank accounts that each employee participant must establish separately.  We are constructing " +
            "the online enrollment portal for your company.  This will allow employees to open their HSA online and have it grouped under " +
            "your business.  This grouping allows for easier deposit management on your end, and simplicity in billing.</p>" +
            "<p>If you have a few employees who will not or can not utilize the online application, please let us know and we will send you a " +
            "paper application that can be filled out.  Once all accounts are opened, we will be in touch to go over our web portal and how you " +
            "can interact with the HSAs that are established.</p>";

    public static final String QUICK_HSA_CHECK = "" +
            "<p><b>HSA Identified In Test</b></p>" +

            "<p>Per your testing questionnaire, you identified that your plan offering includes Health Savings Accounts (HSAs). " +
            "Could you please confirm that 1) you are offering HSAs as a benefit this year and 2) that you wish to allow employees" +
            " to contribute to their individual HSA accounts via pre-tax payroll.</p>";

    public static final String FSA_WAIT_ON_ENROLL = "" +
            "<p><b>FSA Enrollments Outstanding</b></p>" +
            "<p>Just touching base to see how the enrollment process is coming along.  As a reminder, all we need is " +
            "enrollment information, not a specific form or format.  As a convenience, you may use our standard Salary " +
            "Reduction Agreement (SRA) form (link below) as a way to collect the enrollment.</p>" +
            "<p>Once we receive the enrollment forms and have finalized who is participating, we can move forward with " +
            "additional required testing based on participation counts.</p>";



    public static final String FEEDBACK_HSA_SINCE_NEW_WANT_TO_EXPAND = "" +
            "<p><b>Considerations for Adding HSA</b></p>" +
            "<p>Per your responses, you've identified that you are offering Health Savings Accounts (HSAs), and that you are allowing employees to contribute " +
            "(or desire to allow them to contribute) to them pre-tax through your 125 plan.  This is entirely permissible under a 125 plan, but as a practical matter your plan is " +
            "no longer a POP (<u>P</u>remium <u>O</u>nly <u>P</u>lan), as it includes other contributions that aren't insurance premiums.  " +
            "Further, it does require additional tests to be run in order to make sure your plan is operating in compliance.  " +
            "Be advised there is an additional $50 per year fee with these additional tests and include HSA contributions in your 125 plan.  " +
            "If you desire your plan documents to be modified to include such contributions, there would be a one-time amendment fee of $100.</p>" +
            "<p>HSA contributions do not have to be allowed under your 125 plan (you can maintain just a POP), employees can get an income tax " +
            "deduction for their post-tax HSA contributions through their 1040 tax filing.  However, when contributing to HSAs via payroll, " +
            "employees and employers both get the additional benefit of not paying the 7.65% FICA tax (or match).  That can only be done by " +
            "including HSAs in your Section 125 plan.</p>" +
            "<p>Please advise if you'd like us to expand your POP plan to now be a Section 125 plan that includes HSA contributions.</p>";

    public static final String COBRA_NEED_OPEN_ENROLLMENT_PACKET = "" +
            "<p><b>PDF of Enrollment Materials Requested</b></p>" +
            "<p>In your response to our inquiry for COBRA rates you indicated that you do have an open enrollment packet that is given to employees. " +
            "We will need a copy of that packet to include in our materials available to any current COBRA participants and/or qualified beneficiaries.</p>" +
            "<p>As a reminder, any COBRA participant may make elections for new benefits (subject to COBRA) and/or change levels of coverage during open " +
            "enrollment, just as any other employee would be able to. Please provide a copy of the packet in a PDF format so that we may update our " +
            "resource materials accordingly.</p>";

    public static final String COBRA_NEED_RATES_NOTICES = "" +
            "<p><b>Benefit Changes / Updates Needed</b></p>" +
            "<p>One of the primary steps involved in renewing your COBRA services is gathering any changes to your rates, or your benefit offering " +
            "in general.  In this instance, the matter has more urgency as you have at least one person who is in the COBRA process.</p>" +
            "<p>As such, this person/these persons must be informed of the new benefit offering(s) and costs associated with those benefits, so that they may appropriately " +
            "elect, change, or drop coverage(s).</p>" +
            "<p>We understand they may not be available, but if they are, please send as soon as possible via the link below.</p>" +
            "<p>If they are not, we may need to allow the previous year rates to be the rates for the upcoming month until it is sorted " +
            "out, in order to properly notify the qualified beneficiary.</p>";

    public static final String COBRA_NEED_RATES_GENERAL = "" +
            "<p><b>Updated Rates Needed</b></p>" +
            "<p>One of the primary steps involved in renewing your COBRA services is gathering any changes to your rates, or your benefit offering " +
            "in general.</p>" +
            "<p>If they are available and/or changes were made, please update as soon as possible via the link below</p>";

    public static final String POP_WAITING_ON_PAYMENT = "" +
            "<p><b>Await Payment</b></p>" +
            "<p>Just a quick follow up to let you know we are awaiting payment of your testing invoice and then will proceed with running your Section 125 pre-tax plan tests. " +
            "An invoice was sent previously that included the ability to pay online via bank draft or credit/debit card.  " +
            "If you have any questions, please let us know.</p>";

    public static final String POP_PASS_TEST_NO_HSA = "" +
            "<p><b>YOU PASS!</b></p>" +
            "<p>We have received and reviewed your Section 125 testing questionnaire.  From that information we are able to " +
            "determine that your plan passes the annual non-discrimination tests required based on your setup " +
            "and eligibility.  There is nothing further you need to do this year.</p>" +
            "<p>We will be back in touch a year from now to revisit the tests at that time.</p>";

    public static final String FEE_OUTSTANDING = "" +
            "<p><b>Renewal Invoice Still Outstanding</b></p>" +
            "<p>Just a quick reminder to advise that your annual fee for your plan's renewal is still due.  If you need us to send " +
            "another copy of the invoice via email, please let us know.</p>" +
            "<p>We greatly appreciate your business and look forward to our work together this coming year.</p>";

    public static final String POP_PASS_TEST_HAS_HSA = "" +
            "<p><b>YOU PASS!</b></p>" +
            "<p>Based on the enrollment information you provided, along with the classifications and wage groupings you identified, " +
            "we were able to run your Key Man test. At this time your test passes, and pre-taxing of premiums and employee HSA contributions " +
            "can continue.  Should drastic changes occur to your enrollment and consequentially your participation, you should consider " +
            "seeking a midyear interim test. If/when that occurs, please reach out to us and we can advise if in fact another test " +
            "needs to be run.</p>" +
            "<p>We will be in touch again about a year from now to review any changes to your benefits " +
            "and enrollments to run your tests for next year.</p>";

    public static final String FSA_PASS_TEST = "" +
            "<p><b>YOU PASS!</b></p>" +
            "<p>Based on the enrollment information you provided, along with the classifications and wage groupings you identified, " +
            "we were able to run your Key Man test, along with your eligibility and participation tests." +
            " At this time your test passes, should drastic changes occur to your enrollment and consequentially your participation, " +
            "you should consider " +
            "seeking a midyear interim test. If/when that occurs, please reach out to us and we can advise if in fact another test " +
            "needs to be run.</p>" +
            "<p>We will be in touch again about a year from now to review any changes to your benefits " +
            "and enrollments to run your tests for next year.</p>";

    public static final String HRA_PASS_TEST = "" +
            "<p><b>YOU PASS!</b></p>" +
            "<p>Based on the enrollment information you provided, along with the classifications and wage groupings you identified, " +
            "we were able to run your participation tests." +
            " At this time your test passes, should drastic changes occur to your enrollment and consequentially your participation, " +
            "you should consider " +
            "seeking a midyear interim test. If/when that occurs, please reach out to us and we can advise if in fact another test " +
            "needs to be run.</p>" +
            "<p>We will be in touch again about a year from now to review any changes to your benefits " +
            "and enrollments to run your tests for next year.</p>";

    public static final String GENERAL_PASS_TEST = "" +
            "<p><b>YOU PASS!</b></p>" +
            "<p>Based on the enrollment information you provided your test passes.</p>" +
            "<p>We will be in touch again about a year from now to review any changes to your benefits " +
            "and enrollments to run your tests for next year.</p>";

    public static final String TEST_ELIGIBILITY = "" +
            "<p><b>Eligibility (Non-Discrimination) Testing</b></p>" +
            "<p>We are ready to proceed with running the ELIGIBILITY test for your Section 125 plan. " +
            "In order for us to determine your compliance, we must get certain information from you first. " +
            "By following the link below, it will bring you to a questionnaire. " +
            "Please complete this at your earliest convenience.</p>";

    public static final String TEST_ELIGIBILITY_SETUP = "" +
            "<p><b>Eligibility (Non-Discrimination) Testing</b></p>" +
            "<p>One of the elements of your benefit plans with us, is that we perform the necessary non-discrimination " +
            "testing that is required by the government to maintain your tax-free status when offering this plan.</p>" +
            "<p>We are now ready to proceed with running the ELIGIBILITY test for your Section 125 plan. " +
            "In order for us to determine your compliance, we must get certain information from you first. " +
            "By following the link below, it will bring you to a questionnaire. " +
            "Please complete this at your earliest convenience.</p>";

    public static final String TEST_KEY_MAN = "" +
            "<p><b>Keyman Concentration Test</b></p>" +
            "<p>Because you offer employees the ability to contribute to their HSAs through pre-tax payroll, we must run an additional test to " +
            "determine your compliance with the rules of Section 125.  The test is called the KEYMAN test, and in order for us to run the test " +
            "we need to know information about who is using the pre-tax plan, what their income level is, and how much they are pre-taxing.</p>" +
            "<p>The first thing to tackle are the medical, dental, and/or vision plans that are offered by your company.  We will need the full premium " +
            "that you pay the carrier for each person participating (not the share they pay, the full carrier cost).  Many times, the rates are standard " +
            "for a given enrollment level (i.e., single, ee+spouse, family, etc...).  If that is the case, just let us know who is enrolled and and what " +
            "tier.</p>" +
            "<p>Once we have that information, we need to know how much any person is contributing to their HSA pre-tax.  This can be given to us as an annualized " +
            "total or a monthly or weekly amount.  It doesn't matter to us as long as it is clearly indicated.</p>" +
            "<p>Finally, we will need to determine which employees that are participating are KEY.  A key employee is defined as someone who is either:</p>" +
            "<p><ul><li>An <b>OFFICER</b> with annual compensation in excess of $" + KEYMAN_OFFICER_COMP +
            "</li>" +
            "<li>A greater than 5% Owner, or</li><li>A greater than 1% owner with annual compensation in excess of $" + KEYMAN_1_PER_OWNER_COMP +
            "</li></ul></p>" +
            "<p><b><u>Who is an Officer?</u></b><br/>Generally an administrative executive who is in regular and continued service, whose source of authority and " +
            "nature of duties fit that of an officer.  The number of officers in your organization will be:</p><ul><li>No greater than 50</li>" +
            "<li>At least 3, or 10% of the workforce, whichever is greater</li></ul>" +
            "<p>Reminder, the only officers that are Key are those making more than $" + KEYMAN_OFFICER_COMP + ".  So please, let us know which " +
            "employees are to be categorized as Key based on the 3 criteria above.</p>";

    public static final String PLAN_DOCUMENTATION = "" +
            "<p><b>Plan Documents</b></p>" +
            "<p>Thank you for providing the information for your testing.  After reviewing the data and running your tests, we can advise " +
            "that you pass plan testing for this upcoming plan year based on your current offering.</p>" +
            "<p>Attached you will find your plan documentation.  There are 3 documents provided.  The Plan Document and Adoption Agreement " +
            "comprise the formal legal documents.  The SPD should be distributed to plan participants.  All should be reviewed and executed " +
            "prior to their use.</p>" +
            "<p><b><i>Simple Cafeteria Plan</i></b></p>" +
            "<p>In the SPD and the Adoption Agreement, there may be a section near the end of the documents that discuss SIMPLE CAFETERIA " +
            "PLANS.  Leave these unchecked at this time.  Unless you fail your testing (which we would advise you of), structuring your plan " +
            "as a SIMPLE PLAN provides LESS flexibility in how to operate the plan.  We will advise in the future if you should enact this " +
            "structure.</p>";

    public static final String COBRA_COMPLETE = "" +
            "<p><b>COBRA Renewal Completed</b></p>" +
            "<p>Thank you for providing your most recent benefit information and rates.  They have been updated in our system, and are visible to you in your " +
            "HR portal, if you wish to review.  No further action is needed.</p>" +
            "<p>As always, if you have any questions, please don't hesitate to reach out.</p>";

    public static final String TEST_KEY_MAN_FSA = "" +
            "<p><b>Keyman Concentration Test</b></p>" +
            "<p>We have one final test to perform for your FSA plan. The test is called the KEYMAN test, and in order for us to run the test " +
            "we need to know information about who is using the pre-tax plan, what their income level is, and how much they are pre-taxing.</p>" +
            "<p>The first thing to tackle are the medical, dental, and/or vision plans that are offered by your company.  We will need the full premium " +
            "that you pay the carrier for each person participating (not the share they pay, the full carrier cost).  Many times, the rates are standard " +
            "for a given enrollment level (i.e., single, ee+spouse, family, etc...).  If that is the case, just let us know who is enrolled and and what " +
            "tier.</p>" +
            "<p>Once we have that information, we need to confirm how much any person is contributing to their FSAs pre-tax.  This can be achieved through confirmation" +
            " of an enrollment report or perhaps a payroll export.  It doesn't matter to us as long as it is clearly indicated.</p>" +
            "<p>Finally, we will need to determine which employees that are participating are KEY.  A key employee is defined as someone who is either:</p>" +
            "<p><ul><li>An <b>OFFICER</b> with annual compensation in excess of $" + KEYMAN_OFFICER_COMP +
            "</li>" +
            "<li>A greater than 5% Owner, or</li><li>A greater than 1% owner with annual compensation in excess of $" + KEYMAN_1_PER_OWNER_COMP +
            "</li></ul></p>" +
            "<p><b><u>Who is an Officer?</u></b><br/>Generally an administrative executive who is in regular and continued service, whose source of authority and " +
            "nature of duties fit that of an officer.  The number of officers in your organization will be:</p><ul><li>No greater than 50</li>" +
            "<li>At least 3, or 10% of the workforce, whichever is greater</li></ul>" +
            "<p>Reminder, the only officers that are Key are those making more than $" + KEYMAN_OFFICER_COMP + ".  So please, let us know which " +
            "employees are to be categorized as Key based on the 3 criteria above.</p>";
    public static final String TEST_PARTICIPATION = "" +
            "<p><b>Participation (Non-Discrimination) Testing</b></p>" +

            "<p>We are ready to proceed with running the PARTICIPATION test for your Section 105 FSA or HRA plan. " +
            "In order for us to help determine your compliance with the testing requirements, we need to gather information " +
            "regarding who is participating in the plan.  Please complete the questionnaire, which can be reached by clicking " +
            "the link below.</p>";

    public static final String EE_APP_CARD ="" +
            "<p><b>Employee Debit Card Application</b></p>" +
            "<p>As part of your benefit offering, your employer is providing Debit Cards as a means to access" +
            " your benefits.  You (and your spouse) can request a card by completing the form below.</p>";




    public static String hsaApplicationInstructions(String myRscCode){
        String theText = "";
        theText = "<p><b>INDIVIDUAL HSA ACCOUNT APPLICATION INSTRUCTIONS</b></p>" +
                "Your employer enrollment portal is now setup and employees may being applying online to open their " +
                "individual Health Savings Account (HSA).  To do so:</p>" +
                "<ul><li>Go to <a href=\"https://secure.myrsc.com/hsaenroll\" target=\"_blank\">https://secure.myrsc.com/hsaenroll</a></li>" +
                "<li>Click on <b>ENROLL NOW!</b> in the upper right of the page.</li>" +
                "<li>When prompted enter " + myRscCode + " as your employer code.</li></ul>";
        return theText;
    }
    public static String insertRenewalBenefitList(EntityManager em, long renewalId){
        Renewal renewal = EntityLookup.getRenewalById(em,renewalId);
        StringBuilder path = new StringBuilder("<p><u>Benefits In Renewal</u></p><p><ul>");
        assert renewal != null;
        for(RenewalItem ri:renewal.getRenewalItemList()){
            path.append("<li>").append(ri.getBenefit().getPlanDescription()).append("</li>");
        }
        path.append("</ul</p>");
        return path.toString();
    }

    private static String insertWebLink(EntityManager em, Long webLinkId,String title){
        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.id = :id");
        q.setParameter("id",webLinkId);
        WebLink w = (WebLink) q.getSingleResult();
        return "<p><a href=\"" + w.getLinkPath()+ "\" target=\"_blank\">" + title + "</a></p>";
    }

    public static String insertSRA(EntityManager em){
        return insertWebLink(em,1106L,"Salary Reduction Agreement (SRA)");
    }

    public static String insert125Test(int renewalId, String erName){
        return "<p><a href=\"https://form.jotform.com/92684899657181?renewalId="+renewalId+
                "&nameIn="+erName+"&textOrigin=1\" target=\"_blank\">125 Eligibility Test Questionnaire</a></p>";
    }
    public static String insert125Test(String guid, String erName){
        return "<p><a href=\"https://form.jotform.com/92684899657181?queryString="+guid+
                "&nameIn="+erName+"&textOrigin=1\" target=\"_blank\">125 Eligibility Test Questionnaire</a></p>";
    }
    public static String insert125Test(Activity a){
        String classType = a.getClass().getSimpleName();
        if(classType.equals("Renewal")){
            return insert125Test(a.getId().intValue(),a.getFullName());
        } else if (classType.equals("Setup")) {
            Setup s = (Setup) a;
            return insert125Test(s.getApplication().getProposal().getApplicationGUID(),s.getFullName());
        }
        return null;
    }

    public static String insert105TestGoogle(String erName){
        String myLink = "https://docs.google.com/forms/d/e/1FAIpQLSeanQw64hrvJ75meVyWVTkWYACOmEVl3sb5yw_Az4MMtN3PgA/viewform";
        return "<p><a href=\"" + myLink + "\" target=\"_blank\">FSA/HRA Participation Test</a></p>";
    }

    public static String insertCobraTakeover(String app_key){
        return "<p><a href=\"https://form.jotform.com/212594164491055?ApplicationId="+app_key+ "\""+
                " target=\"_blank\">COBRA Participant / Beneficiary Takeover Form</a></p>";
    }

    public static String insertErEftForm(String app_key){
        return "<p><a href=\"https://form.jotform.com/212714328469056?qid="+app_key+ "\""+
                " target=\"_blank\">Payment Services Application Form</a></p>";
    }

    public static String insertCobraQuestionnaireGoogle(Long renewalId, String erName){
        String myLink = "https://docs.google.com/forms/d/e/1FAIpQLSdwYlUQTsnYZoFqWrjPdoO50Okzyfx3KraqxklxWhYSGB4Img/viewform?usp=pp_url&entry.63434542=" + renewalId +
                "&entry.2100560293=" + erName;
        return "<p><a href=\"" + myLink + "\" target=\"_blank\">COBRA Rate Questionnaire</a></p>";
    }

    public static String insertErCardApplication(String app_key, String theEmail){
        return "<p><a href=\"https://form.jotform.com/212563952994064?QID="+app_key+
                "&Email= "+theEmail+"\" target=\"_blank\">Employer Debit Card Application</a></p>";
    }

    private static String insertEeCardApplication(String app_key, String erName){
        return "<p><a href=\"https://form.jotform.com/212563952994064?qid="+app_key+
                "&erName= "+erName+"\" target=\"_blank\">Employee Debit Card Application</a></p>";
    }

    public static final String INSERT_DIR_DEP_APP = "" +
            "<p><a href=\"https://form.jotform.com/212714102254038\" target=\"_blank\">Employee Direct Deposit Form</a></p>";

    public static boolean hasHra(Renewal r){
        boolean hasHra = false;
        for(RenewalItem ri:r.getRenewalItemList()){
            int ptId = ri.getBenefit().getPlanType().getPlanTypeId();
            if(ptId == 3 || ptId == 6 || ptId == 1002 || ptId == 1003 || ptId == 1004) {
                hasHra = true;
                break;
            }
        }
        return hasHra;
    }

    public static boolean hasFsa(Renewal r){
        boolean hasFsa = false;
        for(RenewalItem ri:r.getRenewalItemList()){
            int ptId = ri.getBenefit().getPlanType().getPlanTypeId();
            if(ptId == 1 || ptId == 2 || ptId == 5 || ptId == 1001) {
                hasFsa = true;
                break;
            }
        }
        return hasFsa;
    }

    public static boolean hasDCAP(Renewal r){
        boolean hasDCAP = false;
        for(RenewalItem ri:r.getRenewalItemList()){
            int ptId = ri.getBenefit().getPlanType().getPlanTypeId();
            if(ptId == 1) {
                hasDCAP = true;
                break;
            }
        }
        return hasDCAP;
    }
    public static boolean hasCobraInsurance(Renewal r){
        boolean hasCobraInsurance = false;
        for(RenewalItem ri:r.getRenewalItemList()){
            int ptId = ri.getBenefit().getPlanType().getPlanTypeId();
            if(ptId >= 9 && ptId<=15) {
                hasCobraInsurance = true;
                break;
            }
        }
        return hasCobraInsurance;
    }

    public static boolean isPop(Renewal r){
        boolean hasPop = false;
        for(RenewalItem ri:r.getRenewalItemList()){
            int ptId = ri.getBenefit().getPlanType().getPlanTypeId();
            if(ptId == 1005) {
                hasPop = true;
                break;
            }
        }
        return hasPop && !hasFsa(r) && !hasHra(r);
    }

    public static boolean hasQbs(EntityManager em, Renewal r){
        boolean hasQb = false;
        List<ToDo> toDoList;
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.renewal.id = :id");
        q.setParameter("id",r.getId());
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            toDoList = null;
        }
        if(toDoList!=null && toDoList.size()>0){
            for(ToDo t: toDoList){
                if(t.getTask().getId()==565L){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasHsa(EntityManager em,Renewal r){
        boolean hasHsa = false;
        for(RenewalItem ri:r.getRenewalItemList()){
            int ptId = ri.getBenefit().getPlanType().getPlanTypeId();
            if(ptId == 4) {
                hasHsa = true;
                break;
            }
        }
        if(hasHsa)
            return true;
        List<ToDo> toDoList;
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.renewal.id = :id");
        q.setParameter("id",r.getId());
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            toDoList = null;
        }
        if(toDoList!=null && toDoList.size()>0){
            for(ToDo t: toDoList){
                if(t.getTask().getId()==2158L){
                    hasHsa = true;
                    break;
                }
            }
        }
        return hasHsa;
    }

    private static ToDo findToDo(EntityManager em, Renewal r, Long taskId){
        ToDo t;
        Query q= em.createQuery("SELECT t FROM ToDo t WHERE t.task.id = :taskId AND t.checkList.id = :checkId");
        q.setParameter("taskId", taskId);
        q.setParameter("checkId", r.getCheckList().getId());
        try{
            t = (ToDo) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        return t;
    }
    private static ToDo createToDo(EntityManager em, Renewal r, Person p, Long taskId, int sort){
        Task t = EntityLookup.getTaskById(em,taskId);
        CheckList c = EntityLookup.getCheckListById(em,r.getCheckList().getId());

        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setTask(t);
        toDo.setComplete(false);
        toDo.setDateCompleted(null);
        toDo.setCheckList(c);
        toDo.setSortOrder(sort);
        em.persist(toDo);
        em.getTransaction().commit();

        CheckList checkList = EntityLookup.getCheckListById(em,c.getId());
        ToDo t1 = EntityLookup.getToDoById(em,toDo.getId());

        em.getTransaction().begin();
        checkList.getToDoList().add(t1);
        em.persist(checkList);
        em.getTransaction().commit();
        return t1;
    }

    public static ToDo addNextTask1(EntityManager em, Renewal r, Person p, Long taskId, int sort){
        ToDo t = findToDo(em,r,taskId);
        ToDo t1;
        if(t==null)
            t1 = createToDo(em,r,p,taskId,sort);
        else {
            t1 = EntityLookup.getToDoById(em, t.getId());
            em.getTransaction().begin();
            t1.setComplete(false);
            t1.setDateCompleted(null);
            em.persist(t1);
            em.getTransaction().commit();
        }
        return t1;
    }
    public static void addNextTask(HttpServletRequest request, EntityManager em, Renewal r, Person p, Long taskId, int sort){

        ToDo test = findToDo(em,r,taskId);
        if(test==null)
            createToDo(em,r,p,taskId,sort);
        else {
            ToDo t1 = EntityLookup.getToDoById(em, test.getId());
            em.getTransaction().begin();
            t1.setComplete(false);
            t1.setDateCompleted(null);
            em.persist(t1);
            em.getTransaction().commit();
        }
    }

    private static List<Email> getQuickActions(EntityManager em, Renewal r){
        Query q = em.createQuery("SELECT e FROM Email e WHERE e.reasonCreated.id = :rId order by e.id desc");
        q.setParameter("rId",8);
        List<Email> quickActionList;
        try{
            quickActionList = (List<Email>) q.getResultList();
        } catch (NoResultException e){
            return null;
        }
        return quickActionList;
    }

    private static String followUp(EntityManager em, Renewal r, String standardStatement){
        int timesPreviouslyRequested = headerCount(em,r,standardStatement);
        if(timesPreviouslyRequested==0)
            return "";
        Date lastRequested = mostRecent(em,r,standardStatement);
        String response;
        if(timesPreviouslyRequested==1){
            response = "<p>Following up on our last request on " + lastRequested.toString() +"</p>";
        } else {
            response = "<p>We ask for your attention to the matter below.  We have been in touch previously, most recently on " +
                    lastRequested.toString() + ".  We understand that due to other factors, providing this information may not be possible " +
                    "or easily achieved, but we will need it nonetheless to complete the renewal.  If you have any questions, please let us " +
                    "know.</p>";
        }
        return response;
    }

    public static final String REMINDER_TEXT = "<p style=\"color:red;\"><i>Following up on our previous request, please see the information we need to proceed as detailed below." +
            " If you have any questions, please don't hesitate to ask.</i></p>" ;

    private static int headerCount(EntityManager em, Renewal r, String standardStatement){
        List<Email> emailList = getQuickActions(em,r);
        String check = getHeader(standardStatement);
        String compare;
        int timesSent = 0;
        for(Email e: emailList){
            compare = getHeader(e.getDetail());
            if(check.equals(compare))
                timesSent ++;
        }
        return timesSent;
    }

    private static Date mostRecent(EntityManager em, Renewal r, String standardStatement){
        List<Email> emailList = getQuickActions(em,r);
        String check = getHeader(standardStatement);
        String compare;
        Date mostRecent = null;
        for(Email e: emailList){
            compare = getHeader(e.getDetail());
            if(check.equals(compare)){
                mostRecent = e.getDateGenerated();
                break;
            }
        }
        return mostRecent;
    }

    private static String getHeader(String statement){
        int indexStart = statement.indexOf("<p><b>");
        int indexEnd = statement.indexOf("</b></p>");
        if(indexStart==-1 || indexEnd ==-1)
            return "";
        return statement.substring(indexStart,indexEnd+8);
    }

    public static String qText(EntityManager em, Renewal r, String standardStatement){
        int indexEnd = standardStatement.indexOf("</b></p>");
        String header = standardStatement.substring(0,indexEnd+8);
        String footer = standardStatement.substring(indexEnd+8);
        return header + REMINDER_TEXT + footer;
    }
    public static String qText(EntityManager em, Setup s, String standardStatement){
        int indexEnd = standardStatement.indexOf("</b></p>");
        String header = standardStatement.substring(0,indexEnd+8);
        String footer = standardStatement.substring(indexEnd+8);
        return header + standardStatement + footer;
    }

    public static String qText(EntityManager em, Activity a, String std){
        String classType = a.getClass().getSimpleName();
        System.out.println(classType);
        if(classType.equals("Renewal")){
            return qText(em,(Renewal) a,std);
        } else if(classType.equals("Setup")){
            return qText(em,(Setup) a,std);
        }
        return "";
    }


}
