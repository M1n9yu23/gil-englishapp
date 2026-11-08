/*
 * Copyright 2026 MinGyu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gyugle.gil.feature.listening.impl.data.playback

/** What a controller connecting to the listening session may do. */
internal enum class ControllerAccess {
    /** The app itself: it loads playlists. */
    FULL,

    /** The media notification, the lock screen, headphones and the car: play, pause, skip, stop. */
    CONTROL,
    REJECTED
}

/**
 * The service is exported so the system can control playback, which also lets any app try to
 * connect. Only the app and controllers the system trusts get in: the system and apps the user
 * allowed to control media (`MediaSession.ControllerInfo.isTrusted`), and the session's own
 * notification controller.
 */
internal fun controllerAccess(
    packageName: String,
    ownPackageName: String,
    trusted: Boolean,
    notificationController: Boolean
): ControllerAccess = when {
    // The notification controller runs in the app's own process, so it is checked first.
    notificationController -> ControllerAccess.CONTROL

    packageName == ownPackageName && trusted -> ControllerAccess.FULL

    trusted -> ControllerAccess.CONTROL

    else -> ControllerAccess.REJECTED
}
