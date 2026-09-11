package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.SummitServiceItemFlagsDAO;
import net.superiorstate.ams.model.market.SummitServiceItemFlags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * T238 part 1 / D46 — computes the four Summit Employer Demographic administration flags for one
 * employer, as the union (logical OR) across every elected {@code ServiceItem} that has a row in
 * {@code summit_service_item_flags} (V101). Each elected item that maps to CDH, COBRA, Retiree
 * Billing or Direct Bill contributes that flag; an item with no row contributes nothing.
 * <p>
 * <b>Not yet read by {@code SummitExportServlet}.</b> The file 1 (Employer Demographic) emitter
 * change that would call this is a later, separate build — it is blocked on SDX-27 (does Summit
 * honor an explicit {@code false} on create, and what does {@code false} do to a flag already on).
 * Until that build ships, this resolver has no caller and changes no export behaviour.
 * <p>
 * ⚠️ <b>Guidance, not enforced here:</b> when that emitter build lands, it should refuse to emit
 * file 1 rather than write it when {@link EmployerFlags#getMappedCount()} is zero — a file with all
 * four flags {@code false} would create a Summit employer with no administration enabled at all,
 * which is a worse silent failure than refusing outright. This class only reports the count; the
 * refuse-or-emit decision belongs to the emitter that does not yet exist.
 */
public final class SummitEmployerFlagResolver {

    private SummitEmployerFlagResolver() {}

    /** The four flags, unioned across every elected item that has a mapping, plus how many did. */
    public static final class EmployerFlags {
        private final boolean cdh;
        private final boolean cobra;
        private final boolean retireeBilling;
        private final boolean directBill;
        private final int mappedCount;
        private final List<Integer> unmappedServiceItemIds;

        private EmployerFlags(boolean cdh, boolean cobra, boolean retireeBilling, boolean directBill,
                              int mappedCount, List<Integer> unmappedServiceItemIds) {
            this.cdh = cdh;
            this.cobra = cobra;
            this.retireeBilling = retireeBilling;
            this.directBill = directBill;
            this.mappedCount = mappedCount;
            this.unmappedServiceItemIds = unmappedServiceItemIds;
        }

        private static EmployerFlags empty() {
            return new EmployerFlags(false, false, false, false, 0, List.of());
        }

        public boolean isCdh() { return cdh; }
        public boolean isCobra() { return cobra; }
        public boolean isRetireeBilling() { return retireeBilling; }
        public boolean isDirectBill() { return directBill; }
        /** How many elected items had a mapping row. Zero means the union above is all-false by default, not by decision. */
        public int getMappedCount() { return mappedCount; }
        /** Elected item ids with no row in {@code summit_service_item_flags}, in the order given. Never null. */
        public List<Integer> getUnmappedServiceItemIds() { return unmappedServiceItemIds; }
    }

    /**
     * @param electedServiceItemIds the ServiceItem ids the setup actually elected; null or empty
     *                              returns all four flags {@code false}, {@code mappedCount = 0}
     *                              and an empty unmapped list — there is nothing to union.
     */
    public static EmployerFlags flagsFor(EntityManager em, Long pspId, Collection<Integer> electedServiceItemIds) {
        if (electedServiceItemIds == null || electedServiceItemIds.isEmpty()) {
            return EmployerFlags.empty();
        }

        List<SummitServiceItemFlags> mapped =
                SummitServiceItemFlagsDAO.findByPspAndServiceItems(em, pspId, electedServiceItemIds);

        boolean cdh = false;
        boolean cobra = false;
        boolean retireeBilling = false;
        boolean directBill = false;
        for (SummitServiceItemFlags flags : mapped) {
            cdh |= flags.isEnableCdh();
            cobra |= flags.isEnableCobra();
            retireeBilling |= flags.isEnableRetireeBilling();
            directBill |= flags.isEnableDirectBill();
        }

        List<Integer> unmapped = new ArrayList<>();
        for (Integer id : electedServiceItemIds) {
            boolean found = false;
            for (SummitServiceItemFlags flags : mapped) {
                if (flags.getServiceItemId() != null && flags.getServiceItemId().equals(id)) { found = true; break; }
            }
            if (!found) unmapped.add(id);
        }

        return new EmployerFlags(cdh, cobra, retireeBilling, directBill, mapped.size(),
                Collections.unmodifiableList(unmapped));
    }
}
