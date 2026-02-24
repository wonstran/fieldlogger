package com.fieldlogger.di

import android.content.Context
import com.fieldlogger.util.CsvExporter
import com.fieldlogger.util.LocationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLocationHelper(@ApplicationContext context: Context): LocationHelper {
        return LocationHelper(context)
    }

    @Provides
    @Singleton
    fun provideCsvExporter(@ApplicationContext context: Context): CsvExporter {
        return CsvExporter(context)
    }
}
