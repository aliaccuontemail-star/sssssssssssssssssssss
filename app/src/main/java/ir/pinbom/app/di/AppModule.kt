package ir.pinbom.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.pinbom.app.data.local.LocalDb
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule{
 @Provides @Singleton fun db(@ApplicationContext c:Context):LocalDb=Room.databaseBuilder(c,LocalDb::class.java,"pinbom-cache.db").build()
}
