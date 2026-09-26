package com.skillmcp.mentor.backup

import com.skillmcp.mentor.data.db.ChatMessageEntity
import com.skillmcp.mentor.data.db.ProjectEntity
import com.skillmcp.mentor.data.db.SkillEntity
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupSnapshot(
    val version: Int = 1,
    val exportedAt: Long,
    val projects: List<ProjectEntity>,
    val messages: List<ChatMessageEntity>,
    val skills: List<SkillEntity>,
)
