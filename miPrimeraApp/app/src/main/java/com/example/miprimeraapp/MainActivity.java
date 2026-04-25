package com.example.miprimeraapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    DB db;
    Button btn;
    TextView tempval;
    String accion = "nuevo", idProducto = "", id = "", rev = "";
    String urlFoto1 = "", urlFoto2 = "", urlFoto3 = "";
    FloatingActionButton fab;

    // Array de imágenes para los 3 productos
    ImageView[] imgProductos = new ImageView[3];
    int imagenActual = 0;
    Intent tomarFotoIntento;

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_PICK_IMAGE = 2;

    detectarInternet di;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar variables
        db = new DB(this);
        di = new detectarInternet(this);

        // Inicializar las 3 imágenes
        imgProductos[0] = findViewById(R.id.imgFotoProducto1);
        imgProductos[1] = findViewById(R.id.imgFotoProducto2);
        imgProductos[2] = findViewById(R.id.imgFotoProducto3);

        // Asignar listener a cada imagen
        for (int i = 0; i < imgProductos.length; i++) {
            final int index = i;
            imgProductos[i].setOnClickListener(v -> {
                imagenActual = index;
                mostrarOpcionesImagen();
            });
        }

        // Botón guardar
        btn = findViewById(R.id.btnGuardarAmigo);
        btn.setOnClickListener(v -> guardarProducto());

        // FAB para regresar
        fab = findViewById(R.id.fabListaAmigo);
        fab.setOnClickListener(v -> regresarListaProductos());

        mostrarDatosProducto();

        // Manejar botón atrás
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                regresarListaProductos();
            }
        });
    }

    // ============================================
    // MOSTRAR OPCIONES DE IMAGEN
    // ============================================
    private void mostrarOpcionesImagen() {
        String[] opciones = {"📷 Tomar foto con cámara", "🖼️ Seleccionar de galería"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen " + (imagenActual + 1));
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                tomarFoto();
            } else {
                seleccionarDeGaleria();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    // ============================================
    // TOMAR FOTO CON CÁMARA
    // ============================================
    private void tomarFoto() {
        tomarFotoIntento = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File fotoProducto = null;

        try {
            fotoProducto = crearImgProducto();
            if (fotoProducto != null) {
                Uri uriFoto = FileProvider.getUriForFile(MainActivity.this,
                        "com.example.miprimeraapp.fileprovider", fotoProducto);
                tomarFotoIntento.putExtra(MediaStore.EXTRA_OUTPUT, uriFoto);
                startActivityForResult(tomarFotoIntento, REQUEST_TAKE_PHOTO);
            } else {
                mostrarMensaje("No se pudo crear la foto");
            }
        } catch (Exception e) {
            mostrarMensaje("Error al tomar la foto: " + e.getMessage());
        }
    }

    // ============================================
    // SELECCIONAR DE GALERÍA
    // ============================================
    private void seleccionarDeGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    // ============================================
    // CREAR ARCHIVO PARA LA FOTO
    // ============================================
    private File crearImgProducto() throws Exception {
        String fechaHoraMs = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "producto_" + fechaHoraMs + "_img" + imagenActual;
        File dirAlmacenamiento = getExternalFilesDir(Environment.DIRECTORY_DCIM);
        if (!dirAlmacenamiento.exists()) {
            dirAlmacenamiento.mkdir();
        }
        File image = File.createTempFile(fileName, ".jpg", dirAlmacenamiento);
        guardarUrlFoto(imagenActual, image.getAbsolutePath());
        return image;
    }

    // ============================================
    // GUARDAR IMAGEN DESDE GALERÍA
    // ============================================
    private String guardarImagenDesdeGaleria(Uri uri) {
        try {
            String fechaHoraMs = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "producto_galeria_" + fechaHoraMs + "_img" + imagenActual + ".jpg";
            File dirAlmacenamiento = getExternalFilesDir(Environment.DIRECTORY_DCIM);
            if (!dirAlmacenamiento.exists()) {
                dirAlmacenamiento.mkdir();
            }
            File imageFile = new File(dirAlmacenamiento, fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(imageFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return imageFile.getAbsolutePath();

        } catch (Exception e) {
            mostrarMensaje("Error al guardar imagen: " + e.getMessage());
            return "";
        }
    }

    // ============================================
    // RESULTADO DE ACTIVIDAD (FOTO O GALERÍA)
    // ============================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
                // Foto tomada con cámara
                String urlFoto = obtenerUrlFoto(imagenActual);
                if (!urlFoto.isEmpty()) {
                    imgProductos[imagenActual].setImageURI(Uri.parse(urlFoto));
                    mostrarMensaje("📷 Foto " + (imagenActual + 1) + " guardada");
                } else {
                    mostrarMensaje("Error: No se pudo obtener la URL de la foto");
                }
            } else if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
                // Imagen seleccionada de galería
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    String nuevaUrl = guardarImagenDesdeGaleria(selectedImageUri);
                    if (!nuevaUrl.isEmpty()) {
                        guardarUrlFoto(imagenActual, nuevaUrl);
                        imgProductos[imagenActual].setImageURI(Uri.parse(nuevaUrl));
                        mostrarMensaje("🖼️ Imagen " + (imagenActual + 1) + " seleccionada");
                    } else {
                        mostrarMensaje("Error al guardar la imagen");
                    }
                }
            }
        } catch (Exception e) {
            mostrarMensaje("Error: " + e.getMessage());
        }
    }

    // ============================================
    // MOSTRAR DATOS DEL PRODUCTO (MODIFICAR)
    // ============================================
    private void mostrarDatosProducto() {
        try {
            Bundle parametros = getIntent().getExtras();
            if (parametros != null) {
                accion = parametros.getString("accion");
                if (accion.equals("modificar")) {
                    JSONObject datos = new JSONObject(parametros.getString("producto"));

                    // Guardar _id y _rev de CouchDB

                    if (datos.has("_id") && datos.has("_rev")) {
                        id = datos.getString("_id");
                        rev = datos.getString("_rev");
                    }

                    idProducto = datos.getString("idProducto");

                    // Llenar campos
                    tempval = findViewById(R.id.txtNombreAmigos);
                    tempval.setText(datos.getString("nombre"));

                    tempval = findViewById(R.id.txtDireccionAmigos);
                    tempval.setText(datos.getString("descripcion"));

                    tempval = findViewById(R.id.txtTelefonoAmigos);
                    tempval.setText(String.valueOf(datos.getDouble("precio")));

                    tempval = findViewById(R.id.txtEmailAmigos);
                    tempval.setText(String.valueOf(datos.getInt("stock")));

                    tempval = findViewById(R.id.txtEmailAmigos);
                    tempval.setText(String.valueOf(datos.getInt("stock")));

                    tempval = findViewById(R.id.txtEmailAmigos2);
                    tempval.setText(String.valueOf(datos.getInt("costo")));

                    tempval = findViewById(R.id.txtDuiAmigos);
                    tempval.setText(datos.getString("categoria"));

                    // Cargar imágenes
                    cargarImagenesProducto(datos);
                } else {
                    limpiarCampos();
                }
            } else {
                limpiarCampos();
            }
        } catch (Exception e) {
            mostrarMensaje("Error al mostrar los datos: " + e.getMessage());
        }
    }

    // ============================================
    // CARGAR IMÁGENES DEL PRODUCTO
    // ============================================
    private void cargarImagenesProducto(JSONObject datos) {
        try {
            urlFoto1 = "";
            urlFoto2 = "";
            urlFoto3 = "";

            // Intentar cargar desde CouchDB primero
            boolean cargadoDesdeCouch = false;

            if (datos.has("imagenes")) {
                JSONArray imagenesArray = datos.getJSONArray("imagenes");

                if (imagenesArray.length() > 0) {
                    for (int i = 0; i < imagenesArray.length() && i < 3; i++) {
                        JSONObject imagen = imagenesArray.getJSONObject(i);
                        String url = imagen.getString("url");

                        if (url != null && !url.isEmpty()) {
                            switch (i) {
                                case 0:
                                    urlFoto1 = url;
                                    imgProductos[0].setImageURI(Uri.parse(url));
                                    break;
                                case 1:
                                    urlFoto2 = url;
                                    imgProductos[1].setImageURI(Uri.parse(url));
                                    break;
                                case 2:
                                    urlFoto3 = url;
                                    imgProductos[2].setImageURI(Uri.parse(url));
                                    break;
                            }
                            cargadoDesdeCouch = true;
                        }
                    }
                }
            }

            // Solo cargar desde SQLite si NO se cargaron desde CouchDB
            if (!cargadoDesdeCouch) {
                idProducto = datos.getString("idProducto");

                Cursor cImagenes = db.obtener_imagenes(idProducto);

                int contador = 0;
                while (cImagenes.moveToNext() && contador < 3) {
                    String urlFoto = cImagenes.getString(0);
                    int orden = cImagenes.getInt(1);

                    switch (orden) {
                        case 0:
                            urlFoto1 = urlFoto;
                            if (!urlFoto1.isEmpty()) {
                                imgProductos[0].setImageURI(Uri.parse(urlFoto1));
                            }
                            break;
                        case 1:
                            urlFoto2 = urlFoto;
                            if (!urlFoto2.isEmpty()) {
                                imgProductos[1].setImageURI(Uri.parse(urlFoto2));
                            }
                            break;
                        case 2:
                            urlFoto3 = urlFoto;
                            if (!urlFoto3.isEmpty()) {
                                imgProductos[2].setImageURI(Uri.parse(urlFoto3));
                            }
                            break;
                    }
                    contador++;
                }
                cImagenes.close();
            }

            // Mostrar imagen placeholder si no hay imagen
            if (urlFoto1.isEmpty()) {
                imgProductos[0].setImageResource(R.drawable.camara);
            }
            if (urlFoto2.isEmpty()) {
                imgProductos[1].setImageResource(R.drawable.camara);
            }
            if (urlFoto3.isEmpty()) {
                imgProductos[2].setImageResource(R.drawable.camara);
            }

        } catch (Exception e) {
            mostrarMensaje("Error al cargar imágenes: " + e.getMessage());
        }
    }

    // Metodo separado para carga de sqlite mejor aun
    private void cargarImagenesDesdeSQLite(JSONObject datos) {
        try {
            mostrarMensaje("Cargando imaganes desde Sqlite Local");

            idProducto = datos.getString("idProducto");

            Cursor cImagenes = db.obtener_imagenes(idProducto);

            int contador = 0;
            while (cImagenes.moveToNext() && contador < 3) {
                String urlFoto = cImagenes.getString(0);
                int orden = cImagenes.getInt(1);

                switch (orden) {
                    case 0:
                        urlFoto1 = urlFoto;
                        if (!urlFoto1.isEmpty()) {
                            imgProductos[0].setImageURI(Uri.parse(urlFoto1));
                        }
                        break;
                    case 1:
                        urlFoto2 = urlFoto;
                        if (!urlFoto2.isEmpty()) {
                            imgProductos[1].setImageURI(Uri.parse(urlFoto2));
                        }
                        break;
                    case 2:
                        urlFoto3 = urlFoto;
                        if (!urlFoto3.isEmpty()) {
                            imgProductos[2].setImageURI(Uri.parse(urlFoto3));
                        }
                        break;
                }
                contador++;
            }
            cImagenes.close();

        } catch (Exception e) {
            mostrarMensaje("Error al cargar imágenes desde SQLite: " + e.getMessage());
        }
    }

    // ============================================
    // LIMPIAR CAMPOS (NUEVO PRODUCTO)
    // ============================================
    private void limpiarCampos() {
        idProducto = "";
        urlFoto1 = "";
        urlFoto2 = "";
        urlFoto3 = "";

        TextView txtNombre = findViewById(R.id.txtNombreAmigos);
        txtNombre.setText("");

        TextView txtDireccion = findViewById(R.id.txtDireccionAmigos);
        txtDireccion.setText("");

        TextView txtTelefono = findViewById(R.id.txtTelefonoAmigos);
        txtTelefono.setText("");

        TextView txtEmail = findViewById(R.id.txtEmailAmigos);
        txtEmail.setText("");

        TextView txtEmail2 = findViewById(R.id.txtEmailAmigos2);
        txtEmail.setText("");

        TextView txtDui = findViewById(R.id.txtDuiAmigos);
        txtDui.setText("");

        for (int i = 0; i < imgProductos.length; i++) {
            imgProductos[i].setImageResource(R.drawable.camara);
        }
    }

    // ============================================
    // OBTENER URL DE FOTO POR ÍNDICE
    // ============================================
    private String obtenerUrlFoto(int index) {
        switch (index) {
            case 0:
                return urlFoto1;
            case 1:
                return urlFoto2;
            case 2:
                return urlFoto3;
            default:
                return "";
        }
    }

    // ============================================
    // GUARDAR URL DE FOTO POR ÍNDICE
    // ============================================
    private void guardarUrlFoto(int index, String url) {
        switch (index) {
            case 0:
                urlFoto1 = url;
                break;
            case 1:
                urlFoto2 = url;
                break;
            case 2:
                urlFoto3 = url;
                break;
        }
    }

    // ============================================
    // GUARDAR PRODUCTO
    // ============================================
    private void guardarProducto() {
        try {
            // Obtener valores de los campos
            tempval = findViewById(R.id.txtNombreAmigos);
            String nombre = tempval.getText().toString().trim();
            if (accion.equals("nuevo")) {
                idProducto = String.valueOf(System.currentTimeMillis());
            }

            tempval = findViewById(R.id.txtDireccionAmigos);
            String descripcion = tempval.getText().toString().trim();

            tempval = findViewById(R.id.txtTelefonoAmigos);
            String precio = tempval.getText().toString().trim();

            tempval = findViewById(R.id.txtEmailAmigos);
            String stock = tempval.getText().toString().trim();

            tempval = findViewById(R.id.txtEmailAmigos2);
            String costo = tempval.getText().toString().trim();

            tempval = findViewById(R.id.txtDuiAmigos);
            String categoria = tempval.getText().toString().trim();

            // ============ VALIDACIONES ============

            if (nombre.isEmpty()) {
                mostrarMensaje("Por favor ingrese el nombre del producto");
                findViewById(R.id.txtNombreAmigos).requestFocus();
                return;
            }

            if (descripcion.isEmpty()) {
                mostrarMensaje("Por favor ingrese la descripción del producto");
                findViewById(R.id.txtDireccionAmigos).requestFocus();
                return;
            }

            if (precio.isEmpty()) {
                mostrarMensaje("Por favor ingrese el precio del producto");
                findViewById(R.id.txtTelefonoAmigos).requestFocus();
                return;
            }

            double precioDouble;
            try {
                precioDouble = Double.parseDouble(precio);
                if (precioDouble <= 0) {
                    mostrarMensaje("El precio debe ser mayor a 0");
                    findViewById(R.id.txtTelefonoAmigos).requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarMensaje("El precio debe ser un número válido");
                findViewById(R.id.txtTelefonoAmigos).requestFocus();
                return;
            }

            if (stock.isEmpty()) {
                mostrarMensaje("Por favor ingrese el stock del producto");
                findViewById(R.id.txtEmailAmigos).requestFocus();
                return;
            }

            int stockInt;
            try {
                stockInt = Integer.parseInt(stock);
                if (stockInt < 0) {
                    mostrarMensaje("El stock no puede ser negativo");
                    findViewById(R.id.txtEmailAmigos).requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarMensaje("El stock debe ser un número entero válido");
                findViewById(R.id.txtEmailAmigos).requestFocus();
                return;
            }
            double costoDouble;
            try {
                costoDouble = Double.parseDouble(costo);
                if (costoDouble <= 0) {
                    mostrarMensaje("El costo debe ser mayor a 0");
                    findViewById(R.id.txtEmailAmigos2).requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarMensaje("El costo debe ser un número válido");
                findViewById(R.id.txtEmailAmigos2).requestFocus();
                return;
            }

            if (categoria.isEmpty()) {
                mostrarMensaje("Por favor ingrese la categoría del producto");
                findViewById(R.id.txtDuiAmigos).requestFocus();
                return;
            }

            int imagenesValidas = 0;
            if (urlFoto1 != null && !urlFoto1.isEmpty()) imagenesValidas++;
            if (urlFoto2 != null && !urlFoto2.isEmpty()) imagenesValidas++;
            if (urlFoto3 != null && !urlFoto3.isEmpty()) imagenesValidas++;

            if (imagenesValidas == 0) {
                mostrarMensaje("Por favor agregue al menos una imagen del producto");
                return;
            }

            // ============ GUARDAR EN BASE DE DATOS LOCAL ============
            String[] imagenes = {urlFoto1, urlFoto2, urlFoto3};
            String[] datos = {idProducto, nombre, descripcion, precio, stock,costo, categoria};

            String respuesta = db.administrar_productos(accion, datos, imagenes);

            if (!respuesta.equals("ok")) {
                mostrarMensaje("Error al guardar en local: " + respuesta);
                return;
            }

            // ============ GUARDAR EN COUCHDB ============
            // ============ GUARDAR EN COUCHDB ============
            JSONObject datosProducto = new JSONObject();

            if (accion.equals("modificar")) {
                datosProducto.put("_id", id);
                datosProducto.put("_rev", rev);
            }

            datosProducto.put("idProducto", idProducto);
            datosProducto.put("nombre", nombre);
            datosProducto.put("descripcion", descripcion);
            datosProducto.put("precio", precioDouble);
            datosProducto.put("stock", stockInt);
            datosProducto.put("costo", costoDouble);
            datosProducto.put("categoria", categoria);
            datosProducto.put("tipo", "producto");
            datosProducto.put("fecha_creacion", System.currentTimeMillis());

            // Crear array de imágenes
            JSONArray imagenesArray = new JSONArray();
            String[] urls = {urlFoto1, urlFoto2, urlFoto3};
            for (String url : urls) {
                if (url != null && !url.isEmpty()) {
                    JSONObject imagen = new JSONObject();
                    imagen.put("url", url);
                    imagen.put("fecha_agregada", System.currentTimeMillis());
                    imagenesArray.put(imagen);
                }
            }
            datosProducto.put("imagenes", imagenesArray);

            // 🔥 SI HAY INTERNET: Sincronizar
            if (di.hayConexionInternet()) {
                String metodo = accion.equals("modificar") ? "PUT" : "POST";
                String url = utilidades.url_mantenimiento;
                if (accion.equals("modificar")) {
                    url = utilidades.url_mantenimiento + "/" + id;
                }

                try {
                    enviarDatosServidor objEnviar = new enviarDatosServidor(this);
                    respuesta = objEnviar.execute(datosProducto.toString(), metodo, url).get();
                    JSONObject respuestaJSON = new JSONObject(respuesta);

                    if (respuestaJSON.getBoolean("ok")) {
                        id = respuestaJSON.getString("id");
                        rev = respuestaJSON.getString("rev");

                        datosProducto.put("_id", id);
                        datosProducto.put("_rev", rev);

                        mostrarMensaje("✅ Sincronizado con servidor");
                    } else {
                        // 🔥 Si falla, guardar como pendiente
                        guardarEnPendientes(idProducto, accion, datosProducto);
                        mostrarMensaje("⚠️ Se sincronizará después");
                    }
                } catch (Exception e) {
                    // 🔥 Si hay error, guardar como pendiente
                    guardarEnPendientes(idProducto, accion, datosProducto);
                    mostrarMensaje("⚠️ Se sincronizará cuando hay conexión");
                }
            } else {
                // 🔥 SIN INTERNET: Guardar como pendiente
                guardarEnPendientes(idProducto, accion, datosProducto);
                mostrarMensaje("📱 Guardado localmente. Se sincronizará cuando hay conexión");
            }

            regresarListaProductos();

        } catch (Exception e) {
            mostrarMensaje("Error al guardar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 🔥 GUARDAR EN PENDIENTES (SharedPreferences)
    private void guardarEnPendientes(String idProducto, String accion, JSONObject datos) {
        try {
            android.content.SharedPreferences sp = getSharedPreferences("pendientes", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = sp.edit();

            // Guardar los datos del producto
            editor.putString("pendiente_" + idProducto, datos.toString());
            // Guardar la acción (nuevo o modificar)
            editor.putString("accion_" + idProducto, accion);
            editor.apply();

            Log.d("PENDIENTE", "Guardado para producto: " + idProducto + " | Acción: " + accion);
        } catch (Exception e) {
            Log.e("PENDIENTE_ERROR", "Error guardando: " + e.getMessage());
        }
    }

    // ============================================
    // MOSTRAR MENSAJE
    // ============================================
    private void mostrarMensaje(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // ============================================
    // REGRESAR A LISTA
    // ============================================
    private void regresarListaProductos() {
        Intent intent = new Intent(this, lista_productos.class);
        startActivity(intent);
        finish();
    }
    // ============================================
    // CALCULAR GANANCIA (precio - costo)
    // ============================================
    private double calcularGanancia(String precioStr, String costoStr) {
        try {
            double precio = Double.parseDouble(precioStr);
            double costo  = Double.parseDouble(costoStr);

            if (precio <= 0 || costo <= 0) return -1;

            return precio - costo; // ganancia bruta
        } catch (NumberFormatException e) {
            return -1; // señal de error
        }
    }
}