package com.security.chat.multiplatform.common.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

public interface ScopedModel {
    public fun start(parentScope: CoroutineScope?): Job
}