package com.example.financialapp

import android.content.Context
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
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update

@Entity(tableName = "FinancialDataTable")
data class FinancialDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val month: Int,
)

@Entity(
    tableName = "SalaryItemTable",
    foreignKeys = [ForeignKey(// 使用外键，和 FinancialDataTable 建立联系，因为这张表作为它的子表
        entity = FinancialDataEntity::class,
        parentColumns = ["id"],// 告诉 Room 外键要连到父表的“id”列
        childColumns = ["financialDataTableId"],// 告诉 Room 子表用哪一列来存这个外键
        onDelete = ForeignKey.CASCADE// 如果父表的某行被删除，则自动删除所有引用它的子表行（级联删除）
    )],
    indices = [Index("financialDataTableId")]
)
data class SalaryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val financialDataTableId: Int,
    val type: String,
    val amount: Double,
    val isTaxable: Boolean,
)

@Entity(
    tableName = "InsuranceItemTable",
    foreignKeys = [ForeignKey(
        entity = FinancialDataEntity::class,
        parentColumns = ["id"],
        childColumns = ["financialDataTableId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("financialDataTableId")]
)
data class InsuranceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val financialDataTableId: Int,
    val type: String,
    val value: Double
)

// Room 中的组合关系类（relationship data class），它的作用是把多张表的关联数据“一次性加载”成一个 Kotlin 对象
// 这个类本身不是表（Entity），而是组合视图（关系映射类），Room 会把这个类当作一个“查询结果结构”
data class QueryDataContainer(
    @Embedded val financialDataEntity: FinancialDataEntity,// 把 FinancialDataEntity 的字段“内嵌”进这个类中，作为主表数据部分
    @Relation(
        parentColumn = "id",// 当前 FinancialDataEntity 的 id 字段
        entityColumn = "financialDataTableId"// 对应 SalaryItemEntity 的 financialDataTableId 字段，
    ) val salaryList: List<SalaryItemEntity>,// 并且一个父对象可以对应多个子对象（List）
    @Relation(
        parentColumn = "id",
        entityColumn = "financialDataTableId"
    ) val insuranceList: List<InsuranceItemEntity>
)

@Dao
interface FinancialDataDao {
    @Transaction// 用于确保数据库操作的原子性
    @Query("SELECT * FROM FinancialDataTable WHERE year = :year AND month = :month LIMIT 1")
    // suspend 表示这是一个挂起函数，只能在协程或其他挂起函数中调用，不会阻塞线程，可以暂停和恢复，适合用于IO操作
    suspend fun getDataByYearMonth(year: Int, month: Int): QueryDataContainer?

    @Transaction
    @Query("SELECT * FROM FinancialDataTable WHERE year = :year")
    suspend fun getDataByYear(year: Int): List<QueryDataContainer>

    @Transaction
    @Query("SELECT * FROM FinancialDataTable")
    suspend fun getAllData(): List<QueryDataContainer>

    @Insert
    suspend fun insertData(financialDataEntity: FinancialDataEntity): Long// 返回插入行的ID，如果主键冲突会抛出异常

    @Insert
    suspend fun insertSalaryItems(items: List<SalaryItemEntity>)

    @Insert
    suspend fun insertInsuranceItems(items: List<InsuranceItemEntity>)

    @Update// 用于更新已存在的数据
    suspend fun updateData(financialDataEntity: FinancialDataEntity)

    @Query("DELETE FROM SalaryItemTable WHERE financialDataTableId = :financialDataTableId")
    suspend fun deleteSalaryByDataId(financialDataTableId: Int)

    @Query("DELETE FROM InsuranceItemTable WHERE financialDataTableId = :financialDataTableId")
    suspend fun deleteInsuranceByDataId(financialDataTableId: Int)
}

@Database(
    entities = [FinancialDataEntity::class, SalaryItemEntity::class, InsuranceItemEntity::class],
    version = 1,
    exportSchema = false// 不在编译时自动生成数据库的结构描述文件
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financialDataDao(): FinancialDataDao

    companion object {
        @Volatile  // 1. 确保多线程可见性：主线程和工作线程都能看到最新的 INSTANCE 值
        private var INSTANCE: AppDatabase? = null  // 2. 静态变量，整个应用生命周期只存在一份

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {  // 3. 同步锁：防止多线程同时创建实例
                val instance = Room.databaseBuilder(
                    context.applicationContext,  // 4. 使用 Application Context（避免内存泄漏）
                    AppDatabase::class.java,
                    "FinancialApp.db"
                ).build()
                INSTANCE = instance  // 5. 保存唯一实例
                instance
            }
        }
    }
}
