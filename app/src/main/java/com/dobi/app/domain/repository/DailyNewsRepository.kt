package com.dobi.app.domain.repository

import com.dobi.app.domain.model.DailyNewsStory

interface DailyNewsRepository {
    suspend fun fetchDailyNews(): Result<List<DailyNewsStory>>
}
