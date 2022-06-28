package io.github.amame04.android.cnteditor

import androidx.room.*

class RoomController {
    @Entity
    data class Line(
        @PrimaryKey val index: Int,
        @ColumnInfo(name = "length") val length: Int,
        @ColumnInfo(name = "line") val line: String,
    )

    @Dao
    interface LinesDao {
        @Query("SELECT * FROM Line")
        fun getAll(): MutableList<Line>

        @Insert
        fun insert(line: Line)

        @Update
        fun update(line: Line)

        @Delete
        fun deleteLine(line: Line)
    }

    @Database(entities = [Line::class], version = 1, exportSchema = false)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun LinesDao(): LinesDao
    }
}