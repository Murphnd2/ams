package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.general.ChatbotSkill;

import java.util.List;

public abstract class ChatbotSkillDAO {

    /**
     * Returns all active skills for a PSP, ordered by sort_order.
     */
    public static List<ChatbotSkill> getActiveSkills(EntityManager em, Long pspId) {
        return em.createQuery(
                "SELECT s FROM ChatbotSkill s WHERE s.psp.id = :pspId AND s.active = true ORDER BY s.sortOrder",
                ChatbotSkill.class)
                .setParameter("pspId", pspId)
                .getResultList();
    }

    /**
     * Returns all skills for a PSP (active and inactive), for the admin UI.
     */
    public static List<ChatbotSkill> getAllSkills(EntityManager em, Long pspId) {
        return em.createQuery(
                "SELECT s FROM ChatbotSkill s WHERE s.psp.id = :pspId ORDER BY s.sortOrder, s.skillName",
                ChatbotSkill.class)
                .setParameter("pspId", pspId)
                .getResultList();
    }

    /**
     * Returns the first active skill matching the given name for a specific PSP,
     * or null if no match is found.
     * Used by internal endpoints (e.g. EmailAssistantDraft) that look up a skill
     * by its well-known name rather than by keyword trigger.
     */
    public static ChatbotSkill getBySkillName(EntityManager em, String skillName, Long pspId) {
        try {
            List<ChatbotSkill> results = em.createQuery(
                    "SELECT s FROM ChatbotSkill s WHERE s.skillName = :name AND s.psp.id = :pspId",
                    ChatbotSkill.class)
                    .setParameter("name", skillName)
                    .setParameter("pspId", pspId)
                    .setMaxResults(1)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            System.err.println("❌ getBySkillName: " + e.getMessage());
            return null;
        }
    }

    /**
     * Finds the best matching skill for a user message, optionally with a file.
     *
     * @param skills      active skills list (pre-filtered for role)
     * @param messageText the user's text message (may be empty if file-only)
     * @param fileMimeType the MIME type of an attached file, or null if no file
     * @return the best matching skill, or null if no match
     */
    public static ChatbotSkill findMatchingSkill(List<ChatbotSkill> skills, String messageText, String fileMimeType) {
        // Priority 1: If a file is attached, find a skill that accepts this file type
        if (fileMimeType != null) {
            ChatbotSkill bestFileSkill = null;
            int bestScore = 0;
            for (ChatbotSkill s : skills) {
                if (s.isAcceptsFileUpload() && s.acceptsMimeType(fileMimeType)) {
                    int score = scoreKeywordMatch(s, messageText) + 10;
                    if (score > bestScore) {
                        bestScore = score;
                        bestFileSkill = s;
                    }
                }
            }
            if (bestFileSkill != null) return bestFileSkill;
        }

        // Priority 2: Keyword match on message text
        if (messageText != null && !messageText.isBlank()) {
            ChatbotSkill bestTextSkill = null;
            int bestScore = 0;
            for (ChatbotSkill s : skills) {
                int score = scoreKeywordMatch(s, messageText);
                if (score > bestScore) {
                    bestScore = score;
                    bestTextSkill = s;
                }
            }
            // Require at least 2 keyword hits to activate via text alone
            if (bestTextSkill != null && bestScore >= 2) return bestTextSkill;
        }

        return null;
    }

    private static int scoreKeywordMatch(ChatbotSkill skill, String text) {
        if (text == null || text.isBlank()) return 0;
        String lower = text.toLowerCase();
        int score = 0;
        for (String kw : skill.getTriggerKeywordSet()) {
            if (lower.contains(kw)) score++;
        }
        return score;
    }
}
