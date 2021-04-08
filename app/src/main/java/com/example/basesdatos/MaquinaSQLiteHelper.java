package com.example.basesdatos;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MaquinaSQLiteHelper extends SQLiteOpenHelper {
    //constructor
    public MaquinaSQLiteHelper(Context contexto, String nombre, SQLiteDatabase.CursorFactory factory, int version) {
        //se llama al constructor de la clase base
        super(contexto, nombre, factory, version);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        //se ejecutan las sentencias de creación de las tablas
        db.execSQL("CREATE TABLE tablaClusters (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre_archivo TEXT NOT NULL,nombre_cluster TEXT NOT NULL, descripcion TEXT, aviso INTEGER,textoaviso TEXT, medio TEXT, desviacion TEXT)");
        db.execSQL("CREATE TABLE tablaUltimoArchivo (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL)");
        db.execSQL("CREATE TABLE tablaArchivos (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, numclusters INTEGER, numatributos INTEGER, nombre_atributos TEXT,fecha TEXT, hora TEXT)");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int versionAnterior, int versionNueva) {
        if (versionAnterior != versionNueva) { //si es una nueva versión
            //se eliminan las tablas
            db.execSQL("DROP TABLE IF EXISTS tablaClusters");
            db.execSQL("DROP TABLE IF EXISTS tablaUltimoArchivo");
            db.execSQL("DROP TABLE IF EXISTS tablaArchivos");
            //se vuelven a crear las tablas
            onCreate(db);
        }
    }
}