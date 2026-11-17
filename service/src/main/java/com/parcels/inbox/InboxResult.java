package com.parcels.inbox;

/**
 * @param transactionCount number of transactions stored
 * @param duplicate        the same file content had already been imported, so nothing was stored
 */
public record InboxResult(int transactionCount, boolean duplicate) {

    public static InboxResult imported(int count) {
        return new InboxResult(count, false);
    }

    public static InboxResult alreadyImported() {
        return new InboxResult(0, true);
    }
}
