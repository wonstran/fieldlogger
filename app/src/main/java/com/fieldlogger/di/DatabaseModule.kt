package com.fieldlogger.di

import android.content.Context
import androidx.room.Room
import com.fieldlogger.data.local.FieldLoggerDatabase
import com.fieldlogger.data.local.dao.EventButtonDao
import com.fieldlogger.data.local.dao.EventDao
import com.fieldlogger.data.repository.FieldLoggerRepositoryImpl
import com.fieldlogger.domain.repository.FieldLoggerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FieldLoggerDatabase {
        return Room.databaseBuilder(
            context,
            FieldLoggerDatabase::class.java,
            "fieldlogger_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideEventDao(database: FieldLoggerDatabase): EventDao {
        return database.eventDao()
    }

    @Provides
    @Singleton
    fun provideEventButtonDao(database: FieldLoggerDatabase): EventButtonDao {
        return database.eventButtonDao()
    }

    @Provides
    @Singleton
    fun provideRepository(
        eventDao: EventDao,
        eventButtonDao: EventButtonDao
    ): FieldLoggerRepository {
        return FieldLoggerRepositoryImpl(eventDao, eventButtonDao)
    }
}
