package com.example.basesdatos;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class MainActivity extends AppCompatActivity {
    //se declaran variables y referencias
    MaquinaSQLiteHelper helper;
    SQLiteDatabase baseDatos;

    EditText nombre_archivo;
    Button boton_relacionar, boton_abrir, boton_NOrelacionar, boton_continuar, boton_cambiar,boton_seleccionar_archivo_anterior;
    TextView texto_relacionar, texto_archivoactual,texto_buscararchivo,texto_xml,texto_archivo_anterior;
    Spinner desplegable_relacionar,desplegable_archivo_anterior;
    ProgressDialog progressDialog;

    Integer num_definitivo_clusters, num_cluster_calculado,numero_clusters;
    String nombre,nombre_atributos,seleccionado,seleccionado_archivo_anterior;
    String[] nombres_atributos;
    double[][][] grupos;
    ArrayList<String> opciones,opciones_archivo_anterior;

    EM_modificada agrupamiento;
    Instances dataset;
    MiTareaAsincronaDialog tarea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //es necesario llamar al mismo método de la clase base
        super.onCreate(savedInstanceState);
        //esta actividad muestra el layout descrito en activity_main2.xml
        setContentView(R.layout.activity_main2);
        //se relacionan las variables con los elementos del layout
        texto_archivo_anterior = (TextView) findViewById(R.id.texto_archivo_anterior);
        texto_relacionar = (TextView) findViewById(R.id.texto_relacionar);
        texto_archivoactual = (TextView) findViewById(R.id.texto_archivoactual);
        texto_buscararchivo = (TextView) findViewById(R.id.textView4);
        texto_xml = (TextView) findViewById(R.id.textView8);
        boton_relacionar = (Button) findViewById(R.id.boton_relacionar);
        boton_NOrelacionar = (Button) findViewById(R.id.boton_NoRelacionar);
        boton_abrir = (Button) findViewById(R.id.BotonAbrir);
        boton_cambiar = (Button) findViewById(R.id.boton_cambiar);
        boton_continuar = (Button) findViewById(R.id.boton_continuar);
        boton_seleccionar_archivo_anterior = (Button) findViewById(R.id.BotonSeleccionarArchivoAnterior);
        desplegable_relacionar = (Spinner) findViewById(R.id.desplegable_relacionar);
        desplegable_archivo_anterior = (Spinner) findViewById(R.id.desplegable_archivo_anterior);
        nombre_archivo = (EditText) findViewById(R.id.texto_nombrearchivo);

        progressDialog = new ProgressDialog(MainActivity.this);
        //se establece una conexión con la base de datos BDprograma versión 4
        helper = new MaquinaSQLiteHelper(this, "BDprograma", null, 4);
        baseDatos = helper.getWritableDatabase();
        //preparo la visualizanción
        texto_relacionar.setVisibility(View.INVISIBLE);
        boton_relacionar.setVisibility(View.INVISIBLE);
        boton_NOrelacionar.setVisibility(View.INVISIBLE);
        desplegable_relacionar.setVisibility(View.INVISIBLE);
        texto_archivo_anterior.setVisibility(View.INVISIBLE);
        boton_seleccionar_archivo_anterior.setVisibility(View.INVISIBLE);
        desplegable_archivo_anterior.setVisibility(View.INVISIBLE);
        //compruebo si tengo información guardada en tabla UltimoArchivo
        String ordenSQL = "SELECT nombre FROM tablaUltimoArchivo";
        Cursor cursor = baseDatos.rawQuery(ordenSQL, null);
        Integer cuenta = cursor.getCount();
        if (cuenta == 0){//no hay información
            //continuo preparando la visualización
            boton_cambiar.setVisibility(View.INVISIBLE);
            boton_continuar.setVisibility(View.INVISIBLE);
            texto_archivoactual.setVisibility(View.INVISIBLE);
            texto_buscararchivo.setVisibility(View.VISIBLE);
            texto_xml.setVisibility(View.VISIBLE);
            nombre_archivo.setVisibility(View.VISIBLE);
            boton_abrir.setVisibility(View.VISIBLE);
        }else{//hay información
            //continuo preparando la visualización
            texto_buscararchivo.setVisibility(View.INVISIBLE);
            texto_xml.setVisibility(View.INVISIBLE);
            nombre_archivo.setVisibility(View.INVISIBLE);
            boton_abrir.setVisibility(View.INVISIBLE);
            boton_cambiar.setVisibility(View.VISIBLE);
            boton_continuar.setVisibility(View.VISIBLE);
            texto_archivoactual.setVisibility(View.VISIBLE);

            cursor.moveToFirst();
            String nombre_archivo_ultimo=cursor.getString(0);
            ordenSQL = "SELECT fecha FROM tablaArchivos WHERE nombre='" + nombre_archivo_ultimo + "' ";
            Cursor cursor_fecha = baseDatos.rawQuery(ordenSQL, null);
            ordenSQL = "SELECT hora FROM tablaArchivos WHERE nombre='" + nombre_archivo_ultimo + "' ";
            Cursor cursor_hora = baseDatos.rawQuery(ordenSQL, null);
            cursor_fecha.moveToFirst();
            cursor_hora.moveToFirst();
            texto_archivoactual.setText("Los datos se están procesando con la información proporcionada por el archivo '"+nombre_archivo_ultimo+"' leído por última vez el "+cursor_fecha.getString(0)+" a las "+cursor_hora.getString(0)+".\nSi  desea que se vuelva a leer el archivo o que los datos sean procesados con la información proporcionada por otro archivo pulse 'CAMBIAR ARCHIVO DE LECTURA'.");
        }
    }

    public void BotonContinuar_Click(View v){
        //cambio de pantalla
        Intent i = new Intent(this, Main2Activity.class);
        startActivity(i);
        Singleton.guarda("basedatos", baseDatos);
    }

    public void BotonCambiar_Click(View v){
        //preparo la visualización
        boton_cambiar.setVisibility(View.INVISIBLE);
        boton_continuar.setVisibility(View.INVISIBLE);
        texto_archivoactual.setVisibility(View.INVISIBLE);
        texto_buscararchivo.setVisibility(View.VISIBLE);
        texto_xml.setVisibility(View.VISIBLE);
        nombre_archivo.setVisibility(View.VISIBLE);
        boton_abrir.setVisibility(View.VISIBLE);
        //relleno el desplegable con las opciones
        opciones_archivo_anterior = new ArrayList<String>();
        String ordenSQL = "SELECT nombre FROM tablaArchivos";
        Cursor cursor_nombre = baseDatos.rawQuery(ordenSQL, null);
        if (cursor_nombre.moveToFirst()) {//apunta al primer resultado
            do {
                //añade el nombre del archivo a opciones_archivo_anterior
                opciones_archivo_anterior.add(cursor_nombre.getString(0));
            } while (cursor_nombre.moveToNext()); //mientras haya más resultados
            texto_archivo_anterior.setVisibility(View.VISIBLE);
            boton_seleccionar_archivo_anterior.setVisibility(View.VISIBLE);
            desplegable_archivo_anterior.setVisibility(View.VISIBLE);
        }
        //se ordena la lista
        Collections.sort(opciones_archivo_anterior);
        //se asocia la lista de opciones con el desplegable
        final ArrayAdapter<String> adaptador = new ArrayAdapter<String>(this,
                R.layout.spinner_personalizado_gris, opciones_archivo_anterior);
        adaptador.setDropDownViewResource(R.layout.spinner_personalizado_gris);
        desplegable_archivo_anterior.setAdapter(adaptador);
        desplegable_archivo_anterior.setSelection(0);
        //creación de una clase anónima
        desplegable_archivo_anterior.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //cuando el ususario elige una opción
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                seleccionado_archivo_anterior = desplegable_archivo_anterior.getSelectedItem().toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    public void BotonSeleccionarArchivoAnterior_Click(View v){
        //borro la informacion de la tablaUltimoArchivo
        String ordenSQL = "DELETE FROM tablaUltimoArchivo";
        baseDatos.execSQL(ordenSQL);
        //guardo el nombre del archivo seleccionado en la tablaUltimoArchivo
        nombre=seleccionado_archivo_anterior;
        guardar_info_tablaUltimoArchivo();
        //dejo la pantalla preparada y cambio de pantalla
        cambiar_pantalla();
    }

    public void cambiar_pantalla(){
        //se deja preparada la pantalla
        String ordenSQL = "SELECT nombre FROM tablaUltimoArchivo";
        Cursor cursor_nombre_pantalla = baseDatos.rawQuery(ordenSQL, null);
        cursor_nombre_pantalla.moveToFirst();
        String nombre_archivo_pantalla=cursor_nombre_pantalla.getString(0);
        texto_buscararchivo.setVisibility(View.INVISIBLE);
        texto_xml.setVisibility(View.INVISIBLE);
        nombre_archivo.setVisibility(View.INVISIBLE);
        boton_abrir.setVisibility(View.INVISIBLE);
        texto_archivo_anterior.setVisibility(View.INVISIBLE);
        boton_seleccionar_archivo_anterior.setVisibility(View.INVISIBLE);
        desplegable_archivo_anterior.setVisibility(View.INVISIBLE);
        texto_relacionar.setVisibility(View.INVISIBLE);
        boton_relacionar.setVisibility(View.INVISIBLE);
        boton_NOrelacionar.setVisibility(View.INVISIBLE);
        desplegable_relacionar.setVisibility(View.INVISIBLE);
        boton_cambiar.setVisibility(View.VISIBLE);
        boton_continuar.setVisibility(View.VISIBLE);
        texto_archivoactual.setVisibility(View.VISIBLE);
        ordenSQL = "SELECT fecha FROM tablaArchivos WHERE nombre='" + nombre_archivo_pantalla + "' ";
        Cursor cursor_fecha_pantalla = baseDatos.rawQuery(ordenSQL, null);
        ordenSQL = "SELECT hora FROM tablaArchivos WHERE nombre='" + nombre_archivo_pantalla + "' ";
        Cursor cursor_hora_pantalla = baseDatos.rawQuery(ordenSQL, null);
        cursor_fecha_pantalla.moveToFirst();
        cursor_hora_pantalla.moveToFirst();
        texto_archivoactual.setText("Los datos se están procesando con la información proporcionada por el archivo '"+nombre_archivo_pantalla+"' leído por última vez el "+cursor_fecha_pantalla.getString(0)+" a las "+cursor_hora_pantalla.getString(0)+".\nSi  desea que se vuelva a leer el archivo o que los datos sean procesados con la información proporcionada por otro archivo pulse 'CAMBIAR ARCHIVO DE LECTURA'.");
        //cambio de pantalla
        Intent i = new Intent(this, Main2Activity.class);
        startActivity(i);
        Singleton.guarda("basedatos", baseDatos);
    }
    /*public void BotonBorrar_Click(View v){
        //botón para el desarrollo de la app
        baseDatos.execSQL("DROP TABLE IF EXISTS tablaClusters"); // Ejecuta la orden SQL
        baseDatos.execSQL("DROP TABLE IF EXISTS tablaUltimoArchivo"); // Elimina la tabla
        baseDatos.execSQL("DROP TABLE IF EXISTS tablaArchivos"); // Elimina la tabla
        helper.onCreate(baseDatos);
        Singleton.guarda("basedatos",baseDatos);
        Toast.makeText(this, "Borrado", Toast.LENGTH_LONG).show();
    }*/

    public void BotonAbrir_Click(View v) {
        //buleano para saber si la app tiene permiso de acceso al almacenamiento
        boolean permiso = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!permiso) {//si la app NO tiene el permiso de acceso al almacenamiento
            //se muestra mensaje en pantalla
            AlertDialog.Builder dialogo_aviso = new AlertDialog.Builder(MainActivity.this);
            dialogo_aviso.setMessage("No se pudo abrir el archivo.\nEs necesario que conceda permiso a la aplicación para acceder al almacenamiento interno de su móvil.\nPuede hacer esto en ajustes < aplicaciones < permisos < memoria.").setCancelable(true)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            AlertDialog dialogo_titulo = dialogo_aviso.create();
            dialogo_titulo.setTitle("Permiso denegado.");
            dialogo_titulo.show();
        } else{//si la app SÍ tiene permiso de acceso al almacenamiento
            nombre = nombre_archivo.getText().toString();
            //se añade la extensión al nombre del archivo
            String nombre_extension = nombre + ".xml";
            //objetos necesarios para manejar un archivo en formato XML
            Document archivoXml = null;
            DocumentBuilderFactory dbf;
            DocumentBuilder db;
            dbf = DocumentBuilderFactory.newInstance();
            try { //intenta la ejecución de ...
                db = dbf.newDocumentBuilder();
                //se obtiene la ruta del archivo
                File archivoleer = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), nombre_extension);
                //se lee toda la información escrita en archivo.xml y se interpreta
                archivoXml = db.parse(archivoleer);
                //se organiza la información leída con una estructura en forma de árbol
                Element nodoRaiz = archivoXml.getDocumentElement();
                //se guardan los datos del archivo leído en variables
                numero_clusters = Integer.parseInt(nodoRaiz.getElementsByTagName("numero_clusters").item(0).getFirstChild().getNodeValue());
                nombre_atributos = nodoRaiz.getElementsByTagName("nombre_atributos").item(0).getFirstChild().getNodeValue();
                NodeList datos_recogidos = nodoRaiz.getElementsByTagName("datos_recogidos");
                Element datos = (Element) datos_recogidos.item(0);
                NodeList listaDatos = datos.getElementsByTagName("dato");
                int numDatos = listaDatos.getLength();
                nombres_atributos = nombre_atributos.split(",");
                //nombres de los atributos
                ArrayList<Attribute> atributos = new ArrayList<Attribute>();
                for (int j = 0; j < nombres_atributos.length; j++) {
                    atributos.add(new Attribute(nombres_atributos[j]));
                }
                //se crea un dataset asignándole el nombre "datos"
                dataset = new Instances("datos", atributos, 0);
                //se rellena el dataset
                double[] valores;
                for (int k = 0; k < numDatos; k++) {
                    valores = new double[dataset.numAttributes()];
                    Element dato = (Element) listaDatos.item(k);
                    for (int j = 0; j < nombres_atributos.length; j++) {
                        valores[j] = Double.parseDouble(dato.getAttribute(nombres_atributos[j]));
                    }
                    DenseInstance instancia = new DenseInstance(1.0, valores);
                    dataset.add(instancia);
                }
                agrupamiento = new EM_modificada();
                //se llama a la ejecucuion de una tarea en segundo plano
                tarea = new MiTareaAsincronaDialog();
                tarea.execute();
            } catch (Exception e) {
                e.printStackTrace();
                //se informa al usuario de que no se pudo leer el archivo
                Toast.makeText(this, "No se pudo leer el archivo", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void continuar_abrir(){
        try {
            //se realiza el clustering
            agrupamiento.setNumClusters(num_definitivo_clusters);
            agrupamiento.buildClusterer(dataset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        grupos = agrupamiento.getClusterModelsNumericAtts();
        //se busca si hay información de archivos anteriores guardada
        String ordenSQL = "SELECT nombre FROM tablaArchivos";
        Cursor cursor = baseDatos.rawQuery(ordenSQL, null);
        Integer num_registros = cursor.getCount();
        //si hay información guardada
        if (num_registros != 0) {
            //se rellena el desplegable con los nombres de los archivos
            opciones = new ArrayList<String>();
            ordenSQL = "SELECT nombre FROM tablaArchivos";
            cursor = baseDatos.rawQuery(ordenSQL, null);
            if (cursor.moveToFirst()) {
                do {
                    opciones.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
            Collections.sort(opciones);
            final ArrayAdapter<String> adaptador = new ArrayAdapter<String>(this,
                    R.layout.spinner_personalizado_gris, opciones);
            adaptador.setDropDownViewResource(R.layout.spinner_personalizado_gris);
            desplegable_relacionar.setAdapter(adaptador);
            desplegable_relacionar.setSelection(0);
            desplegable_relacionar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                    //si se selecciona un archivo del desplegable
                    seleccionado = desplegable_relacionar.getSelectedItem().toString();
                }
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
            final AlertDialog.Builder dialogo_cambio = new AlertDialog.Builder(MainActivity.this);
            //se crea un mensaje
            dialogo_cambio.setMessage("¿Quiere que se relacione la información de un antiguo archivo con la del actual?").setCancelable(false)
                    .setPositiveButton("SÍ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //si el usuario quiere relacionar información
                            //se prepara visualización en pantalla
                            texto_relacionar.setVisibility(View.VISIBLE);
                            boton_relacionar.setVisibility(View.VISIBLE);
                            boton_NOrelacionar.setVisibility(View.VISIBLE);
                            desplegable_relacionar.setVisibility(View.VISIBLE);
                            boton_abrir.setVisibility(View.INVISIBLE);
                            texto_archivo_anterior.setVisibility(View.INVISIBLE);
                            boton_seleccionar_archivo_anterior.setVisibility(View.INVISIBLE);
                            desplegable_archivo_anterior.setVisibility(View.INVISIBLE);
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //si el usuario NO quiere relacionar información
                            //si hay un archivo con el mismo nombre se borra la información guardada
                            String ordenSQL = "DELETE FROM tablaArchivos WHERE nombre='" + nombre + "' ";
                            baseDatos.execSQL(ordenSQL);
                            ordenSQL = "DELETE FROM tablaClusters WHERE nombre_archivo='" + nombre + "' ";
                            baseDatos.execSQL(ordenSQL);
                            ordenSQL = "DELETE FROM tablaUltimoArchivo";
                            baseDatos.execSQL(ordenSQL);
                            //se guarda la información del archivo actual
                            guardar_info();
                            //se cambia de pantalla
                            cambiar_pantalla();
                        }
                    });
            AlertDialog dialogo_titulo = dialogo_cambio.create();
            dialogo_titulo.setTitle("Relacionar información.");
            dialogo_titulo.show();
        } else {//si no hay información guardada
            //se guarda la información del archivo actual
            guardar_info();
            //se cambia de pantalla
            cambiar_pantalla();
        }
    }
    public void BotonRelacionar_Click(View v) {
        //se compruebo la compatibilidad de los archivos
        String ordenSQL = "SELECT nombre_atributos FROM tablaArchivos WHERE nombre='" + seleccionado + "' ";
        Cursor cursor_nombre_atributos = baseDatos.rawQuery(ordenSQL, null);
        cursor_nombre_atributos.moveToFirst();
        //si los archivos son compatibles
        if (cursor_nombre_atributos.getString(0).equals(nombre_atributos)) {
            //se extrae la informción de la base de datos
            ordenSQL = "SELECT nombre_cluster FROM tablaClusters WHERE nombre_archivo='" + seleccionado + "' ";
            Cursor cursor_nombre = baseDatos.rawQuery(ordenSQL, null);
            Cursor cursor_nombre_repetido = baseDatos.rawQuery(ordenSQL, null);
            ordenSQL = "SELECT descripcion FROM tablaClusters WHERE nombre_archivo='" + seleccionado + "' ";
            Cursor cursor_descripcion = baseDatos.rawQuery(ordenSQL, null);
            ordenSQL = "SELECT medio FROM tablaClusters WHERE nombre_archivo='" + seleccionado + "' ";
            Cursor cursor_medio = baseDatos.rawQuery(ordenSQL, null);
            ordenSQL = "SELECT aviso FROM tablaClusters WHERE nombre_archivo='" + seleccionado + "' ";
            Cursor cursor_aviso = baseDatos.rawQuery(ordenSQL, null);
            ordenSQL = "SELECT textoaviso FROM tablaClusters WHERE nombre_archivo='" + seleccionado + "' ";
            Cursor cursor_textoaviso = baseDatos.rawQuery(ordenSQL, null);
            //se buscan los id de los modos de funcionamiento
            ordenSQL = "SELECT id FROM tablaClusters WHERE nombre_archivo='" + nombre + "' ";
            Cursor cursor_id = baseDatos.rawQuery(ordenSQL, null);
            Integer [] id_borrar_clusters = new Integer[0];
            //si hay algún archivo anterior con el mismo nombre
            if (opciones.contains(nombre)) {
                //se borra de la tablaArchivos
                ordenSQL = "DELETE FROM tablaArchivos WHERE nombre='" + nombre + "' ";
                baseDatos.execSQL(ordenSQL);
                id_borrar_clusters=new Integer[cursor_id.getCount()];
                Integer in =0;
                if (cursor_id.moveToFirst()){
                    do {
                        //se guardan los id de los modos de funcionamiento
                        id_borrar_clusters[in]=cursor_id.getInt(0);
                        in++;
                    } while (cursor_id.moveToNext());
                }
            }
            //se guarda la información
            guardar_info_tablaArchivos();
            Boolean pertenece = false;
            Integer w = 0,nmodo = 0;
            String medio_insertar = "", desviacion_insertar = "";
            //para cada modo de funcionamiento del archivo leído
            for (int g = 0; g < num_definitivo_clusters; g++) {
                cursor_descripcion.moveToFirst();
                cursor_medio.moveToFirst();
                cursor_aviso.moveToFirst();
                cursor_textoaviso.moveToFirst();
                if (cursor_nombre.moveToFirst()) {
                    //mientras no se encuentre un modo que se pueda relacionar y no se hayan recorrido
                    // todos los modos...
                    do {
                        String vmedios = cursor_medio.getString(0);
                        String[] medio_relacionar = vmedios.split(",");
                        w = 0;
                        pertenece = true;
                        //mientras se puedan relacionar y no se hayan calculado todas las desviaciones...
                        do {
                            //se calcula la desviación de los valores medios
                            double desviacion_obtenida = desviacion(grupos[g][w][0], Double.parseDouble(medio_relacionar[w]));
                            //si la desviación calculada es mayor de la desviación del archivo leído
                            if (desviacion_obtenida > grupos[g][w][1])
                                //los archivos no se pueden relacionar
                                pertenece = false;
                            w++;
                        } while (w < nombres_atributos.length & pertenece);
                        cursor_descripcion.moveToNext();
                        cursor_medio.moveToNext();
                        cursor_aviso.moveToNext();
                        cursor_textoaviso.moveToNext();
                    } while (cursor_nombre.moveToNext() & !pertenece);
                }
                cursor_nombre.moveToPrevious();
                cursor_descripcion.moveToPrevious();
                cursor_medio.moveToPrevious();
                cursor_aviso.moveToPrevious();
                cursor_textoaviso.moveToPrevious();
                medio_insertar = "";
                desviacion_insertar = "";
                //se guardan en variables los datos medios y desviaciones del archivo leído
                for (int a = 0; a < nombres_atributos.length; a++) {
                    if (!medio_insertar.equals("")) {
                        medio_insertar = medio_insertar + ",";
                        desviacion_insertar = desviacion_insertar + ",";
                    }
                    medio_insertar = medio_insertar + Double.toString(grupos[g][a][0]);
                    desviacion_insertar = desviacion_insertar + Double.toString(grupos[g][a][1]);
                }
                //si se encontó un modo con el que relacionar la información
                if (pertenece) {
                    //se guarda la información relacionada
                    ordenSQL = "INSERT INTO tablaClusters (nombre_archivo,nombre_cluster, descripcion, aviso,textoaviso, medio, desviacion) VALUES ('" +
                            nombre + "','" + cursor_nombre.getString(0) + "', '" + cursor_descripcion.getString(0) + "', '" + cursor_aviso.getInt(0) + "','" + cursor_textoaviso.getString(0) + "', '" + medio_insertar + "', '" + desviacion_insertar + "')";
                    baseDatos.execSQL(ordenSQL);
                } else {//si NO se encontó un modo con el que relacionar la información
                    Boolean ok = true;
                    String nombre_modo="";
                    //se comprueba que el nombre no esté repetido
                    do {//mientras no encuentre un nombre no repetido
                        if (cursor_nombre_repetido.moveToFirst()) {
                            do {//mientras no se recorran todos los nombres del archivo seleccionado y
                                // el nombre no esté repetido
                                nombre_modo = "Modo sin nombrar " + Integer.toString(nmodo);
                                ok=true;
                                if (nombre_modo.equals(cursor_nombre_repetido.getString(0))) {
                                    ok = false;
                                }
                            } while (cursor_nombre_repetido.moveToNext() & ok);
                        }
                        nmodo++;
                    }while(!ok);
                    //se guarda la información
                    ordenSQL = "INSERT INTO tablaClusters (nombre_archivo,nombre_cluster, descripcion, aviso,textoaviso, medio, desviacion) VALUES ('" +
                            nombre + "','" + nombre_modo+ "', '', '" + 0 + "','El modo de funcionamiento actual de la planta es incorrecto', '" + medio_insertar + "', '" + desviacion_insertar + "')";
                    baseDatos.execSQL(ordenSQL);
                }
            }
            //si había un archivo anterior con el nombre del actual se borra la información
            if(id_borrar_clusters.length !=0){
                for (int f = 0; f < cursor_id.getCount(); f++) {
                    ordenSQL = "DELETE FROM tablaClusters WHERE id='" + id_borrar_clusters[f] + "' ";
                    baseDatos.execSQL(ordenSQL);
                }
            }
            //se guarda la información en tablaUltimoArchivo
            ordenSQL = "DELETE FROM tablaUltimoArchivo";
            baseDatos.execSQL(ordenSQL);
            guardar_info_tablaUltimoArchivo();
            cambiar_pantalla();
        }else{//si los archivos NO son compatibles
            //se crea un mensaje indicándolo
            AlertDialog.Builder dialogo_aviso = new AlertDialog.Builder(MainActivity.this);
            dialogo_aviso.setMessage("El archivo selecionado no es compatible, para que sea compatible debe tener los mismos atributos.\n" + "Seleccione un archivo diferente o cancele la operación.").setCancelable(true)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            AlertDialog dialogo_titulo = dialogo_aviso.create();
            dialogo_titulo.setTitle("Error de compatibilidad.");
            dialogo_titulo.show();
        }
    }

    public void BotonNoRelacionar_Click(View v) {
        //si hay un archivo con el mismo nombre se borra la información guardada
        String ordenSQL = "DELETE FROM tablaArchivos WHERE nombre='"+nombre+"' ";
        baseDatos.execSQL(ordenSQL);
        ordenSQL = "DELETE FROM tablaClusters WHERE nombre_archivo='"+nombre+"' ";
        baseDatos.execSQL(ordenSQL);
        ordenSQL = "DELETE FROM tablaUltimoArchivo";
        baseDatos.execSQL(ordenSQL);
        //se guarda la información del archivo actual
        guardar_info();
        //se cambia de pantalla
        cambiar_pantalla();
    }
    public double  desviacion(double valor1, double valor2){
        double desviacion;
        double media= (valor1 + valor2)/2;
        double a= Math.pow(valor1-media,2);
        double b= Math.pow(valor2-media,2);
        desviacion=Math.sqrt((a+b)/2);
        return(desviacion);
    }
    public void guardar_info(){
        guardar_info_tablaArchivos();
        guardar_info_tablaCluster();
        guardar_info_tablaUltimoArchivo();
    }
    public void guardar_info_tablaUltimoArchivo(){
        //se guarda la información del último archivo leído
        String ordenSQL = "INSERT INTO tablaUltimoArchivo (nombre) VALUES ('" + nombre + "')";
        baseDatos.execSQL(ordenSQL);
        Singleton.guarda("basedatos",baseDatos);
    }
    public void guardar_info_tablaCluster(){
        String medio_insertar="";
        String desviacion_insertar="";
        //para cada cluster...
        for (int g =0 ; g < num_definitivo_clusters ; g++){
            medio_insertar="";
            desviacion_insertar="";
            //se guardan los valores medios y desviaciones como strings
            for (int a =0 ; a < nombres_atributos.length ; a++){
                //se añade coma separadora
                if (! medio_insertar.equals("")){
                    medio_insertar = medio_insertar + ",";
                    desviacion_insertar = desviacion_insertar + ",";
                }
                medio_insertar = medio_insertar +Double.toString(grupos[g][a][0]);
                desviacion_insertar = desviacion_insertar +Double.toString(grupos[g][a][1]);
            }
            //se guarda la información
            String ordenSQL = "INSERT INTO tablaClusters (nombre_archivo,nombre_cluster, descripcion, aviso,textoaviso, medio, desviacion) VALUES ('" +
                    nombre + "','"+ "Modo sin nombrar "+Integer.toString(g) +"', '', '" + 0 + "','El modo de funcionamiento actual de la planta es incorrecto', '" +medio_insertar+ "', '" + desviacion_insertar + "')";
            baseDatos.execSQL(ordenSQL);
        }
        Singleton.guarda("basedatos",baseDatos);
    }
    public void guardar_info_tablaArchivos() {
        //se consulta la fecha y hora
        Date date = new Date();
        SimpleDateFormat fecha= new SimpleDateFormat("d/MMMM/yyyy");
        String sfecha =fecha.format(date);
        SimpleDateFormat hora= new SimpleDateFormat("HH:mm");
        String shora =hora.format(date);
        //se guarda la información
        String ordenSQL = "INSERT INTO tablaArchivos (nombre, numclusters, numatributos,nombre_atributos,fecha,hora) VALUES ('" +
                nombre + "','"+num_definitivo_clusters+"', '" + nombres_atributos.length + "','"+nombre_atributos+"','"+sfecha+"','"+shora+"')";
        baseDatos.execSQL(ordenSQL);
        Singleton.guarda("basedatos",baseDatos);
    }

    public void elegir_num_clusters(){
        //si el número de clusters calculado coincide con el del archivo
        if (num_cluster_calculado==numero_clusters) {
            num_definitivo_clusters = numero_clusters;
            continuar_abrir();
        }else {//si el número de clusters calculado NO coincide con el del archivo
            //se muestra en pantalla un mensaje
            final AlertDialog.Builder dialogo_cambio = new AlertDialog.Builder(MainActivity.this);
            dialogo_cambio.setMessage("Se ha detectado que el número de clusters es: "+num_cluster_calculado+ ".\nEste número es diferente al indicado en el archivo ("+numero_clusters+").\n¿Desea que se interpreten los datos del archivo con el número de clusters = "+num_cluster_calculado+"?. Si pulsa 'NO' se utilizará el número proporcionado por el archivo.").setCancelable(false)
                    .setPositiveButton("SÍ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //si el usuario escoge el número calculado
                            num_definitivo_clusters = num_cluster_calculado;
                            continuar_abrir();
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //si el usuario escoge el número del archivo
                            num_definitivo_clusters = numero_clusters;
                            continuar_abrir();
                        }
                    });
            AlertDialog dialogo_titulo = dialogo_cambio.create();
            dialogo_titulo.setTitle("Número de clusters incorrecto.");
            dialogo_titulo.show();
            dialogo_titulo.setCancelable(false);
        }
    }

    private class MiTareaAsincronaDialog extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            //se comprueba si el número de clusters indicado en el archivo es correcto
            //declaración de variables
            Integer cluster_min, cluster_max = numero_clusters + 4,p;
            Double desvmax_g = 0.0, desvmax_min=1000000.0;
            //se establece el valor del menor múmero de clusters
            if((numero_clusters-5)>0){
                cluster_min = numero_clusters-5;
            }else{cluster_min = 1;}
            //se crea una matriz para lasdesviaciones máximas
            Double [] desv = new Double[cluster_max-cluster_min];
            try {
                p=0;
                //para cada número de clusters...
                for (int nc = cluster_min; nc < cluster_max; nc ++) {
                    //se indica el número de clusters
                    agrupamiento.setNumClusters(nc);
                    //se realiza el clustering sobre los datos
                    agrupamiento.buildClusterer(dataset);
                    //se recoge la información de los clusters
                    grupos = agrupamiento.getClusterModelsNumericAtts();
                    desvmax_g = 0.0;
                    //se busca la mayor desviación de los clusters y atributos
                    for (int g = 0; g < nc; g++) {
                        for (int j = 0; j < nombres_atributos.length; j++) {
                            if(grupos[g][j][1]>desvmax_g)
                                desvmax_g=grupos[g][j][1];
                        }
                    }
                    //se guarda la mayor desviación
                    desv[p]=desvmax_g;
                    p++;
                }
                Integer posicion_min=0;
                //se busca la menor desviación de las desviaciones máximas
                for(int d =0; d <desv.length; d++){
                    if (desv[d]<desvmax_min){
                        desvmax_min = desv[d];
                        posicion_min = d;
                    }
                }
                Double diferencia=0.0;
                //se busca el salto
                if(posicion_min > 0){
                    do{
                        diferencia = Math.abs(desv[posicion_min - 1] - desv[posicion_min]);
                        posicion_min = posicion_min-1;
                    }while ((diferencia < (desv[posicion_min+1]*2)) & (posicion_min > 0));
                }
                //si el resultado no es fiable se deja el del archivo
                num_cluster_calculado = numero_clusters;
                //si el resultado es fiable se guarda
                if(diferencia > (desv[posicion_min+1]*2))
                    num_cluster_calculado = posicion_min +cluster_min+1;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        @Override
        protected void onPreExecute() {
            //se prepara y muestra la pantalla de carga
            progressDialog.show();
            progressDialog.setContentView(R.layout.show_dialog);
            progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            progressDialog.setCancelable(false);
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                //se deja de mostrar la pantalla de carga
                progressDialog.dismiss();
                elegir_num_clusters();
            }
        }
    }
}