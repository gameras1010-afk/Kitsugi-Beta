package com.lagradost.cloudstream3.actions

abstract class VideoClickAction {
    var sourcePlugin: String? = null
    open val name: Any = ""
}

object VideoClickActionHolder {
    val allVideoClickActions = ArrayList<VideoClickAction>()
}
