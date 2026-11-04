# Publicar Despierta en Google Play

Cómo se firma el AAB de Despierta y cómo se sube a las pruebas cerradas. Este repositorio es
público: **aquí no hay claves ni contraseñas**, solo la huella pública del certificado.

## Las dos claves (y cuál es tuya)

| Clave | Quién la tiene | Para qué |
|---|---|---|
| **Clave de firma de la app** | Google (Play App Signing) | Firma la app que se instala en los teléfonos. No se toca nunca. |
| **Clave de subida** | Tú (y una copia en los secretos de GitHub) | Firma el AAB que subes. Google comprueba que viene de ti. |

La clave de subida actual: alias `despierta-upload`, PKCS12, RSA 4096, válida hasta 2054.
Huella SHA-256 del certificado:

```
C6:03:44:32:7C:B5:8F:3C:6D:FE:DA:56:02:13:CA:EC:6B:D0:70:84:02:B9:EC:B0:19:DE:69:CD:20:9C:64:54
```

## 1. Antes de subir nada: comprobar Play Console

Play Console → Despierta → **Prueba y lanzamiento → Configuración → Integridad de la app →
Firma de apps**.

- **Si aparece un "Certificado de clave de subida"** con una huella SHA-256: compárala con la de
  arriba. Si es **igual**, todo listo. Si es **distinta**, no subas este AAB: Play ya tiene otra
  clave de subida registrada (hay que usar esa, o pedir a Google el restablecimiento de la clave de
  subida desde esa misma pantalla).
- **Si todavía no aparece ninguna clave de subida** (nunca se subió un AAB): el primer AAB firmado
  que subas registra su certificado como clave de subida. Este flujo firma con la clave de arriba.

La clave de firma de la app que gestiona Google no se cambia en ningún caso.

## 2. Copia de recuperación de la clave de subida

Si pierdes la clave de subida no pierdes la app (Google conserva la clave de firma), pero tendrás que
pedir un restablecimiento y esperar a que Google lo apruebe. Guarda **dos copias** en sitios
distintos, por ejemplo un USB y tu Google Drive:

- el archivo `despierta-upload.jks`
- la contraseña (en un gestor de contraseñas o en papel, **no** en el mismo sitio que el archivo)

### Generar una clave propia (opcional, solo si Play todavía no tiene clave de subida)

En tu computadora, con Java instalado (`keytool` viene con Java o Android Studio):

```bash
keytool -genkeypair -v -storetype PKCS12 -keystore despierta-upload.jks \
  -alias despierta-upload -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Gabriel Villarreta, O=Blue Lab Studios, C=PA"
keytool -list -v -keystore despierta-upload.jks -alias despierta-upload   # muestra la huella SHA-256
base64 -w0 despierta-upload.jks > despierta-upload.jks.base64.txt          # en Windows: certutil -encode
```

Después: actualiza los cuatro secretos (paso 3) y pon la nueva huella en
`.github/play-release.env` (`UPLOAD_CERT_SHA256`) mediante un PR.

## 3. Secretos de GitHub

Repositorio → **Settings → Secrets and variables → Actions → Repository secrets**:

| Secreto | Valor |
|---|---|
| `DESPIERTA_UPLOAD_KEYSTORE_BASE64` | contenido completo de `despierta-upload.jks.base64.txt` |
| `DESPIERTA_UPLOAD_STORE_PASSWORD` | contraseña del almacén |
| `DESPIERTA_UPLOAD_KEY_PASSWORD` | contraseña de la clave (en PKCS12 es la misma) |
| `DESPIERTA_UPLOAD_KEY_ALIAS` | `despierta-upload` |

Solo los usa `.github/workflows/play-aab.yml`. La clave se escribe en el disco temporal del
ejecutor, se borra al terminar la compilación y las contraseñas quedan ocultas en los registros.

## 4. Generar el AAB firmado

1. En `.github/play-release.env` están el commit exacto, el paquete, la versión y la huella esperada.
2. Al hacer push de ese archivo a una rama `release/**` o `firma/**` se ejecuta
   **"AAB firmado para Google Play"** (también a mano, desde la pestaña Actions, cuando el flujo
   esté en la rama principal).
3. El flujo **falla** si algo no coincide: firma inválida, otra huella de certificado, otro paquete,
   otro versionCode/versionName, AAB depurable o un commit distinto grabado en la app.
4. Descarga el artefacto `Despierta-<versión>-<versionCode>-play-firmado` (un .zip). Dentro:
   - `Despierta-<versión>-<versionCode>-play-firmado.aab` ← **este se sube a Play**
   - `VERIFICACION.txt` con el resultado de las comprobaciones
   - `CAMBIOS.md`

El `Despierta-*-unsigned.aab` del flujo "Generar APK" **no sirve para Play**: no está firmado.

## 5. Subir a pruebas cerradas

Play Console → Despierta → **Prueba y lanzamiento → Pruebas → Prueba cerrada** → crear o elegir
una pista → **Crear versión** → subir el `.aab` → notas de la versión → **Revisar versión** →
**Iniciar lanzamiento**.

Cada AAB subido necesita un `versionCode` mayor que el anterior: la próxima versión será 13.

## Versiones futuras

1. Sube `versionCode` y `versionName` en `app/build.gradle.kts` y añade la entrada en `CHANGELOG.md`.
2. En un PR, actualiza `.github/play-release.env` (commit, versión, versionCode).
3. Descarga el artefacto firmado de ese flujo y súbelo a Play.
