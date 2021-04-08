package com.example.basesdatos;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.view.View;
import android.widget.TextView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Authenticator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.EditText;
import android.widget.Toast;

public class Main2Activity extends AppCompatActivity {
    //Declaración de variables
    String correo;
    String contrasena;
    String varios_correos;
    ConstraintLayout clayout;
    TextView texto_modoactual, texto_descripcionactual,texto_medio,texto_aviso,texto_cabecera_aviso, texto_archivo_actual,texto_cabecera;
    SQLiteDatabase baseDatos;
    ClienteTCPIP cliente;
    Thread hiloCliente;
    private Timer temporizador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //es necesario llamar al mismo método de la clase base
        super.onCreate(savedInstanceState);
        //esta actividad muestra el layout descrito en activity_main.xml
        setContentView(R.layout.activity_main);
        //se relacionan las variables con los elementos del layout
        texto_cabecera = (TextView) findViewById(R.id.textView9);
        texto_archivo_actual = (TextView) findViewById(R.id.texto_archivoactual_1);
        texto_modoactual = (TextView) findViewById(R.id.Texto_modoactual);
        texto_descripcionactual = (TextView) findViewById(R.id.Texto_descripcion);
        texto_medio = (TextView) findViewById(R.id.texto_mediox);
        texto_aviso = (TextView) findViewById(R.id.Texto_aviso);
        texto_cabecera_aviso = (TextView) findViewById(R.id.Texto_cabecera_aviso);
        clayout = (ConstraintLayout) findViewById(R.id.cLayout);
        texto_modoactual.setTextColor(ContextCompat.getColor(this,R.color.colorGris));

        baseDatos =(SQLiteDatabase) Singleton.busca("basedatos");
        //se indica en la cabecera el nombre del archivo con el que se están procesando los datos
        String ordenSQL = "SELECT nombre FROM tablaUltimoArchivo";
        Cursor cursor_nombre_archivo = baseDatos.rawQuery(ordenSQL, null);
        cursor_nombre_archivo.moveToFirst();
        String nombre_archivo =cursor_nombre_archivo.getString(0);
        texto_archivo_actual.setText("Datos procesados con la información del archivo '"+nombre_archivo+"'");
        //se inicializan las variables
        correo= "recogidadatosplanta@gmail.com";
        contrasena="tfgcontrasena";
        varios_correos="recogidadatosplanta@gmail.com,ines1junio97@gmail.com";
    }
    @Override
    protected void onResume() {
        super.onResume();
        baseDatos =(SQLiteDatabase) Singleton.busca("basedatos");
//////////////********DESCOMENTAR PARA APP DEFINITIVA******////////////////////////
        //se lanza el hilo
        cliente = new ClienteTCPIP(this, "192.168.0.203", 502);
        hiloCliente = new Thread(cliente);
        hiloCliente.start();
        //temporizacion
        temporizador = new Timer();
        temporizador.schedule(hiloAdicionalTemporizador, 200, 3000);
//////////////********DESCOMENTAR HASTA AQUI******////////////////////////
    }

    TimerTask hiloAdicionalTemporizador = new TimerTask() {
        @Override
        public void run() {
            runOnUiThread(actualizaInterfaz);
        }
    };

    private Runnable actualizaInterfaz = new Runnable() {
        @Override
        public void run() {
            actualizar_datos_pantalla();
        }
    };
//////////////********DESCOMENTAR PARA APP DEFINITIVA******////////////////////////
    @Override
    protected void onPause() {
        super.onPause();
        //se solicita al hilo que finalice
        cliente.finaliza();
        try {
            //se espera a que finalice
            hiloCliente.join();
        } catch (InterruptedException e) {
            Toast.makeText(this, "Error esperando finalización del cliente", Toast.LENGTH_LONG).show();
        }
    }
//////////////********DESCOMENTAR HASTA AQUI******////////////////////////
    public void BotonPantallaClick(View v) {
        Intent i = new Intent(this, Main3Activity.class);
        startActivity(i);
        Singleton.guarda("basedatos",baseDatos);
    }

    public void actualizar_datos_pantalla(){
        clayout.setBackgroundColor(ContextCompat.getColor(this,R.color.colorBlanco));
        //datos archivo
        String ordenSQL = "SELECT nombre FROM tablaUltimoArchivo";
        Cursor cursor_nombre_archivo = baseDatos.rawQuery(ordenSQL, null);
        cursor_nombre_archivo.moveToFirst();
        String nombre_archivo =cursor_nombre_archivo.getString(0);
        ordenSQL = "SELECT numatributos FROM tablaArchivos WHERE nombre ='"+ nombre_archivo+"' ";
        Cursor cursor_numatributos = baseDatos.rawQuery(ordenSQL, null);
        cursor_numatributos.moveToFirst();
        Integer numatributos =cursor_numatributos.getInt(0);
        ordenSQL = "SELECT nombre_atributos FROM tablaArchivos WHERE nombre ='"+ nombre_archivo+"' ";
        Cursor cursor_nombre_atributos = baseDatos.rawQuery(ordenSQL, null);
        cursor_nombre_atributos.moveToFirst();
        String nombre_atributos_juntos =cursor_nombre_atributos.getString(0);
        String[] nombres_atributos=nombre_atributos_juntos.split(",");
        //se guradan los datos de la planta
        double [] datos_actuales =cliente.entradas_double;
        //si el número de atributos recibidos es correcto...
        if(datos_actuales.length == numatributos){
            //se recoge la información del archivo con el que se están procesando los datos
            ordenSQL = "SELECT nombre_cluster FROM tablaClusters WHERE nombre_archivo='" + nombre_archivo + "' ";
            Cursor cursor_nombre_cluster = baseDatos.rawQuery(ordenSQL, null);
            ordenSQL = "SELECT descripcion FROM tablaClusters WHERE nombre_archivo='" + nombre_archivo + "' ";
            Cursor cursor_descripcion = baseDatos.rawQuery(ordenSQL, null);
            ordenSQL = "SELECT medio FROM tablaClusters WHERE nombre_archivo='" + nombre_archivo + "' ";
            Cursor cursor_medio = baseDatos.rawQuery(ordenSQL, null);
            ordenSQL = "SELECT desviacion FROM tablaClusters WHERE nombre_archivo='" + nombre_archivo + "' ";
            Cursor cursor_desviacion = baseDatos.rawQuery(ordenSQL, null);
            ordenSQL = "SELECT aviso FROM tablaClusters WHERE nombre_archivo='" + nombre_archivo + "' ";
            Cursor cursor_aviso = baseDatos.rawQuery(ordenSQL, null);
            ordenSQL = "SELECT textoaviso FROM tablaClusters WHERE nombre_archivo='" + nombre_archivo + "' ";
            Cursor cursor_textoaviso = baseDatos.rawQuery(ordenSQL, null);
            Boolean pertenece = false;
            Integer w = 0;
            cursor_descripcion.moveToFirst();
            cursor_medio.moveToFirst();
            cursor_aviso.moveToFirst();
            cursor_textoaviso.moveToFirst();
            cursor_desviacion.moveToFirst();
            //se busca el cluster al que pertenecen los datos
            if (cursor_nombre_cluster.moveToFirst()) {
                // Mientras haya más clusters y no se haya encontrado al que pertenecen los datos
                do {
                    String vmedios = cursor_medio.getString(0);
                    String[] medio_cluster = vmedios.split(",");
                    String vdesviaciones = cursor_desviacion.getString(0);
                    String[] desviaciones_cluster = vdesviaciones.split(",");
                    w = 0;
                    pertenece = true;
                    do {
                        //se calcula la desviacion entre el dato recibido y el valor medio del cluster
                        double desviacion_obtenida = desviacion(datos_actuales[w], Double.parseDouble(medio_cluster[w]));
                        //si  no se cumple la condición
                        if (desviacion_obtenida > Double.parseDouble(desviaciones_cluster[w]))
                            pertenece = false;
                        w++;
                    //mientras se cumpla la consición y no se hayan comprobado todos los atributos
                    } while (w < nombres_atributos.length & pertenece);
                    //si pertenece al cluster
                    if (pertenece){
                        //se muestra en pantalla la información
                        texto_modoactual.setText(cursor_nombre_cluster.getString(0));
                        texto_descripcionactual.setText(cursor_descripcion.getString(0));
                        String texto="";
                        for (int j = 0; j < nombres_atributos.length; j ++) {
                            texto = texto + nombres_atributos[j]+" = "+medio_cluster[j]+"\n";
                        }
                        texto_medio.setText(texto);
                        //se establecen los colores
                        texto_modoactual.setTextColor(ContextCompat.getColor(this,R.color.colorGris));
                        texto_medio.setTextColor(ContextCompat.getColor(this,R.color.colorGris));
                        texto_descripcionactual.setTextColor(ContextCompat.getColor(this,R.color.colorGris));
                        texto_archivo_actual.setTextColor(ContextCompat.getColor(this,R.color.colorGris));
                        texto_cabecera.setTextColor(ContextCompat.getColor(this,R.color.colorVerde));
                        //no se muestra el aviso
                        texto_cabecera_aviso.setVisibility(View.INVISIBLE);
                        texto_aviso.setVisibility(View.INVISIBLE);
                        //si se tiene que emitir aviso
                        if(cursor_aviso.getInt(0) == 1){
                            //se establecen los colores
                            texto_modoactual.setTextColor(ContextCompat.getColor(this,R.color.colorBlanco));
                            texto_medio.setTextColor(ContextCompat.getColor(this,R.color.colorBlanco));
                            texto_descripcionactual.setTextColor(ContextCompat.getColor(this,R.color.colorBlanco));
                            texto_aviso.setTextColor(ContextCompat.getColor(this,R.color.colorBlanco));
                            texto_archivo_actual.setTextColor(ContextCompat.getColor(this,R.color.colorBlanco));
                            texto_cabecera.setTextColor(ContextCompat.getColor(this,R.color.colorPrimaryDark));
                            clayout.setBackgroundColor(ContextCompat.getColor(this,R.color.colorRojo));
                            //se muestra el aviso
                            texto_cabecera_aviso.setVisibility(View.VISIBLE);
                            texto_aviso.setVisibility(View.VISIBLE);
                            String cuerpo=cursor_textoaviso.getString(0);
                            texto_aviso.setText(cuerpo);
                            cuerpo= cuerpo +".<br>El modo actual de funcionamiento es: \"" +cursor_nombre_cluster.getString(0)+"\", los datos de dicho modo son:<br>("+nombre_atributos_juntos+") = ("+cursor_medio.getString(0)+")";
                            enviar_mail(correo,contrasena,varios_correos,"AVISO modo de funcionamiento actual incorrecto",cuerpo);
                        }
                    }
                    cursor_descripcion.moveToNext();
                    cursor_medio.moveToNext();
                    cursor_aviso.moveToNext();
                    cursor_textoaviso.moveToNext();
                    cursor_desviacion.moveToNext();
                } while (cursor_nombre_cluster.moveToNext() & !pertenece);
            }
            if (!pertenece){//no se encontro cluster relacionado
                //Mensaje de error
                AlertDialog.Builder dialogo_aviso = new AlertDialog.Builder(Main2Activity.this);
                dialogo_aviso.setMessage("Los datos recibidos no se pueden relacionar con ningún modo de funcionamiento. Seleccione un archivo adecuado para procesar los datos.").setCancelable(false)
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //se cambia a la pantalla de inicio
                                cambiar_pantalla();
                                dialog.cancel();
                            }
                        });
                AlertDialog dialogo_titulo = dialogo_aviso.create();
                dialogo_titulo.setTitle("Error.");
                dialogo_titulo.show();
            }
        }else{//error en los datos recibidos o en la seleccion de archivo
            AlertDialog.Builder dialogo_aviso = new AlertDialog.Builder(Main2Activity.this);
            dialogo_aviso.setMessage("Los atributos recibidos no coinciden con los atributos del archivo de lectura. Seleccione un archivo adecuado para procesar los datos.").setCancelable(false)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //se cambia a la pantalla de inicio
                            cambiar_pantalla();
                            dialog.cancel();
                        }
                    });
            AlertDialog dialogo_titulo = dialogo_aviso.create();
            dialogo_titulo.setTitle("ERROR");
            dialogo_titulo.show();
        }
    }
    public double  desviacion(double valor1, double valor2){
        double desviacion;
        double media= (valor1 + valor2)/2;
        double a= Math.pow(valor1-media,2);
        double b= Math.pow(valor2-media,2);
        desviacion=Math.sqrt((a+b)/2);
        return(desviacion);
    }
    public void cambiar_pantalla(){
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
    private void enviar_mail(final String correo_procede, final String contrasena_envio, String correos_destino, String asunto, String cuerpo_correo){
        //se conecta conel servidor
        StrictMode.ThreadPolicy policy= new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Properties properties = new Properties();
        properties.put("mail.smtp.host","smtp.googlemail.com");
        properties.put("mail.smtp.socketFactory.port","465");
        properties.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.port","465");
        try{
            Session sesion=Session.getDefaultInstance( properties, new Authenticator(){
                @Override
                protected PasswordAuthentication getPasswordAuthentication(){
                    return new PasswordAuthentication(correo_procede, contrasena_envio);
                }
            });
            if (sesion!=null){
                //se envía el correo electrónico
                Message message = new MimeMessage(sesion);
                message.setFrom(new InternetAddress(correo_procede));
                message.setSubject(asunto);
                message.addRecipients(Message.RecipientType.CC, InternetAddress.parse(correos_destino));
                message.setContent(cuerpo_correo, "text/html; charset=utf-8");
                Transport.send(message);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void BotonXMLClick(View v) {
        //buleano para saber si la app tiene permiso de acceso al almacenamiento
        boolean permiso = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!permiso) {
            AlertDialog.Builder dialogo_aviso = new AlertDialog.Builder(Main2Activity.this);
            dialogo_aviso.setMessage("No se pudo guardar el archivo.\nEs necesario que conceda permiso a la aplicación para acceder al almacenamiento interno de su móvil.\nPuede hacer esto en ajustes < aplicaciones < permisos < memoria").setCancelable(false)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            AlertDialog dialogo_titulo = dialogo_aviso.create();
            dialogo_titulo.setTitle("Permiso denegado");
            dialogo_titulo.show();
        } else {//si la app SÍ tiene permiso de acceso al almacenamiento
            baseDatos = (SQLiteDatabase) Singleton.busca("basedatos");
            //objetos necesarios para manejar un archivo en formato XML
            DocumentBuilderFactory dbf;
            DocumentBuilder db;
            dbf = DocumentBuilderFactory.newInstance();
            try {
                //se recoge el nombre del archivo
                String ordenSQL = "SELECT nombre FROM tablaUltimoArchivo";
                Cursor cursor_nombre_archivo = baseDatos.rawQuery(ordenSQL, null);
                cursor_nombre_archivo.moveToFirst();
                String nombreArchivo = cursor_nombre_archivo.getString(0);
                //se consultan la fecha y hora anctuales
                Date date = new Date();
                SimpleDateFormat fecha= new SimpleDateFormat("d/MMMM/yyyy");
                String sfecha =fecha.format(date);
                SimpleDateFormat hora= new SimpleDateFormat("HH:mm");
                String shora =hora.format(date);
                //objetos necesarios para manejar un archivo en formato XML
                db = dbf.newDocumentBuilder();
                Document documento = db.newDocument();
                Element elementoRaiz = documento.createElement("Clusters_"+nombreArchivo);
                documento.appendChild(elementoRaiz);
                elementoRaiz.setAttribute("fecha", sfecha);
                elementoRaiz.setAttribute("hora", shora);
                //se guarda en cursores la información de los modos de funcionamiento
                ordenSQL = "SELECT nombre_cluster FROM tablaClusters WHERE nombre_archivo ='"+ nombreArchivo+"' ";
                Cursor cursor_nombre = baseDatos.rawQuery(ordenSQL, null);
                ordenSQL = "SELECT descripcion FROM tablaClusters WHERE nombre_archivo ='"+ nombreArchivo+"' ";
                Cursor cursor_descripcion = baseDatos.rawQuery(ordenSQL, null);
                ordenSQL = "SELECT medio FROM tablaClusters WHERE nombre_archivo ='"+ nombreArchivo+"' ";
                Cursor cursor_medio = baseDatos.rawQuery(ordenSQL, null);
                ordenSQL = "SELECT desviacion FROM tablaClusters WHERE nombre_archivo ='"+ nombreArchivo+"' ";
                Cursor cursor_desviacion = baseDatos.rawQuery(ordenSQL, null);
                ordenSQL = "SELECT aviso FROM tablaClusters WHERE nombre_archivo ='"+ nombreArchivo+"' ";
                Cursor cursor_aviso = baseDatos.rawQuery(ordenSQL, null);
                ordenSQL = "SELECT textoaviso FROM tablaClusters WHERE nombre_archivo ='"+ nombreArchivo+"' ";
                Cursor cursor_textoaviso = baseDatos.rawQuery(ordenSQL, null);
                ordenSQL = "SELECT numatributos FROM tablaArchivos WHERE nombre ='"+ nombreArchivo+"' ";
                Cursor cursor_numatributos = baseDatos.rawQuery(ordenSQL, null);
                cursor_numatributos.moveToFirst();
               Integer numero_atributos = cursor_numatributos.getInt(0);
                ordenSQL = "SELECT nombre_atributos FROM tablaArchivos WHERE nombre ='"+ nombreArchivo+"' ";
                Cursor cursor_nombre_atributos = baseDatos.rawQuery(ordenSQL, null);
                cursor_nombre_atributos.moveToFirst();
                String nombre_atributos_juntos = cursor_nombre_atributos.getString(0);
                String [] nombres_atributos = nombre_atributos_juntos.split(",");
                cursor_descripcion.moveToFirst();
                cursor_medio.moveToFirst();
                cursor_desviacion.moveToFirst();
                cursor_aviso.moveToFirst();
                cursor_textoaviso.moveToFirst();
                //se añade la información de los modos de funcionamiento
                if (cursor_nombre.moveToFirst()) {
                    do {
                        Element cluster = documento.createElement("cluster");
                        elementoRaiz.appendChild(cluster);

                        Element nomb = documento.createElement("nombre");
                        nomb.appendChild(documento.createTextNode(cursor_nombre.getString(0)));
                        cluster.appendChild(nomb);

                        Element desc = documento.createElement("descripcion");
                        desc.appendChild(documento.createTextNode(cursor_descripcion.getString(0)));
                        cluster.appendChild(desc);

                        String[] vmedio = cursor_medio.getString(0).split(",");
                        Element valores_medios = documento.createElement("valores_medios");
                        cluster.appendChild(valores_medios);
                        for (int a =0 ; a < numero_atributos ; a++) {
                        valores_medios.setAttribute(nombres_atributos[a], vmedio[a]);
                        }
                        String[] vdesviacion = cursor_desviacion.getString(0).split(",");
                        Element valores_desviaciones = documento.createElement("valores_desviaciones");
                        cluster.appendChild(valores_desviaciones);
                        for (int a =0 ; a < numero_atributos ; a++) {
                            valores_desviaciones.setAttribute(nombres_atributos[a], vdesviacion[a]);
                        }
                        String avis = "NO";
                        if (cursor_aviso.getString(0).equals("1")) {
                            avis = "SI";
                        }
                        Element aviso = documento.createElement("aviso");
                        cluster.appendChild(aviso);
                        aviso.setAttribute("Emitir_aviso", avis);
                        if (cursor_aviso.getString(0).equals("1")) {
                            aviso.setAttribute("Texto_aviso", cursor_textoaviso.getString(0));
                        }
                        cursor_descripcion.moveToNext();
                        cursor_medio.moveToNext();
                        cursor_desviacion.moveToNext();
                        cursor_aviso.moveToNext();
                        cursor_textoaviso.moveToNext();
                    } while (cursor_nombre.moveToNext());
                }
                //se guarda el archivo
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                DOMSource fuente = new DOMSource(documento);
                File tarjeta = Environment.getExternalStorageDirectory();
                File file = new File(tarjeta.getAbsolutePath(), "clusters_"+nombreArchivo+".xml");
                StreamResult resultado = new StreamResult(file);
                transformer.transform(fuente, resultado);
                //se muestra mensaje de guardado
                Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                System.out.println("Error" + e.getMessage());
                System.exit(0);
            } catch (TransformerException tfe) {
                System.out.println("Error" + tfe.getMessage());
                System.exit(0);
            }
        }
    }
}
