# Résolution du problème Gradle / Java

## Problème rencontré

```
FAILURE: Build failed with an exception.
* What went wrong:
Could not open cp_settings generic class cache for settings file
> BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_'
Unsupported class file major version 65
```

## Cause

Votre version de Java (Java 21, class file version 65) est trop récente pour l'ancienne version de Gradle (7.5).

## ✅ Solution (3 options)

### Option 1 : Nettoyer le cache Gradle (RECOMMANDÉ)

J'ai mis à jour tous les fichiers Gradle pour utiliser Gradle 8.5 qui est compatible avec Java 21.

**Exécutez ces commandes dans PowerShell :**

```powershell
# Nettoyer complètement le projet
flutter clean

# Supprimer le cache Gradle
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches" -ErrorAction SilentlyContinue

# Réinstaller les dépendances
flutter pub get

# Relancer l'application
flutter run
```

### Option 2 : Forcer la régénération du wrapper Gradle

Si l'option 1 ne fonctionne pas :

```powershell
# Aller dans le dossier android
cd android

# Supprimer les fichiers de cache
Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue

# Retourner à la racine
cd ..

# Nettoyer et relancer
flutter clean
flutter pub get
flutter run
```

### Option 3 : Utiliser une version Java compatible

Si les options précédentes ne fonctionnent pas, utilisez Java 17 (LTS) au lieu de Java 21 :

1. **Téléchargez Java 17 :**
   - Allez sur : https://adoptium.net/
   - Téléchargez Java 17 (LTS)
   - Installez-le

2. **Configurez Android Studio pour utiliser Java 17 :**
   - Ouvrez Android Studio
   - File > Settings (ou Ctrl+Alt+S)
   - Build, Execution, Deployment > Build Tools > Gradle
   - Gradle JDK : Sélectionnez Java 17
   - Cliquez sur OK

3. **Relancez Flutter :**
   ```powershell
   flutter clean
   flutter pub get
   flutter run
   ```

## Vérifier la version de Java utilisée

```powershell
# Vérifier la version Java système
java -version

# Vérifier la version Java utilisée par Flutter
flutter doctor --verbose
```

## Fichiers mis à jour

J'ai automatiquement mis à jour ces fichiers avec les bonnes versions :

1. ✅ `android/gradle/wrapper/gradle-wrapper.properties` → Gradle 8.5
2. ✅ `android/settings.gradle` → Android Gradle Plugin 8.1.0
3. ✅ `android/gradlew` et `android/gradlew.bat` → Scripts wrapper

## Tableau de compatibilité Java / Gradle

| Java Version | Gradle Version | Android Gradle Plugin |
|--------------|----------------|----------------------|
| Java 17      | 7.3+           | 7.2+                 |
| Java 21      | 8.5+           | 8.1+                 |

## Après la résolution

Une fois le problème résolu, vous devriez voir :

```
Launching lib\main.dart on Pixel 8 Pro in debug mode...
Running Gradle task 'assembleDebug'...
✓ Built build\app\outputs\flutter-apk\app-debug.apk
Installing build\app\outputs\flutter-apk\app-debug.apk...
```

## Problèmes persistants ?

Si le problème persiste après avoir essayé toutes les options :

1. **Fermez Android Studio** complètement
2. **Supprimez tous les caches :**
   ```powershell
   Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle"
   Remove-Item -Recurse -Force "android\.gradle"
   Remove-Item -Recurse -Force "android\build"
   flutter clean
   ```
3. **Redémarrez votre PC**
4. **Relancez :**
   ```powershell
   flutter pub get
   flutter run
   ```

## Commandes de diagnostic

Si vous voulez diagnostiquer le problème :

```powershell
# Vérifier la configuration Flutter
flutter doctor -v

# Vérifier la version Gradle
cd android
.\gradlew --version
cd ..

# Voir les logs détaillés
flutter run --verbose
```

---

**En résumé : Exécutez l'Option 1 et le problème devrait être résolu ! 🚀**
