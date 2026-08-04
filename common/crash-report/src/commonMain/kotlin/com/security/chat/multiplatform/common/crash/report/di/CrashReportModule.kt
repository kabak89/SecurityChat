package com.security.chat.multiplatform.common.crash.report.di

import com.security.chat.multiplatform.common.crash.report.CrashReporter
import com.security.chat.multiplatform.common.crash.report.platformCrashReporter
import org.koin.core.module.Module
import org.koin.dsl.module

public val crashReportModule: Module =
    module {
        single<CrashReporter> { platformCrashReporter }
    }
