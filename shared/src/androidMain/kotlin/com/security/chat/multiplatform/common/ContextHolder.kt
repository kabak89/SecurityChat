package com.security.chat.multiplatform.common

import android.content.Context

object ContextHolder {

    private var context: Context? = null

    fun setContext(context: Context) {
        this.context = context.applicationContext
    }

    fun getContext(): Context {
        return context!!
    }

}