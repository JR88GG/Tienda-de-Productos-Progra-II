package com.example.miprimeraapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

// Constructor de la base de datos
public class DB extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tienda";
    private static final int DATABASE_VERSION = 1;

    // Tabla de productos
    private static final String SQL_PRODUCTOS = "CREATE TABLE productos (" +
            "idProducto INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "nombre TEXT, " +
            "descripcion TEXT, " +
            "precio REAL, " +
            "stock INTEGER, " +
            "costo REAL,"+
            "categoria TEXT)";

    // Tabla de imágenes (relación muchos a uno con productos)
    private static final String SQL_IMAGENES = "CREATE TABLE imagenes (" +
            "idImagen INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "idProducto INTEGER, " +
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

    // Administrar productos
    public String administrar_productos(String accion, String[] datos, String[] imagenes) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            String mensaje = "ok", sql = "";

            switch (accion) {
                case "nuevo":
                    sql = "INSERT INTO productos(idProducto, nombre, descripcion, precio, stock, categoria) VALUES(" +
                            "'" + datos[0] + "'," +      // 🔥 datos[0] es idProducto
                            "'" + datos[1] + "'," +
                            "'" + datos[2] + "'," +
                            datos[3] + "," +
                            datos[4] + "," +
                            datos[5] + "," +
                            "'" + datos[6] + "'" +
                            ")";
                    db.execSQL(sql);

                    // Obtener el ID del producto recién insertado
                    Cursor c = db.rawQuery("SELECT last_insert_rowid()", null);
                    c.moveToFirst();
                    String idProducto = c.getString(0);
                    c.close();

                    // Guardar las imágenes
                    guardarImagenes(db, idProducto, imagenes);
                    break;

                case "modificar":
                    sql = "UPDATE productos SET " +
                            "nombre='" + datos[1] + "'," +
                            "descripcion='" + datos[2] + "'," +
                            "precio=" + datos[3] + "," +
                            "stock=" + datos[4] + "," +
                            "costo=" + datos[5] + "," +
                            "categoria='" + datos[6] + "' " +
                            "WHERE idProducto='" + datos[0] + "'";
                    db.execSQL(sql);

                    // Eliminar imágenes antiguas y guardar las nuevas
                    db.execSQL("DELETE FROM imagenes WHERE idProducto='" + datos[0] + "'");
                    guardarImagenes(db, datos[0], imagenes);
                    break;

                case "eliminar":
                    sql = "DELETE FROM productos WHERE idProducto='" + datos[0] + "'";
                    db.execSQL(sql);
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

    // Obtener lista de productos
    public Cursor lista_productos() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM productos ORDER BY nombre", null);
    }

    // Obtener imágenes de un producto específico
    public Cursor obtener_imagenes(String idProducto) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT urlFoto, orden FROM imagenes WHERE idProducto='" + idProducto + "' ORDER BY orden", null);
    }
}