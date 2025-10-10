package com.example.financialapp

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val month: Int,
    val bManual: Boolean
)

@Entity(
    tableName = "salary_items",
    foreignKeys = [ForeignKey(
        entity = SettingsEntity::class,
        parentColumns = ["id"],
        childColumns = ["settingsId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("settingsId")]
)
data class SalaryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val settingsId: Int,
    val type: String,
    val amount: Double,
    val isTaxable: Boolean,
    val isInsured: Boolean
)

@Entity(
    tableName = "insurance_items",
    foreignKeys = [ForeignKey(
        entity = SettingsEntity::class,
        parentColumns = ["id"],
        childColumns = ["settingsId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("settingsId")]
)
data class InsuranceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val settingsId: Int,
    val type: String,
    val value: String
)

data class SettingsWithItems(
    @Embedded val settings: SettingsEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "settingsId"
    ) val salaryList: List<SalaryItemEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "settingsId"
    ) val insuranceList: List<InsuranceItemEntity>
)

@Dao
interface SettingsDao {
    @Transaction
    @Query("SELECT * FROM settings WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getDataByYearMonth(year: Int, month: Int): SettingsWithItems?

    @Transaction
    @Query("SELECT * FROM settings WHERE year = :year")
    suspend fun getDataByYear(year: Int): List<SettingsWithItems>

    @Transaction
    @Query("SELECT * FROM settings")
    suspend fun getAllData(): List<SettingsWithItems>

    @Insert
    suspend fun insertSettings(settings: SettingsEntity): Long

    @Insert
    suspend fun insertSalaryItems(items: List<SalaryItemEntity>)

    @Insert
    suspend fun insertInsuranceItems(items: List<InsuranceItemEntity>)

    @Update
    suspend fun updateSettings(settings: SettingsEntity)

    @Query("DELETE FROM salary_items WHERE settingsId = :settingsId")
    suspend fun deleteSalaryBySettings(settingsId: Int)

    @Query("DELETE FROM insurance_items WHERE settingsId = :settingsId")
    suspend fun deleteInsuranceBySettings(settingsId: Int)
}

@Database(
    entities = [SettingsEntity::class, SalaryItemEntity::class, InsuranceItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}
