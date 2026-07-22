package com.orbys.launcherfakets13.domain.model

/**
 * Data class representing a recent application.
 *
 * @property packageName The package name of the application.
 * @property imagePath The path to the saved application icon.
 * @property name The display name (label) of the application.
 * @property taskId The unique identifier for the task in ActivityManager.
 */
data class RecentAppInfo(
    val packageName: String,
    val imagePath: String,
    val name: String,
    val taskId: Int
)
