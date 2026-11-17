package com.parcels.inbox;

import java.nio.file.Path;

/**
 * Turns one kind of inbox file into transactions. Add an implementation to support a new kind of file
 * (for example receipt photos); {@link InboxProcessor} picks the first handler that supports a file.
 */
public interface InboxFileHandler {

    boolean supports(Path file);

    /** Short description of the accepted files, used in the error message for unsupported files. */
    String description();

    /**
     * Imports the file. Must be all-or-nothing: either every entry is stored or none is.
     *
     * @throws InboxFileException when the content is invalid; its errors are written next to the failed file
     */
    InboxResult process(Path file);
}
