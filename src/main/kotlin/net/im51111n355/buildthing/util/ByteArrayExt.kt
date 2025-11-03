package net.im51111n355.buildthing.util

import java.security.MessageDigest

fun ByteArray.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(this)

    return digest
        .digest()
        .joinToString("") { "%02x".format(it) }
}