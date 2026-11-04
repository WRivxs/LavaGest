# LavaGest PRO 🚗🏍️

**Sistema de Gestión Operativa y Financiera para Lavaderos de Carros y Motos**  
*Proyecto de Aula - Ingeniería del Software*

---

## 📋 Descripción del Proyecto

**LavaGest PRO** es una solución de escritorio desarrollada en **Java** diseñada para digitalizar y automatizar el control operativo y la liquidación financiera en centros de lavado vehicular (autolavados). 

Resuelve problemas comunes como la fuga de dinero en caja, el cálculo manual de nómina por comisiones a operarios y la falta de métricas de rendimiento en tiempo real.

---

## 🏛️ Arquitectura del Software

El sistema sigue una **Arquitectura en Capas (Layered Architecture)** con separación estricta de responsabilidades bajo principios de Ingeniería del Software (GRASP, SOLID, MVC):

```
com.lavagest
├── config/       # Contenedor de dependencias (IoC ligero) y Pool de conexiones JDBC
├── controller/   # Controladores que exponen la lógica a la vista mediante DTOs
├── dto/          # Data Transfer Objects para desacoplar datos de la persistencia
├── model/        # Entidades del dominio (Wash, Worker, ServiceRate, WorkerPayout)
├── repository/   # Patrón Repositorio (Interfaces, JDBC Supabase y Fallback en Memoria)
├── service/      # Reglas de negocio y cálculos financieros (Ingresos - Comisiones = Ganancia)
└── ui/           # Interfaz gráfica moderna con FlatLaf Look & Feel y SwingWorker
```

### Modelo de Desarrollo
* **Modelo Iterativo e Incremental (Ágil):** Desarrollo evolutivo en ciclos cortos:
  1. *Fase 1:* Modelado del dominio y lógica de comisiones.
  2. *Fase 2:* Diseño de base de datos relacional y vistas analíticas en PostgreSQL.
  3. *Fase 3:* Persistencia remota en la nube (Supabase Pooler).
  4. *Fase 4:* Interfaz gráfica Swing con aceleración por hardware y optimización de latencia.

---

## ⚙️ Tecnologías Utilizadas

- **Lenguaje:** Java (Compatible con JDK 8 hasta JDK 25)
- **Gestor de Construcción:** Apache Maven
- **Interfaz Gráfica:** Java Swing con Look & Feel moderno **FlatLaf 3.4.1**
- **Base de Datos:** PostgreSQL en la nube mediante **Supabase** (Transaction Pooler PgBouncer)
- **Conector:** Driver oficial `org.postgresql:postgresql:42.7.2`

---

## 🚀 Funcionalidades Principales

1. **Dashboard Operativo en Tiempo Real:**
   - Balance automático de caja: *Ingresos Totales — Egresos (Comisiones) = Ganancia Libre*.
   - Ratios operativos en vivo (proporción de Carros vs Motos atendidos).
   - Gráfica semanal interactiva de ingresos (últimos 7 días).
   - Monitor de operadores activos en turno.
   - Tabla de últimos lavados registrados con scroll optimizado.

2. **Modal de Registro Rápido ("Modo Dispatcher"):**
   - Alternador rápido de tipo de vehículo (Carro / Moto) con tarifas dinámicas.
   - Entrada de placa vehicular con auto-capitalización.
   - Asignación de operario mediante lista de selección con checks exclusivos.
   - Preview financiero instantáneo del ticket antes de guardar.

3. **Módulos de Gestión:**
   - **Operadores:** Registro, asignación de bahías/turnos y control de estado activo/inactivo.
   - **Historial de Lavados:** Auditoría completa de servicios realizados.
   - **Nómina y Liquidación:** Control de pagos de comisiones pendientes y desembolsadas.
   - **Reportes Financieros:** Reportes diarios y mensuales de rentabilidad.

---

## 🛠️ Configuración y Puesta en Marcha

### Prerrequisitos
- JDK instalado (Java 8 o superior).
- Maven instalado (o configurado en el sistema).

### 1. Variables de Entorno (`.env`)
El proyecto incluye un archivo de plantilla `.env.example`. Copia o renómbralo a `.env` con las credenciales de conexión:

```env
DB_URL=jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0&tcpKeepAlive=true
DB_USER=postgres.bjskevrbjzjceorecbzf
DB_PASSWORD=tu_contrasena_aqui
```

### 2. Base de Datos
El script DDL completo con tablas, disparadores (triggers), índices y vistas analíticas se encuentra en:
📁 `database/schema.sql`

### 3. Ejecución

* **Windows (Doble clic):**  
  Ejecutar el archivo `run.bat` o `run.ps1` (detecta el JDK automáticamente y activa aceleración Direct3D).

* **Línea de Comandos (Maven):**
  ```bash
  mvn clean compile exec:java
  ```
