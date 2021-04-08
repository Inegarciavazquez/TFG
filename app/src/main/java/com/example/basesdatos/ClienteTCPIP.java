package com.example.basesdatos;

import android.app.Activity;
import android.os.Handler;
import android.widget.Toast;
import java.net.InetSocketAddress;
import java.net.Socket;
//Hilo para implantar la comunicación TCP/IP
class ClienteTCPIP implements Runnable {
    private String direccionIP;
    private int puerto;
    private Handler handler = new Handler();
    private Activity actividad;
    private volatile boolean finalizar = false;
    public volatile int[] entradas = new int[4];
    public volatile double[] entradas_double = new double[4];
    // Constructor
    public ClienteTCPIP (Activity _actividad, String _direccionIP, int _puerto) {
        actividad = _actividad;
        direccionIP = _direccionIP;
        puerto = _puerto;
    }
    @Override
    public void run() {
        try {
            //se realiza la conexión con el autómata
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(direccionIP, puerto), 1000);
            while(! finalizar) {  // Repetir hasta que haya que finalizar ...
                // Espera bloqueado 0.1 segundos
                Thread.sleep(100);
                //se leen los datos recibidos
                ModbusTCP.lee(socket, 0, 4, entradas);
                for(int i=0;i<4;i++){
                    //se guardan los datos
                    entradas_double[i]=entradas[i]/5.0;
                }
            }
            //se cierra la conexión con el autómata
            socket.close();
        } catch (Exception e) {
            //se muestra un mensaje de error
            visualizaTostada(e.toString());
        }
    }
    private void visualizaTostada (final String mensaje) {
        //se visualiza una tostada en la actividad
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(actividad, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }
    void finaliza() {
        finalizar = true;
    }
}
