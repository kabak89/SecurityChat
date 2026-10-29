# App UI tests

Run all UI scenarios in the project:

```bash
./gradlew uiTest
```

For a single module or scenario, use `:shared:uiTest` or
`:shared:uiTest --tests '*AppStartupTest'`.

UI scenario classes must have `@Tag("ui")`. A module containing them applies
`securitychat.convention.ui-test` in addition to `securitychat.convention.base`. The task uses the
existing `jvmTest` source set and dependencies. Normal `jvmTest` tasks exclude the `ui` tag;
`uiTest` includes it and excludes `screenshot`.

Run unit tests with `./gradlew jvmTest` and screenshot tests with `./gradlew checkScreenshots`. UI
test reports are written to `shared/build/reports/tests/uiTest` and
`shared/build/test-results/uiTest`.

The tests launch `RootContent` offscreen with in-memory settings and a mocked HTTP engine.

## Video recording

Install FFmpeg with the `libx264` encoder and make `ffmpeg` available on `PATH`. If needed, set
`FFMPEG_PATH` to the absolute path of the executable before running Gradle or the IDE.

Pass a unique `videoName` to `runAppUiTest` to record a scenario:

```kotlin
runAppUiTest(
    testModules = listOf(successfulSignInModule()),
    videoName = "sign-in-onboarding",
) {
    signIn()
}
```

Videos are written to `shared/build/reports/ui-tests/<videoName>.mp4` at 25 fps. Encoder diagnostics
are saved next to each video in `<videoName>-ffmpeg.log`. The last line of each test's output
contains a `Video: file:///...mp4` link after recording and app cleanup finish, including when the
scenario fails. Gradle forwards only these video links to the console; other test output stays in
the test reports. The encoder is finalized on both success and failure, so completed frames remain
available when an assertion fails. Omitting `videoName` disables recording and does not require
FFmpeg. Each write and finalization has a 20-second timeout; a stalled encoder is forcibly
terminated. The artifact directory is declared as a `uiTest` output, so Gradle tracks missing files
and restores videos together with cached test results.

Capture opportunities are polled on Compose's scheduler, but the video timeline follows elapsed real
time from `System.nanoTime()`. Extra polls within the same 40 ms time slot are skipped before image
capture. Gaps are filled with the previous frame so waits keep their real duration at 25 fps. The
final state replaces the pending frame in the last time slot, including when the scenario fails.
Duration is rounded to a video frame, with at most 40 ms of padding. FFmpeg startup, initial image
capture, final encoding and app cleanup are outside this timeline.

Frames are streamed as PNGs into FFmpeg; there is no OS window, screen recording permission or
temporary frame directory. There are no artificial pauses or final-frame holds. Fast test actions
and animations advanced by virtual time can therefore appear fast in the video. If final capture
also fails, its error is suppressed on the original scenario failure. Instant actions such as
`performScrollTo()` appear as jumps. Use touch gestures when a scenario needs to show the scrolling
motion.
