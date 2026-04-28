package com.example.miprimeraapp;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class obtenerDatosServidor extends AsyncTask<String, String, String> {
    HttpURLConnection httpURLConnection;

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }

    @Override
    protected String doInBackground(String... strings) {
        StringBuilder respuesta = new StringBuilder();
        try {
            URL url = new URL(utilidades.url_consulta);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setConnectTimeout(5000); // ← timeout 5 seg
            httpURLConnection.setReadTimeout(5000);
            httpURLConnection.setRequestProperty("Authorization",
                    "Basic " + utilidades.credencialesCodificadas);
            httpURLConnection.setRequestProperty("Accept", "application/json");

            int responseCode = httpURLConnection.getResponseCode();
            Log.d("HTTP_GET", "Código respuesta: " + responseCode + " | URL: " + utilidades.url_consulta);

            InputStream inputStream;

            if (responseCode >= 200 && responseCode < 300) {
                // Respuesta exitosa
                inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
            } else {
                // Error del servidor — leer errorStream para el log
                inputStream = httpURLConnection.getErrorStream();
                if (inputStream != null) {
                    BufferedReader errReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder errBody = new StringBuilder();
                    String linea;
                    while ((linea = errReader.readLine()) != null) errBody.append(linea);
                    Log.e("HTTP_GET_ERROR", "Código " + responseCode + " | Body: " + errBody);
                }
                // Retornar JSON de error para que lista_productos lo maneje
                return "{\"error\":\"http_" + responseCode + "\",\"rows\":[]}";
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String linea;
            while ((linea = bufferedReader.readLine()) != null) {
                respuesta.append(linea);
            }

        } catch (java.net.ConnectException e) {
            Log.e("HTTP_GET_ERROR", "No se pudo conectar: " + e.getMessage());
            return "{\"error\":\"connection_refused\",\"rows\":[]}";
        } catch (java.net.SocketTimeoutException e) {
            Log.e("HTTP_GET_ERROR", "Timeout: " + e.getMessage());
            return "{\"error\":\"timeout\",\"rows\":[]}";
        } catch (Exception e) {
            Log.e("HTTP_GET_ERROR", "Error: " + e.getMessage());
            return "{\"error\":\"" + e.getMessage() + "\",\"rows\":[]}";
        } finally {
            if (httpURLConnection != null) httpURLConnection.disconnect();
        }
        return respuesta.toString();
    }
}