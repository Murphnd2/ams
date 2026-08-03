package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.model.sales.agency.ProposalIchraIntake;

/**
 * DAO for {@link ProposalIchraIntake} — the T125 plus-tier ZIP/county/headcount capture.
 * Mirrors {@code ProposalIchraSnapshotDAO}'s transaction idiom.
 */
public abstract class ProposalIchraIntakeDAO {

    /** The intake for a proposal, or null if none exists. */
    public static ProposalIchraIntake findByProposalId(EntityManager em, Long proposalId) {
        try {
            return em.createQuery(
                            "SELECT i FROM ProposalIchraIntake i WHERE i.proposal.id = :proposalId", ProposalIchraIntake.class)
                    .setParameter("proposalId", proposalId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Persists an intake row. {@code proposal_id} is {@code UNIQUE} — a second write for
     * the same proposal throws here; the caller ({@code ProposalBuilder.attachIchraIntakeIfPresent},
     * wrapped by its own best-effort try/catch) logs it and does not retry. There is no
     * edit-after-create path (spec §6 item 3).
     */
    public static void save(EntityManager em, ProposalIchraIntake intake) {
        em.getTransaction().begin();
        try {
            em.persist(intake);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }
}
