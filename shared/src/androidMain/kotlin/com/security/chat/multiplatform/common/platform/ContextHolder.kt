package com.security.chat.multiplatform.common.platform

import android.content.Context

public object ContextHolder {

    private var context: Context? = null

    public fun setContext(context: Context) {
        this.context = context.applicationContext
    }

    public fun getContext(): Context {
        return context!!
    }

}