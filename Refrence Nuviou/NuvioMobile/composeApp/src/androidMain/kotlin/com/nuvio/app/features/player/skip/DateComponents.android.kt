package com.nuvio.app.features.player.skip

import java.util.Calendar

internal actual fun currentDateComponents(): DateComponents {
    val cal = Calendar.getInstance()
    return DateComponents(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1,
        day = cal.get(Calendar.DAY_OF_MONTH),
    )
}
