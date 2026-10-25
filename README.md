# Jarvis AI - Android Assistant

Asistente autónomo con integración de modelos de lenguaje (Gemini y proveedores universales), automatización en dispositivo y listas compartidas con Firebase Firestore.

---

## 1. Configuración de Firebase y `google-services.json`

El archivo `app/google-services.json` contiene la configuración de Firebase para Authentication y Firestore. Por seguridad, este archivo está excluido del control de versiones (`.gitignore`).

### Cómo obtener `google-services.json`:
1. Ingresa a la [Consola de Firebase](https://console.firebase.google.com/).
2. Selecciona tu proyecto (o crea uno nuevo).
3. Dirígete a **Configuración del proyecto** (icono de engranaje) -> pestaña **General**.
4. En la sección **Tus apps**, selecciona la app de Android con paquete `com.aistudio.jarvisai.anzqdm` (o regístrala si aún no existe).
5. Descarga el archivo `google-services.json`.
6. Colócalo en la ruta local: `app/google-services.json`.

Para configurarlo en **GitHub Actions CI/CD**:
Codifícalo en base64:
```bash
base64 -w 0 app/google-services.json
```
Copia la salida y agrégala como el secret de repositorio `GOOGLE_SERVICES_JSON_BASE64`.

---

## 2. Secretos Requeridos en GitHub Actions

En tu repositorio de GitHub, ve a **Settings** -> **Secrets and variables** -> **Actions** -> **Repository secrets** y crea los siguientes secrets:

| Nombre del Secret | Descripción | Requerido Para |
|-------------------|-------------|----------------|
| `GOOGLE_SERVICES_JSON_BASE64` | Contenido de `app/google-services.json` codificado en base64 (`base64 -w 0 app/google-services.json`). | Builds de Debug y Release en CI |
| `DEBUG_KEYSTORE_BASE64` | Keystore estable de debug codificada en base64 (`base64 -w 0 debug.keystore`) para que las firmas de debug no cambien entre corridas. | Builds y pruebas de Debug |
| `RELEASE_KEYSTORE_BASE64` | Keystore de producción / subida codificada en base64 (`base64 -w 0 release.keystore`). | Builds y tags de Release (`v*`) |
| `STORE_PASSWORD` | Contraseña del almacén de claves (keystore) de release. | Compilación de Release firmada |
| `KEY_PASSWORD` | Contraseña del alias de clave en la keystore de release. | Compilación de Release firmada |
| `KEY_ALIAS` | Alias de la clave dentro de la keystore de release (ej. `upload`). | Compilación de Release firmada |

---

## 3. Despliegue de Reglas de Firestore

Las reglas de seguridad se encuentran en `firestore.rules`.
Para desplegarlas a Firebase:

```bash
firebase deploy --only firestore:rules
```
O con el proyecto explícito:
```bash
firebase deploy --only firestore:rules --project <TU_FIREBASE_PROJECT_ID>
```
