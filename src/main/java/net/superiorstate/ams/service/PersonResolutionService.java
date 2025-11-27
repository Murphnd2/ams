// src/main/java/net/superiorstate/ams/service/PersonResolutionService.java
package net.superiorstate.ams.service;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.previous.model.general.Person;

/**
 * Thread-safe, no-static-fields replacement for the old eV class.
 * Will be filled step by step. For now just a shell so everything compiles.
 */
public class PersonResolutionService {

    // Singleton instance — exactly how you probably already do AmsDataLocal, etc.
    private static final PersonResolutionService INSTANCE = new PersonResolutionService();

    // Private constructor — prevents new PersonResolutionService()
    private PersonResolutionService() {}

    public static PersonResolutionService getInstance() {
        return INSTANCE;
    }

    /**
     * Temporary stub — returns null.
     * We will replace this method body one piece at a time in the next steps that follow.
     */
    public Person getBestPersonFromString(EntityManager em, String input) {
        // temporary — will be replaced in Step 3
        return null;
    }

    // Future helper methods will go here (no statics!)
}