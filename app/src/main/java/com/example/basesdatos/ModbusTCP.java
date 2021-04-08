package com.example.basesdatos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
public class ModbusTCP {
    public static void lee (Socket s, int primero, int cuantos, int[] valores)
            throws Exception  {
        //Se obtienen referencias a los stream de entrada y salida correspondientes a esta conexión de red
        InputStream is = s.getInputStream();
        OutputStream os = s.getOutputStream();
        //Paquete que se envía al servidor
        byte[] paqueteLectura = new byte[12];
        //Número de transacción.No se va a utilizar.
        paqueteLectura[0] = 0;
        paqueteLectura[1] = 0;
        //Los siguientes bytes tienen que estar a 0
        paqueteLectura[2] = 0;
        paqueteLectura[3] = 0;
        paqueteLectura[4] = 0;
        //Número de bytes que siguen
        paqueteLectura[5] = 6;
        //Dirección Modbus que no se utiliza en Modbus/TCP
        paqueteLectura[6] = 1;
        //Código de operación de lectura
        paqueteLectura[7] = 0x3;
        //Dirección del primer registro a leer
        paqueteLectura[8] = (byte)(primero >> 8);
        paqueteLectura[9] = (byte)(primero & 0xFF);
        //Número de registros a leer
        paqueteLectura[10] = (byte)(cuantos >> 8);
        paqueteLectura[11] = (byte)(cuantos & 0xFF);
        //se envía la orden al servidor
        os.write(paqueteLectura);
        //variables
        int nBytesARecibir = 9 + cuantos * 2;
        byte[] respuesta = new byte[nBytesARecibir];
        int nBytesRecibidos = 0;
        do {//se recoge la respuesta del esclavo hasta completarla
            nBytesRecibidos += is.read(respuesta, nBytesRecibidos,
                    nBytesARecibir - nBytesRecibidos);
        } while (nBytesRecibidos < nBytesARecibir);
        //Si el esclavo contesta con un código de error...
        if ((respuesta[7] & 0x80) == 0x80)
            //se genera una excepción y se muestra el código de error en hexadecimal
            throw new IOException("Código de error enviado por el esclavo: 0x" +
                    Integer.toHexString(respuesta[7]));
        // Recoge en la matriz 'valores' los datos enviados por el esclavo en su respuesta
        for (int i = 0; i < cuantos; i++)
            valores[i] = (respuesta[9 + i * 2] << 8) +
                    ( ((int)respuesta[9 + i * 2 + 1]) & 0x00FF );
    }
}
