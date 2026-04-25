package com.example.miprimeraapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class lista_productos extends AppCompatActivity {

    Bundle parametros = new Bundle();
    DB db;
    FloatingActionButton fab;
    ListView ltsProductos;
    Cursor cProductos;
    final ArrayList<Producto> alProductos = new ArrayList<>();
    final ArrayList<Producto> alProductosCopia = new ArrayList<>();
    JSONArray jsonArray;
    JSONObject jsonObject;
    int posicion = 0;
    Producto misProducto;
    detectarInternet di;
    obtenerDatosServidor datosServidor;
    Producto productoSeleccionado;  // ← Agrega esta línea

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_amigos);

        parametros.putString("accion", "nuevo");
        db = new DB(this);

        fab = findViewById(R.id.fabAgregarAmigos);
        fab.setOnClickListener(v -> abrirActivity());

        di = new detectarInternet(this);

        // 🔥 SINCRONIZAR PENDIENTES AL ABRIR
        if (di.hayConexionInternet()) {
            sincronizarPendientes();
            sincronizarEliminacionesPendientes();
        }

        obtenerProductos();
        buscarProductos();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mimenu, menu);

        try {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            posicion = info.position; // 🔥 IMPORTANTE: guardar la posición
            Producto productoSeleccionado = alProductos.get(posicion);
            menu.setHeaderTitle(productoSeleccionado.getNombre());

        } catch (Exception e) {
            mostrarMsg("Error al mostrar menú: " + e.getMessage());
        }
    }

// BUG SOLUCIONADO A LA HORA DE FILTRAR

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        try {
            Producto productoSeleccionado = alProductos.get(posicion);

            if (item.getItemId() == R.id.mnxAgregar) {
                parametros.putString("accion", "nuevo");
                abrirActivity();

            } else if (item.getItemId() == R.id.mnxModificar) {
                parametros.putString("accion", "modificar");

                // Convertir el producto seleccionado a JSON
                JSONObject jsonProducto = new JSONObject();
                jsonProducto.put("idProducto", productoSeleccionado.getIdProducto());
                jsonProducto.put("nombre", productoSeleccionado.getNombre());
                jsonProducto.put("descripcion", productoSeleccionado.getDescripcion());
                jsonProducto.put("precio", productoSeleccionado.getPrecio());
                jsonProducto.put("stock", productoSeleccionado.getStock());
                jsonProducto.put("costo", productoSeleccionado.getCosto());
                jsonProducto.put("categoria", productoSeleccionado.getCategoria());

                // 🔥 AGREGAR: Buscar _id y _rev para CouchDB
                try {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject temp = jsonArray.getJSONObject(i);
                        JSONObject docOriginal;

                        // Verificar si es CouchDB (tiene "value") o local
                        if (temp.has("value")) {
                            docOriginal = temp.getJSONObject("value");
                        } else {
                            docOriginal = temp;
                        }

                        // Comparar por idProducto
                        if (docOriginal.getString("idProducto").equals(productoSeleccionado.getIdProducto())) {

                            if (docOriginal.has("_id")) {
                                jsonProducto.put("_id", docOriginal.getString("_id"));
                                jsonProducto.put("_rev", docOriginal.getString("_rev"));
                            }

                            // 🔥 FIX IMÁGENES
                            if (docOriginal.has("imagenes")) {
                                jsonProducto.put("imagenes", docOriginal.getJSONArray("imagenes"));
                            }

                            break;
                        }
                    }
                } catch (Exception e) {
                    // Si no encuentra _id y _rev, solo es producto local, no hay problema
                    Log.d("DEBUG", "Producto local sin _id/_rev");
                }

                parametros.putString("producto", jsonProducto.toString());
                abrirActivity();

            } else if (item.getItemId() == R.id.mnxEliminar) {
                borrarProducto();
            }

            return true;

        } catch (Exception e) {
            mostrarMsg("Error al seleccionar item de menú: " + e.getMessage());
            return super.onContextItemSelected(item);
        }
    }

    private JSONObject getDoc(int pos) throws Exception {
        try {
            return jsonArray.getJSONObject(pos).getJSONObject("value"); // online
        } catch (Exception e) {
            return jsonArray.getJSONObject(pos); // offline
        }
    }

    private void borrarProducto() {
        try {
            JSONObject doc = getDoc(posicion);
            String nombre = doc.getString("nombre");
            String idProducto = doc.getString("idProducto");

            AlertDialog.Builder confirmacion = new AlertDialog.Builder(this);
            confirmacion.setTitle("¿Está seguro de borrar?");
            confirmacion.setMessage(nombre);

            confirmacion.setPositiveButton("SÍ", (dialog, which) -> {
                try {

                    // 🔥 eliminar local
                    String respuesta = db.administrar_productos("eliminar",
                            new String[]{idProducto},
                            new String[]{});

                    if (!respuesta.equals("ok")) {
                        mostrarMsg("Error al eliminar localmente");
                        return;
                    }

                    // 🔥 eliminar servidor SOLO si existe _id y _rev
                    if (di.hayConexionInternet() && doc.has("_id") && doc.has("_rev")) {
                        // Si hay internet, intenta eliminar del servidor
                        String _id = doc.getString("_id");
                        String _rev = doc.getString("_rev");
                        String url = utilidades.url_mantenimiento + "/" + _id + "?rev=" + _rev;

                        try {
                            enviarDatosServidor obj = new enviarDatosServidor(this);
                            String resp = obj.execute("", "DELETE", url).get();
                            JSONObject respuestaJSON = new JSONObject(resp);

                            if (!respuestaJSON.getBoolean("ok")) {
                                mostrarMsg("Error servidor: " + resp);
                            } else {
                                mostrarMsg("Producto eliminado correctamente");
                            }
                        } catch (Exception e) {
                            // Si falla la sincronización, guardar como pendiente
                            guardarEliminacionPendiente(idProducto, _id, _rev);
                            mostrarMsg("⚠️ Se eliminará del servidor cuando haya conexión");
                        }
                    } else if (!di.hayConexionInternet() && doc.has("_id") && doc.has("_rev")) {
                        // 🔥 SIN INTERNET: Guardar eliminación como pendiente
                        String _id = doc.getString("_id");
                        String _rev = doc.getString("_rev");
                        guardarEliminacionPendiente(idProducto, _id, _rev);
                        mostrarMsg("📱 Eliminado localmente. Se sincronizará cuando hay conexión");
                    } else {
                        // Es un producto solo local, sin _id en servidor
                        mostrarMsg("Producto eliminado correctamente");
                    }

                    obtenerProductos();

                } catch (Exception e) {
                    mostrarMsg(e.getMessage());
                }
            });

            confirmacion.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
            confirmacion.create().show();

        } catch (Exception e) {
            mostrarMsg("Error al borrar: " + e.getMessage());
        }
    }

    // 🔥 NUEVA FUNCIÓN: Guardar eliminación como pendiente
    private void guardarEliminacionPendiente(String idProducto, String _id, String _rev) {
        try {
            android.content.SharedPreferences sp = getSharedPreferences("pendientes_eliminacion", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = sp.edit();

            editor.putString("eliminar_" + idProducto, _id);
            editor.putString("eliminar_rev_" + idProducto, _rev);
            editor.apply();

            Log.d("ELIMINAR", "Guardado para eliminar: " + idProducto + " | _id: " + _id);
        } catch (Exception e) {
            Log.e("ELIMINAR_ERROR", "Error guardando eliminación: " + e.getMessage());
        }
    }

    // 🔥 SINCRONIZAR ELIMINACIONES PENDIENTES
    private void sincronizarEliminacionesPendientes() {
        if (!di.hayConexionInternet()) {
            Log.d("SYNC_DELETE", "Sin conexión a internet");
            return;
        }

        android.content.SharedPreferences sp = getSharedPreferences("pendientes_eliminacion", MODE_PRIVATE);
        java.util.Map<String, ?> pendientes = sp.getAll();

        int eliminados = 0;
        int errores = 0;

        for (String key : pendientes.keySet()) {
            if (key.startsWith("eliminar_") && !key.endsWith("_rev")) {
                String idProducto = key.replace("eliminar_", "");
                String _id = (String) pendientes.get(key);
                String _rev = sp.getString("eliminar_rev_" + idProducto, "");

                try {
                    String url = utilidades.url_mantenimiento + "/" + _id + "?rev=" + _rev;

                    Log.d("SYNC_DELETE", "Eliminando: " + idProducto + " | _id: " + _id);

                    enviarDatosServidor obj = new enviarDatosServidor(this);
                    String resp = obj.execute("", "DELETE", url).get();
                    JSONObject respuestaJSON = new JSONObject(resp);

                    if (respuestaJSON.getBoolean("ok")) {
                        // Eliminar de pendientes
                        android.content.SharedPreferences.Editor editor = sp.edit();
                        editor.remove(key);
                        editor.remove("eliminar_rev_" + idProducto);
                        editor.apply();

                        eliminados++;
                        Log.d("SYNC_DELETE", "✅ Eliminado: " + idProducto);
                    } else {
                        errores++;
                        Log.e("SYNC_DELETE_ERROR", "Error: " + resp);
                    }
                } catch (Exception e) {
                    errores++;
                    Log.e("SYNC_DELETE_ERROR", "Exception: " + e.getMessage());
                }
            }
        }

        if (eliminados > 0 || errores > 0) {
            Log.d("SYNC_DELETE", "Resumen: " + eliminados + " eliminados, " + errores + " errores");
        }
    }

    private void buscarProductos() {
        TextView tempVal = findViewById(R.id.txtBuscarAmigos);
        tempVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                alProductos.clear();
                String buscar = tempVal.getText().toString().trim().toLowerCase();
                if (buscar.length() <= 0) {
                    alProductos.addAll(alProductosCopia);
                } else {
                    for (Producto item : alProductosCopia) {
                        if (item.getNombre().toLowerCase().contains(buscar) ||
                                item.getCategoria().toLowerCase().contains(buscar)) {
                            alProductos.add(item);
                        }
                    }
                    ltsProductos.setAdapter(new AdaptadorProductos(getApplicationContext(), alProductos));
                }
            }
        });
    }

    private void abrirActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtras(parametros);
        startActivity(intent);
    }

    private void obtenerProductos() {
        try {
            if (di.hayConexionInternet()) { //si hay conexion a internet
                datosServidor = new obtenerDatosServidor();
                String respuesta = datosServidor.execute().get();
                jsonObject = new JSONObject(respuesta);
                jsonArray = jsonObject.getJSONArray("rows");
                mostrarProductos();
            } else { //no hay conexion a internet
                cProductos = db.lista_productos();
                if (cProductos.moveToFirst()) {
                    jsonArray = new JSONArray();
                    do {
                        jsonObject = new JSONObject();
                        jsonObject.put("idProducto", cProductos.getString(0));
                        jsonObject.put("nombre", cProductos.getString(1));
                        jsonObject.put("descripcion", cProductos.getString(2));
                        jsonObject.put("precio", cProductos.getDouble(3));
                        jsonObject.put("stock", cProductos.getInt(4));
                        jsonObject.put("categoria", cProductos.getString(5));
                        jsonArray.put(jsonObject);
                    } while (cProductos.moveToNext());
                    mostrarProductos();
                } else {
                    mostrarMsg("No hay productos que mostrar");
                    abrirActivity();
                }
            }
        } catch (Exception e) {
            mostrarMsg(e.getMessage());
        }
    }

    private void mostrarProductos() {
        try {
            if (jsonArray.length() > 0) {
                ltsProductos = findViewById(R.id.ltsAmigos);
                alProductos.clear();
                alProductosCopia.clear();

                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject doc;
                    try {
                        doc = jsonArray.getJSONObject(i).getJSONObject("value"); // online
                    } catch (Exception e) {
                        doc = jsonArray.getJSONObject(i); // offline
                    }

                    ArrayList<String> imagenes = new ArrayList<>();

                    // ========= SI VIENE DE COUCHDB =========
                    if (doc.has("imagenes")) {
                        JSONArray imgs = doc.getJSONArray("imagenes");

                        for (int j = 0; j < imgs.length(); j++) {
                            JSONObject img = imgs.getJSONObject(j);
                            String url = img.getString("url");

                            if (!url.isEmpty()) {
                                imagenes.add(url);
                            }
                        }
                    }

                    // ========= SI NO HAY EN COUCHDB, BUSCAR LOCAL =========
                    if (imagenes.isEmpty()) {
                        Cursor cImagenes = db.obtener_imagenes(doc.getString("idProducto"));

                        while (cImagenes.moveToNext()) {
                            imagenes.add(cImagenes.getString(0));
                        }

                        cImagenes.close();
                    }

                    Producto producto = new Producto(
                            doc.getString("idProducto"),
                            doc.getString("nombre"),
                            doc.getString("descripcion"),
                            doc.getDouble("precio"),
                            doc.getInt("stock"),
                            doc.getDouble("costo"),
                            doc.getString("categoria"),
                            imagenes
                    );

                    alProductos.add(producto);
                }

                alProductosCopia.addAll(alProductos);
                ltsProductos.setAdapter(new AdaptadorProductos(this, alProductos));
                registerForContextMenu(ltsProductos);

            } else {
                mostrarMsg("No hay productos que mostrar");
                abrirActivity();
            }

        } catch (Exception e) {
            mostrarMsg(e.getMessage());
        }
    }
    private void mostrarMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
    // 🔥 SINCRONIZAR PENDIENTES
    private void sincronizarPendientes() {
        if (!di.hayConexionInternet()) {
            Log.d("SYNC", "Sin conexión a internet");
            return;
        }

        android.content.SharedPreferences sp = getSharedPreferences("pendientes", MODE_PRIVATE);
        java.util.Map<String, ?> pendientes = sp.getAll();

        int sincronizados = 0;
        int errores = 0;

        for (String key : pendientes.keySet()) {
            if (key.startsWith("pendiente_")) {
                String idProducto = key.replace("pendiente_", "");
                String datos = (String) pendientes.get(key);
                String accion = sp.getString("accion_" + idProducto, "nuevo");

                try {
                    JSONObject datosJSON = new JSONObject(datos);

                    String metodo = "POST";  // Por defecto: crear nuevo
                    String url = utilidades.url_mantenimiento;

                    // 🔥 SI YA TIENE _id, USAR PUT (actualizar)
                    if (datosJSON.has("_id") && !datosJSON.getString("_id").isEmpty()) {
                        String _id = datosJSON.getString("_id");
                        String _rev = datosJSON.getString("_rev");

                        url = utilidades.url_mantenimiento + "/" + _id;
                        metodo = "PUT";

                        Log.d("SYNC", "Actualizando: " + idProducto + " | Método: PUT | _id: " + _id);
                    } else {
                        Log.d("SYNC", "Creando: " + idProducto + " | Método: POST");
                    }

                    // Enviar a servidor
                    enviarDatosServidor objEnviar = new enviarDatosServidor(this);
                    String respuesta = objEnviar.execute(datosJSON.toString(), metodo, url).get();
                    JSONObject resp = new JSONObject(respuesta);

                    if (resp.getBoolean("ok")) {
                        // 🔥 SI FUE POST (crear), guardar el _id retornado
                        if (metodo.equals("POST")) {
                            String nuevoId = resp.getString("id");
                            String nuevoRev = resp.getString("rev");

                            // Actualizar el JSON con el nuevo _id y _rev
                            datosJSON.put("_id", nuevoId);
                            datosJSON.put("_rev", nuevoRev);

                            // Guardar de nuevo para próximas modificaciones
                            android.content.SharedPreferences.Editor editor = sp.edit();
                            editor.putString("pendiente_" + idProducto, datosJSON.toString());
                            editor.apply();

                            Log.d("SYNC", "Obtenido _id: " + nuevoId + " | _rev: " + nuevoRev);
                        }

                        // Eliminar de pendientes
                        android.content.SharedPreferences.Editor editor = sp.edit();
                        editor.remove(key);
                        editor.remove("accion_" + idProducto);
                        editor.apply();

                        sincronizados++;
                        Log.d("SYNC", "✅ Sincronizado: " + idProducto);
                        obtenerProductos();
                    } else {
                        errores++;
                        Log.e("SYNC_ERROR", "Error en servidor para " + idProducto + ": " + respuesta);
                    }
                } catch (Exception e) {
                    errores++;
                    Log.e("SYNC_ERROR", "Exception sincronizando " + idProducto + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        if (sincronizados > 0 || errores > 0) {
            Log.d("SYNC", "Resumen: " + sincronizados + " sincronizados, " + errores + " errores");
        }
    }
}