package ir.pinbom.app.data.local

import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName="pins") data class PinEntity(@androidx.room.PrimaryKey val id:Long,val title:String,val description:String,val imageUrl:String,val width:Int,val height:Int,val authorName:String,val createdAt:String)
@Dao interface PinDao { @Query("SELECT * FROM pins ORDER BY id DESC") fun observe():Flow<List<PinEntity>>; @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsertAll(items:List<PinEntity>); @Query("DELETE FROM pins") suspend fun clear() }
@Database(entities=[PinEntity::class],version=1,exportSchema=false) abstract class LocalDb:androidx.room.RoomDatabase(){abstract fun pinDao():PinDao}
