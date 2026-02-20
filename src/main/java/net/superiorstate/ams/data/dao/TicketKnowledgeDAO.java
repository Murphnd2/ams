package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Queries completed tickets with resolution notes for use as
 * live knowledge base context in the AI Knowledge Assistant.
 */
public abstract class TicketKnowledgeDAO {

    private static final Logger log = LogManager.getLogger(TicketKnowledgeDAO.class);

    /**
     * Searches completed tickets whose subcategory description or resolution note
     * matches any of the given search terms. Returns formatted context strings
     * ready to be injected into the Claude prompt.
     *
     * @param em          EntityManager
     * @param searchTerms list of lowercase search words from the user's question
     * @param maxResults  maximum number of ticket results to return
     * @return list of formatted ticket context strings
     */
    public static List<String> searchResolvedTickets(EntityManager em, List<String> searchTerms, int maxResults) {
        if (searchTerms == null || searchTerms.isEmpty()) return new ArrayList<>();

        try {
            // Build JPQL with OR conditions for each search term
            // Searches across: subcategory description, category description, ticket description, resolution note detail
            StringBuilder jpql = new StringBuilder();
            jpql.append("SELECT t.description, ");
            jpql.append("tsc.description, ");
            jpql.append("tc.description, ");
            jpql.append("n.detail, ");
            jpql.append("n.dateGenerated, ");
            jpql.append("t.dateCompleted ");
            jpql.append("FROM Ticket t ");
            jpql.append("JOIN t.ticketSubCategory tsc ");
            jpql.append("JOIN tsc.ticketCategory tc ");
            jpql.append("JOIN t.noteList n ");
            jpql.append("WHERE t.isComplete = true ");
            jpql.append("AND n.isResolution = true ");
            jpql.append("AND t.dateCreated >= :legacyCutoff ");
            jpql.append("AND (");

            for (int i = 0; i < searchTerms.size(); i++) {
                if (i > 0) jpql.append(" OR ");
                String param = "term" + i;
                jpql.append("LOWER(tsc.description) LIKE :").append(param).append(" ");
                jpql.append("OR LOWER(tc.description) LIKE :").append(param).append(" ");
                jpql.append("OR LOWER(t.description) LIKE :").append(param).append(" ");
                jpql.append("OR LOWER(n.detail) LIKE :").append(param);
            }
            jpql.append(") ");
            jpql.append("ORDER BY t.dateCompleted DESC");

            Query q = em.createQuery(jpql.toString());
            for (int i = 0; i < searchTerms.size(); i++) {
                q.setParameter("term" + i, "%" + searchTerms.get(i) + "%");
            }
            q.setMaxResults(maxResults);
            q.setParameter("legacyCutoff", java.sql.Timestamp.valueOf("2026-02-19 00:00:00"));

            @SuppressWarnings("unchecked")
            List<Object[]> rows = q.getResultList();

            List<String> results = new ArrayList<>();
            for (Object[] row : rows) {
                String ticketDesc = str(row[0]);
                String subCatDesc = str(row[1]);
                String catDesc = str(row[2]);
                String noteDetail = str(row[3]);
                String noteDate = row[4] != null ? row[4].toString() : "";
                String completedDate = row[5] != null ? row[5].toString() : "";

                StringBuilder entry = new StringBuilder();
                entry.append("--- Source: Resolved Tickets | ").append(catDesc);
                entry.append(" > ").append(subCatDesc).append(" ---\n");
                entry.append("Issue: ").append(ticketDesc).append("\n");
                entry.append("Resolution: ").append(stripHtml(noteDetail)).append("\n");
                if (!completedDate.isEmpty()) {
                    entry.append("Resolved: ").append(completedDate).append("\n");
                }
                results.add(entry.toString());
            }

            log.debug("Ticket knowledge search returned {} results for terms: {}", results.size(), searchTerms);
            return results;

        } catch (Exception e) {
            log.error("Error searching ticket knowledge", e);
            return new ArrayList<>();
        }
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    /**
     * Strips basic HTML tags from note detail text.
     * Notes may contain HTML from CKEditor.
     */
    private static String stripHtml(String text) {
        if (text == null || text.isBlank()) return "";
        return text.replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }
}