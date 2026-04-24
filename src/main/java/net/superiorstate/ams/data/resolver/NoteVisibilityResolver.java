package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.model.activity.note.Note;

/**
 * Resolves the effective agent visibility of a {@link Note}.
 *
 * <p>Per-note override ({@code note.agent_visible}):
 * <ul>
 *   <li>{@code TRUE}  → visible to agent portal regardless of PSP default</li>
 *   <li>{@code FALSE} → hidden from agent portal regardless of PSP default</li>
 *   <li>{@code NULL}  → fall back to PSP default (constant
 *       {@code NOTES_AGENT_VISIBLE_DEFAULT}, default {@code false} = hidden)</li>
 * </ul>
 *
 * <p>Callers that already know the PSP default (e.g. when rendering many
 * notes on one page) should use {@link #isVisibleToAgent(Note, boolean)} to
 * avoid re-reading the constant per note.
 */
public final class NoteVisibilityResolver {

    private static final String DEFAULT_CONSTANT_KEY = "NOTES_AGENT_VISIBLE_DEFAULT";

    private NoteVisibilityResolver() {}

    public static boolean isVisibleToAgent(Note note, EntityManager em) {
        if (note == null) return false;
        Boolean override = note.getAgentVisible();
        if (override != null) return override;
        return resolvePspDefault(em);
    }

    public static boolean isVisibleToAgent(Note note, boolean pspDefault) {
        if (note == null) return false;
        Boolean override = note.getAgentVisible();
        return override != null ? override : pspDefault;
    }

    public static boolean resolvePspDefault(EntityManager em) {
        String v = AppConstantDAO.getConstantValue(em, DEFAULT_CONSTANT_KEY);
        return "true".equalsIgnoreCase(v);
    }
}
