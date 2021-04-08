package com.example.basesdatos;
import java.util.Hashtable;
public class Singleton {
    private static Hashtable<String, Object> datos = new Hashtable<String, Object>();
    // Los datos se guardan en este diccionario privado.
    public static void guarda(String s, Object o) {
        //si el dato ya existe lo borro
        if (datos.containsKey(s))
            datos.remove(s);
        //guarda el objeto o dato
        datos.put(s, o);
    }
    public static Object busca(String s) {
        //localiza en el diccionario el valor u objeto y lo devuelve
        return datos.get(s);
    }
}
