package com.security.chat.multiplatform.common.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface ScopedModel {
    fun start(parentScope: CoroutineScope?): Job
}