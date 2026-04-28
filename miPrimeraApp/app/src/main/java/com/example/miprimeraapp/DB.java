package com.example.miprimeraapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

// Constructor de la base de datos
public class DB extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tienda";
    private static final int DATABASE_VERSION = 2; // ← subido de 1 a 2 para forzar onUpgrade

    // Tabla de productos
    private static final String SQL_PRODUCTOS = "CREATE TABLE productos (" +
            "idProducto TEXT PRIMARY KEY, " +
            "nombre TEXT, " +
            "descripcion TEXT, " +
            "precio REAL, " +
            "stock INTEGER, " +
            "costo REAL, " +
            "ganancia REAL, " +       // ← NUEVO
            "margen_pct REAL, " +     // ← NUEVO
            "categoria TEXT)";

    // Tabla de imágenes (relación muchos a uno con productos)
    private static final String SQL_IMAGENES = "CREATE TABLE imagenes (" +
            "idImagen INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "idProducto TEXT, " +
            "urlFoto TEXT, " +
            "orden INTEGER, " +
            "FOREIGN KEY(idProducto) REFERENCES productos(idProducto) ON DELETE CASCADE)";

    public DB(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_PRODUCTOS);
        sqLiteDatabase.execSQL(SQL_IMAGENES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS imagenes");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS productos");
        onCreate(sqLiteDatabase);
    }

    // ============================================
    // ADMINISTRAR PRODUCTOS
    // Array datos[]:
    //   [0] idProducto
    //   [1] nombre
    //   [2] descripcion
    //   [3] precio
    //   [4] stock
    //   [5] costo
    //   [6] categoria
    //   [7] ganancia
    //   [8] margen_pct
    // ============================================
    public String administrar_productos(String accion, String[] datos, String[] imagenes) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("PRAGMA foreign_keys = ON");
            String mensaje = "ok";

            switch (accion) {
                case "nuevo":
                    String sqlInsert = "INSERT INTO productos(" +
                            "idProducto, nombre, descripcion, precio, stock, costo, ganancia, margen_pct, categoria" +
                            ") VALUES(" +
                            "'" + datos[0] + "'," +   // idProducto
                            "'" + datos[1] + "'," +   // nombre
                            "'" + datos[2] + "'," +   // descripcion
                            datos[3] + "," +           // precio
                            datos[4] + "," +           // stock
                            datos[5] + "," +           // costo
                            datos[7] + "," +           // ganancia
                            datos[8] + "," +           // margen_pct
                            "'" + datos[6] + "'" +    // categoria
                            ")";
                    db.execSQL(sqlInsert);
                    guardarImagenes(db, datos[0], imagenes); // usar idProducto directo
                    break;

                case "modificar":
                    String sqlUpdate = "UPDATE productos SET " +
                            "nombre='" + datos[1] + "'," +
                            "descripcion='" + datos[2] + "'," +
                            "precio=" + datos[3] + "," +
                            "stock=" + datos[4] + "," +
                            "costo=" + datos[5] + "," +
                            "ganancia=" + datos[7] + "," +
                            "margen_pct=" + datos[8] + "," +
                            "categoria='" + datos[6] + "' " +
                            "WHERE idProducto='" + datos[0] + "'";
                    db.execSQL(sqlUpdate);
                    db.execSQL("DELETE FROM imagenes WHERE idProducto='" + datos[0] + "'");
                    guardarImagenes(db, datos[0], imagenes);
                    break;

                case "eliminar":
                    db.execSQL("DELETE FROM productos WHERE idProducto='" + datos[0] + "'");
                    // Las imágenes se eliminan automáticamente por ON DELETE CASCADE
                    break;
            }

            db.close();
            return mensaje;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private void guardarImagenes(SQLiteDatabase db, String idProducto, String[] imagenes) {
        for (int i = 0; i < imagenes.length; i++) {
            if (imagenes[i] != null && !imagenes[i].isEmpty()) {
                String sqlImg = "INSERT INTO imagenes(idProducto, urlFoto, orden) VALUES(" +
                        "'" + idProducto + "'," +
                        "'" + imagenes[i] + "'," +
                        i + ")";
                db.execSQL(sqlImg);
            }
        }
    }

    // ============================================
    // OBTENER LISTA DE PRODUCTOS
    // Columnas: 0=idProducto, 1=nombre, 2=descripcion,
    //           3=precio, 4=stock, 5=costo,
    //           6=ganancia, 7=margen_pct, 8=categoria
    // ============================================
    public Cursor lista_productos() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT idProducto, nombre, descripcion, precio, stock, costo, ganancia, margen_pct, categoria FROM productos ORDER BY nombre", null);
    }

    // Obtener imágenes de un producto específico
    public Cursor obtener_imagenes(String idProducto) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT urlFoto, orden FROM imagenes WHERE idProducto='" + idProducto + "' ORDER BY orden", null);
    }
}