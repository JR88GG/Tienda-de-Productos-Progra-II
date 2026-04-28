package com.example.miprimeraapp;

import java.util.ArrayList;

public class Producto {
    String idProducto;
    String nombre;
    String descripcion;
    double precio;
    int stock;
    double costo;
    double ganancia;   // ← NUEVO
    double margen_pct; // ← NUEVO
    String categoria;
    ArrayList<String> imagenes;

    public Producto(String idProducto, String nombre, String descripcion,
                    double precio, int stock, double costo,
                    double ganancia, double margen_pct,  // ← NUEVO
                    String categoria, ArrayList<String> imagenes) {
        this.idProducto  = idProducto;
        this.nombre      = nombre;
        this.descripcion = descripcion;
        this.precio      = precio;
        this.stock       = stock;
        this.costo       = costo;
        this.ganancia    = ganancia;
        this.margen_pct  = margen_pct;
        this.categoria   = categoria;
        this.imagenes    = imagenes;
    }

    // Getters y Setters
    public String getIdProducto() { return idProducto; }
    public void setIdProducto(String idProducto) { this.idProducto = idProducto; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public double getCosto() { return costo; }
    public void setCosto(double costo) { this.costo = costo; }

    public double getGanancia() { return ganancia; }       // ← NUEVO
    public void setGanancia(double ganancia) { this.ganancia = ganancia; }

    public double getMargenPct() { return margen_pct; }    // ← NUEVO
    public void setMargenPct(double margen_pct) { this.margen_pct = margen_pct; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public ArrayList<String> getImagenes() { return imagenes; }
    public void setImagenes(ArrayList<String> imagenes) { this.imagenes = imagenes; }

    // Metodo para obtener la primera imagen (principal)
    public String getImagenPrincipal() {
        if (imagenes != null && !imagenes.isEmpty()) {
            return imagenes.get(0);
        }
        return null;
    }
}