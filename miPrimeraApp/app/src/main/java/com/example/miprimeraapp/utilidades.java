package com.example.miprimeraapp;

import java.util.Base64;

public class utilidades {
    static String url_consulta = "http://192.168.1.5:5984/herber/_design/adonay/_view/adonay";

    // "http://10.148.226.151:5984/tienda_producto/_design/tienda_producto/_view/tienda_producto"
    static String url_mantenimiento = "http://192.168.1.5:5984/herber"; // CRUD Insertar, Actualizar, Borrar y Buscar
    //http://10.148.226.151:5984/tienda_producto
    static String user = "herber88";
    static String passwd = "61513673";
    static String credencialesCodificadas = Base64.getEncoder().encodeToString((user + ":" + passwd).getBytes());

    public String generarUnicoId() {
        return java.util.UUID.randomUUID().toString();
    }
}

