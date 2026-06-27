package com.example.luapp.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "luapp.db", null, 6) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE cash_registers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                opened_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
                closed_at INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE buddies (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE consumptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cash_register_id INTEGER NOT NULL REFERENCES cash_registers(id),
                concept TEXT NOT NULL,
                customer_name TEXT,
                buddy_id INTEGER REFERENCES buddies(id) ON DELETE SET NULL,
                amount REAL NOT NULL DEFAULT 0,      -- monto efectivo
                amount_qr REAL NOT NULL DEFAULT 0,
                appointment_fee REAL NOT NULL DEFAULT 0,
                pending_amount REAL,
                details TEXT,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
            )
        """)

        db.execSQL("""
            CREATE TABLE consumption_buddies (
                consumption_id INTEGER NOT NULL REFERENCES consumptions(id) ON DELETE CASCADE,
                buddy_id INTEGER NOT NULL REFERENCES buddies(id) ON DELETE CASCADE,
                PRIMARY KEY (consumption_id, buddy_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cash_register_id INTEGER NOT NULL REFERENCES cash_registers(id),
                concept TEXT NOT NULL,
                amount REAL NOT NULL,
                details TEXT,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            db.execSQL("DROP TABLE IF EXISTS cash_close_expense")
            db.execSQL("DROP TABLE IF EXISTS cash_close_consumption")
            db.execSQL("DROP TABLE IF EXISTS cash_closes")
            db.execSQL("DROP TABLE IF EXISTS expenses")
            db.execSQL("DROP TABLE IF EXISTS consumptions")
            db.execSQL("DROP TABLE IF EXISTS buddies")
            db.execSQL("DROP TABLE IF EXISTS cash_registers")
            onCreate(db)
            return
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE consumptions ADD COLUMN amount_qr REAL NOT NULL DEFAULT 0")
        }
        if (oldVersion < 6) {
            db.execSQL("""
                CREATE TABLE consumption_buddies (
                    consumption_id INTEGER NOT NULL REFERENCES consumptions(id) ON DELETE CASCADE,
                    buddy_id INTEGER NOT NULL REFERENCES buddies(id) ON DELETE CASCADE,
                    PRIMARY KEY (consumption_id, buddy_id)
                )
            """)
            // Migrate existing single-buddy relationships
            db.execSQL("""
                INSERT OR IGNORE INTO consumption_buddies (consumption_id, buddy_id)
                SELECT id, buddy_id FROM consumptions WHERE buddy_id IS NOT NULL
            """)
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    fun getActiveCashRegisterId(): Long? {
        val cursor = readableDatabase.rawQuery(
            "SELECT id FROM cash_registers WHERE closed_at IS NULL LIMIT 1", null
        )
        val id = if (cursor.moveToFirst()) cursor.getLong(0) else null
        cursor.close()
        return id
    }

    fun getOrCreateActiveCashRegisterId(): Long {
        return getActiveCashRegisterId() ?: run {
            val values = ContentValues().apply { put("opened_at", System.currentTimeMillis()) }
            writableDatabase.insert("cash_registers", null, values)
        }
    }

    fun closeCashRegister(id: Long) {
        val values = ContentValues().apply { put("closed_at", System.currentTimeMillis()) }
        writableDatabase.update("cash_registers", values, "id = ?", arrayOf(id.toString()))
    }

    companion object {
        @Volatile private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
    }
}
