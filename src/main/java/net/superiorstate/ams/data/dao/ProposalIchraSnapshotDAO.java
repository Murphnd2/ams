package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.model.sales.agency.ProposalIchraSnapshot;
import net.superiorstate.ams.model.sales.agency.ProposalIchraSnapshotBand;

import java.util.List;

/**
 * DAO for {@link ProposalIchraSnapshot} / {@link ProposalIchraSnapshotBand}.
 * <p>
 * ⚠️ {@link #findByProposalId} and {@link #findBandsBySnapshotId} are two flat
 * queries off their own roots — never a two-level nested {@code JOIN FETCH}
 * (snapshot → bands). EclipseLink silently drops the second-level collection on a
 * nested fetch in this codebase (see {@code AgentHome}'s
 * {@code Opportunity → prospect → proposalList} bug, fixed v0.71.08); a section that
 * renders with no band rows and no error is exactly the failure this DAO exists to
 * avoid.
 */
public abstract class ProposalIchraSnapshotDAO {

    /** The snapshot for a proposal, or null if none exists (no ICHRA hand-off, or write failed closed). */
    public static ProposalIchraSnapshot findByProposalId(EntityManager em, Long proposalId) {
        try {
            return em.createQuery(
                            "SELECT s FROM ProposalIchraSnapshot s WHERE s.proposal.id = :proposalId", ProposalIchraSnapshot.class)
                    .setParameter("proposalId", proposalId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** All band rows for a snapshot, ordered by sortOrder. Empty for a RANGE snapshot. Flat query off its own root — never nested under the snapshot fetch. */
    public static List<ProposalIchraSnapshotBand> findBandsBySnapshotId(EntityManager em, Long snapshotId) {
        return em.createQuery(
                        "SELECT b FROM ProposalIchraSnapshotBand b WHERE b.snapshot.id = :snapshotId ORDER BY b.sortOrder", ProposalIchraSnapshotBand.class)
                .setParameter("snapshotId", snapshotId)
                .getResultList();
    }

    /** Persists a snapshot and its band rows (if any) in one transaction. */
    public static void save(EntityManager em, ProposalIchraSnapshot snapshot, List<ProposalIchraSnapshotBand> bands) {
        em.getTransaction().begin();
        try {
            em.persist(snapshot);
            if (bands != null) {
                for (ProposalIchraSnapshotBand band : bands) {
                    band.setSnapshot(snapshot);
                    em.persist(band);
                }
            }
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }
}
