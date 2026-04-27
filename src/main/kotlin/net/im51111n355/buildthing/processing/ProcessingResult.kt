package net.im51111n355.buildthing.processing

enum class ProcessingResult {
    NOT_MODIFIED,
    MODIFIED,
    DELETE;

    companion object {
        fun fromIsModified(v: Boolean) = if (v) MODIFIED else NOT_MODIFIED
        fun fromIsDeleted(v: Boolean) = if (v) DELETE else NOT_MODIFIED
    }
}