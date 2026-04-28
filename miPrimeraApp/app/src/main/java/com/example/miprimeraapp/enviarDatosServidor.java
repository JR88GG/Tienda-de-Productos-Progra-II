package com.example.miprimeraapp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

public class enviarDatosServidor extends AsyncTask<String, String, String> {
    Context context;
    HttpURLConnection httpURLConnection;

    public enviarDatosServidor(Context context) {
        this.context = context;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }

    @Override
    protected String doInBackground(String... parametros) {
        String jsonResponse = "";
        String jsonDatos = parametros[0];
        String metodo = parametros[1];
        String _url = parametros[2];

        BufferedReader bufferedReader = null;

        try {
            URL url = new URL(_url);
            httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod(metodo);
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Accept", "application/json");
            httpURLConnection.setRequestProperty("Authorization", "Basic " + utilidades.credencialesCodificadas);

            // 🔥 Enviar datos
            Writer writer = new BufferedWriter(
                    new OutputStreamWriter(httpURLConnection.getOutputStream(), "UTF-8")
            );
            writer.write(jsonDatos);
            writer.flush();
            writer.close();

            // 🔥 Leer respuesta correctamente
            InputStream inputStream;

            int responseCode = httpURLConnection.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                inputStream = httpURLConnection.getInputStream(); // OK
            } else {
                inputStream = httpURLConnection.getErrorStream(); // ERROR
            }

            if (inputStream == null) return null;

            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder stringBuilder = new StringBuilder();
            String linea;

            while ((linea = bufferedReader.readLine()) != null) {
                stringBuilder.append(linea);
            }

            if (stringBuilder.length() <= 0) return null;

            jsonResponse = stringBuilder.toString();

            // 🔍 LOG PARA DEBUG
            Log.d("RESPUESTA_SERVIDOR", jsonResponse);

        } catch (Exception e) {
            Log.e("ERROR_ENVIO", e.getMessage());
            return e.getMessage();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }

        return jsonResponse;
    }
}