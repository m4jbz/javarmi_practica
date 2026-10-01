# Sistema de Agenda Personal con Java RMI y Base de Datos

Aplicación distribuida en Java que administra una agenda de contactos (altas, consultas,
modificaciones y bajas) mediante **invocación remota de métodos (RMI)** y acceso a base de
datos con **JDBC**. Un solo servidor atiende a *n* clientes de forma concurrente.

| | |
|---|---|
| **Institución** | Instituto Tecnológico de Iguala |
| **Carrera** | Ingeniería Informática |
| **Asignatura** | IFF-1019 Programación en Ambiente Cliente Servidor |
| **Práctica** | Tema 3 RMI – Práctica 1 |
| **Grupo** | 7 U |
| **Docente** | María del Carmen Urióstegui Peralta |

---

## 1. Contenido del proyecto

```
agenda-rmi/
├── pom.xml                      POM padre (Maven multimódulo, JDK 21)
├── agenda-common/               Interfaz remota, DTO y validaciones (lo que comparten cliente y servidor)
│   └── src/main/java/mx/edu/itigualapa/agenda/common/
│       ├── AgendaRemota.java            interfaz Remote con los métodos CRUD
│       ├── Contacto.java                objeto Serializable que viaja por la red
│       ├── ValidacionException.java     excepción de negocio (revisada y serializable)
│       └── Validador.java               reglas de nombre, teléfono y correo
├── agenda-server/               Servidor RMI
│   └── src/main/
│       ├── java/mx/edu/itigualapa/agenda/server/
│       │   ├── ServidorAgenda.java      main: registro RMI, publicación y cierre ordenado
│       │   ├── AgendaRemotaImpl.java    implementación remota, segura para hilos
│       │   ├── ContactoDAO.java         todo el SQL, con PreparedStatement
│       │   ├── ConexionBD.java          conexión JDBC a SQLite
│       │   ├── EsquemaBD.java           crea la tabla si no existe
│       │   └── AuditoriaLogger.java     bitácora logs/auditoria.log
│       └── resources/db.properties      configuración de la base de datos
├── agenda-client/               Clientes
│   └── src/main/java/mx/edu/itigualapa/agenda/client/
│       ├── ClienteConsola.java          cliente de texto con menú
│       ├── ClienteFX.java               cliente gráfico (JavaFX)
│       ├── Launcher.java                main que no extiende Application
│       └── PruebaConcurrencia.java      N clientes simultáneos + verificación de la BD
├── db/                          schema_sqlite.sql, datos_prueba.sql
├── scripts/                     lanzadores .bat (Windows) y .sh (Linux / WSL)
└── logs/                        auditoria.log (se genera al ejecutar)
```

---

## 2. Requisitos

| Requisito | Versión usada | Comprobación |
|---|---|---|
| JDK (LTS) | Temurin **21.0.12** | `java -version` |
| Apache Maven | **3.9.16** | `mvn -v` |
| SQLite | driver `org.xerial:sqlite-jdbc` **3.49.1.0** (incluido) | no requiere instalación |
| JavaFX | `org.openjfx` **21.0.7** (se descarga con Maven) | – |

### Instalación en Windows 11

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
```

Maven no está en el catálogo de winget. Se instala descomprimiendo el binario oficial:

```powershell
$dest = "$env:USERPROFILE\tools"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Invoke-WebRequest -Uri "https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip" `
                  -OutFile "$env:TEMP\maven.zip"
Expand-Archive "$env:TEMP\maven.zip" -DestinationPath $dest -Force

[Environment]::SetEnvironmentVariable('JAVA_HOME','C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot','User')
[Environment]::SetEnvironmentVariable('Path',
    [Environment]::GetEnvironmentVariable('Path','User') + ";$dest\apache-maven-3.9.16\bin",'User')
```

Cierra y vuelve a abrir la terminal y comprueba:

```powershell
java -version
mvn -v
```

> **Aviso.** Si Java estuvo instalado antes y se desinstaló, puede quedar la carpeta
> `C:\Program Files\Common Files\Oracle\Java\javapath` en el `PATH` con accesos rotos que
> hacen fallar el comando `java` con «no se encontró». Si ocurre, borra esa entrada
> del `PATH` del sistema o asegúrate de que la carpeta `bin` del JDK aparezca antes.

### Instalación en Ubuntu / WSL 2

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven sqlite3
java -version && mvn -v
```

---

## 3. Compilación

Desde la carpeta `agenda-rmi`:

```bash
mvn clean package
```

Se generan dos ejecutables autocontenidos (incluyen sus dependencias):

* `agenda-server/target/agenda-server.jar`
* `agenda-client/target/agenda-client.jar`

---

## 4. Base de datos

El sistema usa **SQLite** y **no requiere instalar ningún servidor**: el archivo
`db/agenda.db` se crea solo la primera vez que arranca el servidor.

Para crear la base desde el script oficial y cargar los datos de prueba (opcional, requiere
el ejecutable `sqlite3`):

```bash
scripts/crear-bd.sh          # Linux / WSL
scripts\crear-bd.bat         # Windows
```

o a mano:

```bash
sqlite3 db/agenda.db < db/schema_sqlite.sql
sqlite3 db/agenda.db < db/datos_prueba.sql
```

También puedes apuntar a otro archivo de configuración sin modificar el jar:

```bash
java -Dagenda.db.config=/ruta/a/mi-db.properties -jar agenda-server/target/agenda-server.jar
```

---

## 5. Ejecución

### 5.1 Todo en la misma computadora

**Terminal 1 — servidor**

```bash
scripts/iniciar-servidor.sh          # Linux / WSL
scripts\iniciar-servidor.bat         # Windows
```

Equivale a `java -jar agenda-server/target/agenda-server.jar 1099 1100 127.0.0.1`.
El servidor queda escuchando y va imprimiendo cada operación con la hora, el host del
cliente, el hilo que la atendió y el resultado.

**Terminales 2, 3, … — un cliente de consola por terminal**

```bash
scripts/iniciar-cliente-consola.sh localhost 1099 cliente-A
scripts/iniciar-cliente-consola.sh localhost 1099 cliente-B
```

```bat
scripts\iniciar-cliente-consola.bat localhost 1099 cliente-A
scripts\iniciar-cliente-consola.bat localhost 1099 cliente-B
```

**Cliente gráfico (JavaFX)**

```bash
scripts/iniciar-cliente-fx.sh localhost 1099
```

```bat
scripts\iniciar-cliente-fx.bat localhost 1099
```

Alternativa desde Maven, que descarga los binarios de JavaFX de la plataforma actual:

```bash
mvn -pl agenda-client javafx:run
```

**Prueba de concurrencia (10 clientes simultáneos por omisión)**

```bash
scripts/prueba-concurrencia.sh localhost 1099 10
```

```bat
scripts\prueba-concurrencia.bat localhost 1099 10
```

### 5.2 Servidor y clientes en equipos distintos

1. **En el servidor**, averigua su dirección en la red:

   ```powershell
   ipconfig          # Windows: busca "Dirección IPv4" de tu adaptador
   ```

   ```bash
   ip -4 addr show   # Linux
   ```

2. **Arranca el servidor anunciando esa IP.** Esto es obligatorio: el *stub* que RMI
   entrega al cliente lleva dentro la dirección del servidor, y si no se fija, Java la
   deduce sola y puede anunciar una dirección equivocada.

   ```bat
   scripts\iniciar-servidor.bat 1099 1100 192.168.3.191
   ```

   Equivale a:

   ```bash
   java -Djava.rmi.server.hostname=192.168.3.191 -jar agenda-server/target/agenda-server.jar
   ```

3. **Abre los dos puertos en el cortafuegos del servidor** (1099 para el registro y 1100
   para el objeto remoto). En Windows, en PowerShell como administrador:

   ```powershell
   New-NetFirewallRule -DisplayName "RMI Agenda 1099" -Direction Inbound -Protocol TCP -LocalPort 1099 -Action Allow
   New-NetFirewallRule -DisplayName "RMI Agenda 1100" -Direction Inbound -Protocol TCP -LocalPort 1100 -Action Allow
   ```

   En Linux con `ufw`:

   ```bash
   sudo ufw allow 1099/tcp
   sudo ufw allow 1100/tcp
   ```

4. **En cada equipo cliente**, indica la IP del servidor:

   ```bat
   scripts\iniciar-cliente-consola.bat 192.168.3.191 1099 cliente-lab-3
   scripts\iniciar-cliente-fx.bat 192.168.3.191 1099
   ```

### 5.3 Usar el comando `rmiregistry` externo

El servidor crea el registro dentro de su propia máquina virtual, así que normalmente no
hace falta. Si la práctica pide arrancarlo por separado, el punto importante es que
`rmiregistry` **debe tener en su classpath la interfaz remota**; de lo contrario falla al
publicar el servicio con `ClassNotFoundException`.

```bat
:: Terminal 1 (desde la carpeta agenda-rmi)
rmiregistry -J-Djava.class.path=agenda-common\target\agenda-common-1.0.0.jar 1099
```

```bash
# Terminal 1 (desde la carpeta agenda-rmi)
rmiregistry -J-Djava.class.path=agenda-common/target/agenda-common-1.0.0.jar 1099 &
```

Después arranca el servidor como siempre: detecta que ya hay un registro activo en el
puerto 1099 y lo reutiliza, e imprime
`[RMI] Se reutiliza el registro que ya escucha en el puerto 1099.`

---

## 6. Uso del cliente de consola

```
  1) Agregar contacto
  2) Buscar contacto (por id o por nombre)
  3) Listar todos los contactos
  4) Actualizar contacto
  5) Eliminar contacto
  0) Salir
```

* En **Actualizar**, dejar un campo vacío conserva el valor actual.
* **Eliminar** muestra el contacto y pide confirmación (`s` / `n`) antes de borrar.

Reglas de validación (las aplica el cliente para avisar de inmediato y el servidor como
fuente de verdad):

| Campo | Regla |
|---|---|
| `nombre` | obligatorio, de 2 a 100 caracteres, solo letras (con acentos y ñ) y espacios |
| `telefono` | exactamente 10 dígitos |
| `email` | formato válido y **único** en la base de datos |

---

## 7. Auditoría

Cada operación se escribe en `logs/auditoria.log` con el formato:

```
fecha-hora | cliente | hilo | operación | parámetros | resultado
```

Ejemplo real:

```
2026-09-29 10:12:08.402 | 127.0.0.1 | RMI TCP Connection(2)-127.0.0.1 | agregarContacto | contacto=Ana Laura Mendoza Rios/7335551122/ana.mendoza@itiguala.edu.mx | OK - id=1
2026-09-29 10:12:08.912 | 127.0.0.1 | RMI TCP Connection(4)-127.0.0.1 | agregarContacto | contacto=Sofia Guadalupe Torres Lopez/7332233445/ana.mendoza@itiguala.edu.mx | ERROR - El correo electronico 'ana.mendoza@itiguala.edu.mx' ya esta registrado en la agenda (contacto id 1).
```

---

## 8. Solución de los problemas comunes de RMI

| Síntoma | Causa | Solución |
|---|---|---|
| `ConnectException: Connection refused to host: <IP>` | El *stub* lleva una IP que el cliente no puede alcanzar. Pasa, por ejemplo, cuando el servidor arrancó con una IP de Wi-Fi y ésta cambió después. | Arranca el servidor fijando la dirección: tercer argumento del script, o `-Djava.rmi.server.hostname=<IP>`. Para trabajar en una sola PC, usa `127.0.0.1`. |
| `ConnectException: Connection timed out` | El puerto 1100 (objeto remoto) está cerrado en el cortafuegos, aunque el 1099 esté abierto. | Abre **los dos** puertos, 1099 y 1100 (ver 5.2). El objeto se exporta en un puerto fijo justo para poder hacerlo. |
| `NotBoundException: AgendaPersonal` | El registro responde pero el servicio aún no se publicó. | Espera a que el servidor imprima `Servidor listo` y vuelve a lanzar el cliente. |
| `java.rmi.ServerException` con `ClassNotFoundException` | Se usó un `rmiregistry` externo sin la interfaz remota en su classpath. | Arráncalo con `-J-Djava.class.path=agenda-common/target/agenda-common-1.0.0.jar` (ver 5.3). |
| `Port already in use` / `ExportException` al arrancar | Quedó un servidor anterior ocupando el 1099 o el 1100. | Windows: `netstat -ano | findstr :1099` y `taskkill /PID <pid> /F`. Linux: `ss -ltnp | grep 1099` y `kill <pid>`. |
| `SQLITE_BUSY: database is locked` | Dos escrituras al mismo tiempo sin margen de espera. | Ya está resuelto: las conexiones activan `PRAGMA journal_mode=WAL` y `PRAGMA busy_timeout`, configurable en `db.properties` con `db.busy.timeout.ms`. |
| `JavaFX runtime components are missing` | Se intentó ejecutar como clase principal una que extiende `Application`. | Usa `mx.edu.itigualapa.agenda.client.Launcher` (lo hacen ya los scripts) o `mvn -pl agenda-client javafx:run`. |
| `UnmarshalException` / `InvalidClassException` | Cliente y servidor se compilaron con versiones distintas de `Contacto`. | Distribuye el mismo `agenda-client.jar` que corresponda al servidor. `Contacto` declara un `serialVersionUID` fijo para reducir el problema. |
| La consola de Windows muestra `?` en lugar de acentos | Página de códigos antigua. | Ejecuta `chcp 65001` antes de lanzar el cliente. |

---

## 9. Comandos de referencia rápida

```bash
mvn clean package                                     # compilar todo
scripts/iniciar-servidor.sh                           # servidor (local)
scripts/iniciar-servidor.sh 1099 1100 192.168.3.191   # servidor (en red)
scripts/iniciar-cliente-consola.sh localhost 1099 cliente-A
scripts/iniciar-cliente-fx.sh localhost 1099
scripts/prueba-concurrencia.sh localhost 1099 10
tail -f logs/auditoria.log                            # ver la auditoría en vivo
sqlite3 db/agenda.db "SELECT * FROM Contactos;"       # revisar la base de datos
```
