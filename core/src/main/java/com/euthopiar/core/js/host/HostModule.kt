package com.euthopiar.core.js.host

import com.dokar.quickjs.QuickJs

interface HostModule {
    fun register(runtime: QuickJs)
}

class HostApiRegistry {
    private val modules = mutableListOf<HostModule>()

    fun addModule(module: HostModule) {
        modules.add(module)
    }

    fun registerAll(runtime: QuickJs) {
        modules.forEach { it.register(runtime) }
    }
}
