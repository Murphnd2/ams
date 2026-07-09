package net.superiorstate.ams.model.sales.agency;

import net.superiorstate.ams.model.sales.offering.ServiceModule;

/**
 * Read-model for a single proposal pricing row: the shared RateTable base
 * price plus any per-proposal ProposalPriceAdjustment markup for that
 * (module, priceItem) pair. sellPrice = basePrice + markup, always computed
 * here rather than stored, so it can never drift from its inputs.
 */
public class ProposalPriceLine {

    private final ServiceModule module;
    private final PriceItem priceItem;
    private final double basePrice;
    private final double markup;

    public ProposalPriceLine(ServiceModule module, PriceItem priceItem, double basePrice, double markup) {
        this.module = module;
        this.priceItem = priceItem;
        this.basePrice = basePrice;
        this.markup = markup;
    }

    public ServiceModule getModule() {
        return module;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getMarkup() {
        return markup;
    }

    public double getSellPrice() {
        return basePrice + markup;
    }
}
