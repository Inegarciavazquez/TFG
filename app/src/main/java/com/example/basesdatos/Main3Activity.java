package com.example.basesdatos;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class Main3Activity extends AppCompatActivity {
    //se declaran variables y referencias
    Spinner desplegable,desplegable_archivos;
    Button boton_cambios;
    EditText texto_nombre, texto_descripcion,texto_aviso;
    SQLiteDatabase baseDatos;
    CheckBox check_aviso;
    TextView texto_medio,etiqueta_aviso,texto_archivo_actual;
    Integer idnum;
    String nombre_archivo;
    ArrayList<String>nombre_modos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //es necesario llamar al mismo método de la clase base
        super.onCreate(savedInstanceState);
        //esta actividad muestra el layout descrito en activity_main3.xml
        setContentView(R.layout.activity_main3);
        //se relacionan las variables con los elementos del layout
        texto_archivo_actual = (TextView) findViewById(R.id.texto_archivoactual_2);
        desplegable = (Spinner) findViewById(R.id.desplegable);
        desplegable_archivos = (Spinner) findViewById(R.id.desplegable_archivos_cambiar);
        boton_cambios=(Button) findViewById(R.id.Boton_cambios);
        texto_descripcion = (EditText) findViewById(R.id.texto_descripcion);
        texto_nombre = (EditText) findViewById(R.id.texto_nombre);
        texto_aviso = (EditText) findViewById(R.id.texto_aviso);
        check_aviso = (CheckBox) findViewById(R.id.checkBox_aviso);
        etiqueta_aviso = (TextView) findViewById(R.id.Etiqueta_aviso);
        texto_medio = (TextView) findViewById(R.id.Texto_mediox);

        baseDatos =(SQLiteDatabase) Singleton.busca("basedatos");
        //se indica en la cabecera el nombre del archivo actual
        String ordenSQL = "SELECT nombre FROM tablaUltimoArchivo";
        Cursor cursor_nombre_archivo_actual = baseDatos.rawQuery(ordenSQL, null);
        cursor_nombre_archivo_actual.moveToFirst();
        String nombre_archivo_actual = cursor_nombre_archivo_actual.getString(0);
        texto_archivo_actual.setText("Archivo actual: '"+nombre_archivo_actual+"'");
        //se rellena el desplegable con el nombre de los archivos
        ArrayList<String>nombre_archivos = new ArrayList<String>();
        ordenSQL = "SELECT nombre FROM tablaArchivos";
        Cursor cursor = baseDatos.rawQuery(ordenSQL, null);
        if (cursor.moveToFirst()) {
            do {
                String nombre = cursor.getString(0);
                nombre_archivos.add(nombre);
            } while (cursor.moveToNext());
        }
        ArrayAdapter<String> adaptador_2 = new ArrayAdapter<String>(this,R.layout.spinner_personalizado_gris, nombre_archivos);
        adaptador_2.setDropDownViewResource(R.layout.spinner_personalizado_gris);
        desplegable_archivos.setAdapter(adaptador_2);
        //se deja seleccionado el archivo actual
        desplegable_archivos.setSelection(nombre_archivos.indexOf(nombre_archivo_actual));
        desplegable_archivos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //cuando se selecciona un archivo...
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id){
                nombre_archivo = desplegable_archivos.getSelectedItem().toString();
                String ordenSQL = "SELECT nombre_cluster FROM tablaClusters WHERE nombre_archivo ='"+ nombre_archivo+"'";
                Cursor cursor = baseDatos.rawQuery(ordenSQL, null);
                //se rellena el desplegable con el nombre de los modos de funcionamiento
                nombre_modos = new ArrayList<String>();
                if (cursor.moveToFirst()) {
                    do {
                        String nombre = cursor.getString(0);
                        nombre_modos.add(nombre);
                    } while (cursor.moveToNext());
                }
                crear_desplegable();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        desplegable.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //cuando se selecciona un modo de funcionamiento...
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id){
                baseDatos =(SQLiteDatabase) Singleton.busca("basedatos");
                String seleccionado = desplegable.getSelectedItem().toString();
                texto_nombre.setText(seleccionado);
                //se muestra la descripción del modo seleccionado
                String ordenSQL = "SELECT descripcion FROM tablaClusters WHERE (nombre_cluster='" + seleccionado +"') AND (nombre_archivo='" + nombre_archivo + "')";
                Cursor cursor = baseDatos.rawQuery(ordenSQL, null);
                if (cursor.moveToFirst()) {
                    texto_descripcion.setText(cursor.getString(0));
                }
                //se muestra el mensaje de aviso del modo seleccionado
                ordenSQL = "SELECT textoaviso FROM tablaClusters WHERE (nombre_cluster='" + seleccionado +"') AND (nombre_archivo='" + nombre_archivo + "')";
                cursor = baseDatos.rawQuery(ordenSQL, null);
                if (cursor.moveToFirst()) {
                    texto_aviso.setText(cursor.getString(0));
                }
                //se muestran los valores medios del modo seleccionado
                ordenSQL = "SELECT medio FROM tablaClusters WHERE (nombre_cluster='" + seleccionado +"') AND (nombre_archivo='" + nombre_archivo + "')";
                Cursor cursor_valmedio = baseDatos.rawQuery(ordenSQL, null);
                ordenSQL = "SELECT nombre_atributos FROM tablaArchivos WHERE nombre='" + nombre_archivo + "'";
                Cursor cursor_nombreatributos = baseDatos.rawQuery(ordenSQL, null);
                ordenSQL = "SELECT numatributos FROM tablaArchivos WHERE nombre='" + nombre_archivo + "'";
                Cursor cursor_numatributos = baseDatos.rawQuery(ordenSQL, null);
                cursor_valmedio.moveToFirst();
                cursor_nombreatributos.moveToFirst();
                cursor_numatributos.moveToFirst();
                Integer numero_atributos = cursor_numatributos.getInt(0);
                String valmedios= cursor_valmedio.getString(0);
                String[] vmedio = valmedios.split(",");
                String nombreatributos= cursor_nombreatributos.getString(0);
                String[] nombatributos = nombreatributos.split(",");
                String texto_valoresmedios="";
                for (int m = 0; m < numero_atributos; m++) {
                    texto_valoresmedios = texto_valoresmedios + nombatributos[m] + " = "+ vmedio[m] +"\n";
                }
                texto_medio.setText(texto_valoresmedios);
                //se marca o desmarca la casilla de emitir aviso
                ordenSQL = "SELECT aviso FROM tablaClusters WHERE (nombre_cluster='" + seleccionado +"') AND (nombre_archivo='" + nombre_archivo + "')";
                cursor = baseDatos.rawQuery(ordenSQL, null);
                if (cursor.moveToFirst()) {
                    if((cursor.getInt(0))== 1) {
                        //se marca
                        check_aviso.setChecked(true);
                    }else{
                        //se desmarca
                        check_aviso.setChecked(false);
                    }
                }
                mostrar_aviso();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    public void BotonCambiosClick(View v) {
        String seleccionado = desplegable.getSelectedItem().toString();
        //compruebo que no haya otro cluster con el nombre.
        Integer var = 0;
        //si no se cambió el nombre
        if (seleccionado.equals(texto_nombre.getText().toString()))
            var = 1;
        //busco si el nuevo nombre coincide con el de otro cluster
        String ordenSQL = "SELECT id FROM tablaClusters WHERE (nombre_cluster='" + texto_nombre.getText().toString() + "') AND (nombre_archivo='" + nombre_archivo + "')";
        Cursor cursor = baseDatos.rawQuery(ordenSQL, null);
        //si el nuevo nombre NO coincide con el de otro cluster
        if (cursor.getCount() == var){
            //se recoge el id del modo a guardar cambios
            idnum = 0;
            ordenSQL = "SELECT id FROM tablaClusters WHERE (nombre_cluster='" + seleccionado + "') AND (nombre_archivo='" + nombre_archivo + "')";
            cursor = baseDatos.rawQuery(ordenSQL, null);
            if (cursor.moveToFirst()) {
                idnum = cursor.getInt(0);
            }
            //si se marcó emitir aviso
            if (check_aviso.isChecked()) {
                //si el mensaje de aviso está vacío
                if ((texto_aviso.getText().toString()).equals("")) {
                    AlertDialog.Builder dialogo_textoaviso = new AlertDialog.Builder(Main3Activity.this);
                    //se muestra mensaje
                    dialogo_textoaviso.setMessage("No es posible guardar un mensaje de aviso vacío.\n¿Desea guardar el mensaje por defecto: \"El modo de funcionamiento actual de la planta es incorrecto\"?").setCancelable(false)
                            .setPositiveButton("SÍ", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //se guarda el mensaje predeterminado
                                    guardar_predeterminado();
                                }
                            })
                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //no se guarda la información
                                    dialog.cancel();
                                }
                            });
                    AlertDialog dialogo_titulo = dialogo_textoaviso.create();
                    dialogo_titulo.setTitle("Mensaje de aviso vacío.");
                    dialogo_titulo.show();
                } else {//si el mensaje de aviso no está vacío
                    //se actualiza la información
                    ordenSQL = "UPDATE tablaClusters SET nombre_cluster='" + texto_nombre.getText().toString() + "',descripcion='" + texto_descripcion.getText().toString() + "', aviso='" + 1 + "',textoaviso='" + texto_aviso.getText().toString() + "' WHERE id='" + idnum + "'";
                    baseDatos.execSQL(ordenSQL);
                    Singleton.guarda("basedatos", baseDatos);
                    //se actualiza la información de los desplegables
                    actualizar_desplegable();
                    //Mensaje de cambios guardados
                    Toast.makeText(this, "Cambios guardados", Toast.LENGTH_LONG).show();
                }
            } else {//si NO se marcó emitir aviso
                //se actualiza la información
                ordenSQL = "UPDATE tablaClusters SET nombre_cluster='" + texto_nombre.getText().toString() + "',descripcion='" + texto_descripcion.getText().toString() + "', aviso='" + 0 + "' WHERE id='" + idnum + "'";
                baseDatos.execSQL(ordenSQL);
                Singleton.guarda("basedatos", baseDatos);
                //se actualiza la información de los desplegables
                actualizar_desplegable();
                //Mensaje de cambios guardados
                Toast.makeText(this, "Cambios guardados", Toast.LENGTH_LONG).show();
            }
        }else{//si el nuevo nombre coincide con el de otro cluster
            AlertDialog.Builder dialogo_aviso = new AlertDialog.Builder(Main3Activity.this);
            //se nuestra mensaje
            dialogo_aviso.setMessage("No es posible guardar la información debido a que dos modos de funcionamiento distintos no pueden tener el mismo nombre.\nCambie el nombre del modo de funcionamiento seleccionado y vuelva a intentarlo.").setCancelable(false)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            AlertDialog dialogo_titulo = dialogo_aviso.create();
            dialogo_titulo.setTitle("Nombre incorrecto");
            dialogo_titulo.show();
        }
    }
    public void CheckBoxAvisoClick(View v) {
        mostrar_aviso();
    }
    private void mostrar_aviso(){
        //si el checkbox está marcado
        if (check_aviso.isChecked()){
            //se muestra el mensaje de aviso
            etiqueta_aviso.setVisibility(View.VISIBLE);
            texto_aviso.setVisibility(View.VISIBLE);
        }else{//si el checkbox NO está marcado
            //se oculta el mensaje de aviso
            etiqueta_aviso.setVisibility(View.INVISIBLE);
            texto_aviso.setVisibility(View.INVISIBLE);
        }
    }
    private void guardar_predeterminado(){
        String ordenSQL = "UPDATE tablaClusters SET nombre_cluster='" + texto_nombre.getText().toString() + "',descripcion='" + texto_descripcion.getText().toString() + "', aviso='" + 1 + "',textoaviso='El modo de funcionamiento actual de la planta es incorrecto' WHERE id='" + idnum + "'";
        baseDatos.execSQL(ordenSQL);
        Singleton.guarda("basedatos", baseDatos);
        actualizar_desplegable();
        //Mensaje de cambios guardados
        Toast.makeText(this, "Cambios guardados", Toast.LENGTH_LONG).show();
    }
    private void actualizar_desplegable(){
        baseDatos =(SQLiteDatabase) Singleton.busca("basedatos");
        //se rellena el desplegable con el nombre de los modos de funcionamiento
        nombre_modos = new ArrayList<String>();
        String ordenSQL = "SELECT nombre_cluster FROM tablaClusters WHERE nombre_archivo ='"+ nombre_archivo+"'";
        Cursor cursor = baseDatos.rawQuery(ordenSQL, null);
        if (cursor.moveToFirst()) {
            do {
                 String nombre = cursor.getString(0);
                 nombre_modos.add(nombre);
            } while (cursor.moveToNext());
        }
        crear_desplegable();
        //se deja seleccionado el modo que se acaba de actualizar
        int posicion = nombre_modos.indexOf(texto_nombre.getText().toString());
        desplegable.setSelection(posicion);
    }
    private void crear_desplegable(){
        //se crea el spinner de los modos de funcionamiento
        ArrayAdapter<String> adaptador = new ArrayAdapter<String>(this,R.layout.spinner_personalizado_gris, nombre_modos);
        adaptador.setDropDownViewResource(R.layout.spinner_personalizado_gris);
        desplegable.setAdapter(adaptador);
    }
}
