# TFG
Este repositorio recoge el código de la aplicación que desarrollé como trabajo de fin de grado.

La aplicación desarrollada es genérica, de modo que en un futuro podría ser adaptada para ser utilizada en diversas plantas industriales. Su funcionamiento es el siguiente:

A través de la aplicación desarrollada el usuario podrá abrir un archivo en el que se recojan datos que hayan sido obtenidos de la planta, estos se interpretarán y se clasificarán en grupos mediante una técnica de Machine Learning (Clustering). Además la aplicación dará la opción de relacionar los datos de un archivo recién leído con los de uno antiguo.

El usuario podrá renombrar los distintos modos de funcionamiento, añadir una descripción de los mismos e indicar si el modo de funcionamiento es o no correcto. Mediante la pulsación de un botón se guardará un archivo en el dispositivo móvil con los datos de los modos de funcionamiento de la planta.

La aplicación recibirá cada cierto tiempo, los datos de la planta en ese instante (para realizar las pruebas del funcionamiento de la aplicación se generó una comunicación con un autómata que generaba datos en función de la posición de unos interruptores). Se podrá escoger si se quiere que dichos datos sean interpretados a partir de un archivo que fue leído anteriormente o de uno nuevo. Cada vez que se reciban datos la aplicación identificará a que modo de funcionamiento corresponden y lo mostrará en pantalla. Además en caso de que dicho modo no sea adecuado se mostrará un aviso y se enviará un correo electrónico informando de la situación.

El código principal se encuentra ubicado en TFG/app/src/main/java/com/example. La clase EM_modificada es una copia de la clase EM de la biblioteca weka, por lo que no me atribuyo su autoría.
