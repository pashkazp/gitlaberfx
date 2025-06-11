package com.depavlo.gitlaberfx.model;

import java.util.List;

/**
 * Container class for the result of the delete confirmation dialog.
 * This class encapsulates the user's choice in the delete confirmation dialog,
 * including the list of branches to process and whether to archive them instead of deleting.
 */
public class OperationConfirmationResult {
    private final List<BranchModel> confirmedBranches;
    private final boolean archive; // true if archive option is selected

    /**
     * Constructs a new OperationConfirmationResult with the specified branches and archive flag.
     *
     * @param confirmedBranches the list of branches confirmed for processing
     * @param archive true if branches should be archived, false if they should be deleted
     */
    public OperationConfirmationResult(List<BranchModel> confirmedBranches, boolean archive) {
        this.confirmedBranches = confirmedBranches;
        this.archive = archive;
    }

    /**
     * Gets the list of branches confirmed for processing.
     *
     * @return the list of confirmed branches
     */
    public List<BranchModel> getConfirmedBranches() {
        return confirmedBranches;
    }

    /**
     * Checks if branches should be archived.
     *
     * @return true if branches should be archived, false if they should be deleted
     */
    public boolean isArchive() {
        return archive;
    }
}