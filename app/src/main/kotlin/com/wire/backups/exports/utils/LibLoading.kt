package com.wire.backups.exports.utils

import com.goterl.lazysodium.utils.LibraryLoader

/**
 * Strategy used for loading Libsodium libraries.
 */
internal val LIBSODIUM_BINARIES_LOADING = LibraryLoader.Mode.PREFER_SYSTEM
