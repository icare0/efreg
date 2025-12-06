# Configuration Complète - POC Flutter GPS & Calendar
## Guide pas-à-pas pour Windows

Ce guide vous accompagne étape par étape pour configurer entièrement le projet Flutter avec Google Calendar et Microsoft Calendar.

---

## 📌 Sommaire

1. [Récupérer le SHA-1 Android (Windows)](#1-récupérer-le-sha-1-android-windows)
2. [Configuration Google Cloud Console](#2-configuration-google-cloud-console)
3. [Configuration Azure (Microsoft)](#3-configuration-azure-microsoft)
4. [Configuration du projet Flutter](#4-configuration-du-projet-flutter)
5. [Lancer l'application](#5-lancer-lapplication)
6. [Résolution de problèmes](#6-résolution-de-problèmes)

---

## 1. Récupérer le SHA-1 Android (Windows)

### Problème sur Windows

Sur Windows, le keystore Android n'est PAS à `~/.android/debug.keystore` mais à :
```
C:\Users\<VOTRE_NOM_UTILISATEUR>\.android\debug.keystore
```

### Étape 1.1 : Vérifier que le keystore existe

Ouvrez PowerShell et exécutez :

```powershell
# Vérifier si le fichier existe
Test-Path "$env:USERPROFILE\.android\debug.keystore"
```

**Résultat attendu :** `True`

**Si vous voyez `False`**, cela signifie que le keystore n'existe pas encore. Passez à l'étape 1.2.

### Étape 1.2 : Créer le keystore de debug (si nécessaire)

Si le keystore n'existe pas, créez-le :

```powershell
# Créer le dossier .android s'il n'existe pas
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.android"

# Créer le keystore de debug
keytool -genkey -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000
```

**Remplissez les informations demandées** (nom, organisation, etc.) ou appuyez sur Entrée pour laisser vide.

### Étape 1.3 : Récupérer le SHA-1

**COMMANDE CORRIGÉE POUR WINDOWS :**

```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**Résultat attendu :**

```
Alias name: androiddebugkey
Creation date: ...
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Android Debug, O=Android, C=US
Issuer: CN=Android Debug, O=Android, C=US
Serial number: ...
Valid from: ... until: ...
Certificate fingerprints:
         SHA1: 4A:BC:5F:E3:2D:1A:B9:C7:8E:4F:2D:5A:6C:7B:8D:9E:0F:1A:2B:3C
         SHA256: ...
```

### Étape 1.4 : Copier votre SHA-1

**IMPORTANT :** Copiez la valeur après `SHA1:` (exemple : `4A:BC:5F:E3:2D:1A:B9:C7:8E:4F:2D:5A:6C:7B:8D:9E:0F:1A:2B:3C`)

⚠️ **Ne partagez JAMAIS ce SHA-1 publiquement !**

### Étape 1.5 : Alternative - Récupérer le SHA-1 depuis Android Studio

Si la commande keytool ne fonctionne pas :

1. Ouvrez Android Studio
2. Ouvrez votre projet Flutter
3. Cliquez sur **Gradle** (panneau de droite)
4. Naviguez vers : `android` > `Tasks` > `android` > `signingReport`
5. Double-cliquez sur `signingReport`
6. Le SHA-1 s'affiche dans la console

---

## 2. Configuration Google Cloud Console

### Étape 2.1 : Créer un projet Google Cloud

1. Allez sur : **https://console.cloud.google.com/**
2. Connectez-vous avec votre compte Google
3. En haut de la page, cliquez sur **"Sélectionner un projet"**
4. Cliquez sur **"NOUVEAU PROJET"**
5. Remplissez :
   - **Nom du projet** : `flutter-gps-calendar-poc` (ou autre nom)
   - **Organisation** : Laissez par défaut
6. Cliquez sur **"CRÉER"**
7. Attendez quelques secondes
8. Sélectionnez votre nouveau projet

### Étape 2.2 : Activer l'API Google Calendar

1. Dans le menu de gauche, cliquez sur **"APIs & Services"** > **"Bibliothèque"**
2. Dans la barre de recherche, tapez : `Google Calendar API`
3. Cliquez sur **"Google Calendar API"**
4. Cliquez sur **"ACTIVER"**
5. Attendez quelques secondes

### Étape 2.3 : Configurer l'écran de consentement OAuth

1. Menu de gauche : **"APIs & Services"** > **"Écran de consentement OAuth"**
2. Sélectionnez **"Externe"** (External)
3. Cliquez sur **"CRÉER"**
4. Remplissez les informations :
   - **Nom de l'application** : `Flutter GPS Calendar POC`
   - **E-mail d'assistance utilisateur** : Votre email
   - **Domaine de l'application** : Laissez vide
   - **E-mail du développeur** : Votre email
5. Cliquez sur **"ENREGISTRER ET CONTINUER"**
6. **Portées (Scopes)** : Cliquez sur **"AJOUTER OU SUPPRIMER DES PORTÉES"**
   - Cherchez et cochez : `Google Calendar API` > `.../auth/calendar.readonly`
   - Cliquez sur **"METTRE À JOUR"**
7. Cliquez sur **"ENREGISTRER ET CONTINUER"**
8. **Utilisateurs test** : Cliquez sur **"ADD USERS"**
   - Ajoutez votre adresse email Google
   - Cliquez sur **"AJOUTER"**
9. Cliquez sur **"ENREGISTRER ET CONTINUER"**
10. Cliquez sur **"RETOUR AU TABLEAU DE BORD"**

### Étape 2.4 : Créer les identifiants OAuth 2.0 pour Android

1. Menu de gauche : **"APIs & Services"** > **"Identifiants"**
2. En haut, cliquez sur **"+ CRÉER DES IDENTIFIANTS"**
3. Sélectionnez **"ID client OAuth"**
4. **Type d'application** : Sélectionnez **"Android"**
5. Remplissez :
   - **Nom** : `Flutter GPS Calendar POC Android`
   - **Nom du package** : `com.example.flutter_gps_calendar_poc`
   - **Empreinte numérique du certificat SHA-1** : Collez votre SHA-1 (de l'étape 1.4)
6. Cliquez sur **"CRÉER"**
7. Cliquez sur **"OK"** dans la popup

### Étape 2.5 : Créer un ID client Web (IMPORTANT pour Google Sign-In)

Google Sign-In sur Android nécessite AUSSI un ID client Web :

1. Toujours dans **"Identifiants"**, cliquez sur **"+ CRÉER DES IDENTIFIANTS"**
2. Sélectionnez **"ID client OAuth"**
3. **Type d'application** : Sélectionnez **"Application Web"**
4. **Nom** : `Flutter GPS Calendar POC Web`
5. **URI de redirection autorisés** : Laissez vide
6. Cliquez sur **"CRÉER"**
7. **IMPORTANT** : Copiez l'**ID client** qui s'affiche (format : `123456789-abc.apps.googleusercontent.com`)
8. Cliquez sur **"OK"**

### Étape 2.6 : (Optionnel) Créer les identifiants iOS

**Si vous testez sur iOS** (nécessite macOS) :

1. **"+ CRÉER DES IDENTIFIANTS"** > **"ID client OAuth"**
2. **Type d'application** : **"iOS"**
3. Remplissez :
   - **Nom** : `Flutter GPS Calendar POC iOS`
   - **ID de bundle** : `com.example.flutterGpsCalendarPoc`
4. Cliquez sur **"CRÉER"**

### Étape 2.7 : Vérifier la configuration

Dans **"Identifiants"**, vous devriez voir :
- ✅ 1 ID client OAuth Android
- ✅ 1 ID client OAuth Application Web
- ✅ (Optionnel) 1 ID client OAuth iOS

**Google Calendar est maintenant configuré ! ✅**

---

## 3. Configuration Azure (Microsoft)

### Étape 3.1 : Créer un compte Azure

1. Allez sur : **https://portal.azure.com/**
2. Connectez-vous avec votre compte Microsoft
   - Si vous n'avez pas de compte, créez-en un (gratuit)
3. Attendez que le portail Azure se charge

### Étape 3.2 : Enregistrer une application

1. Dans la barre de recherche en haut, tapez : `Azure Active Directory`
2. Cliquez sur **"Azure Active Directory"**
3. Dans le menu de gauche, cliquez sur **"Inscriptions d'applications"** (App registrations)
4. Cliquez sur **"+ Nouvelle inscription"** (New registration)

### Étape 3.3 : Remplir le formulaire d'inscription

Remplissez les informations suivantes :

**1. Nom de l'application :**
```
Flutter GPS Calendar POC
```

**2. Types de comptes pris en charge :**
Sélectionnez : **"Comptes dans un annuaire d'organisation et comptes personnels Microsoft"**
(Accounts in any organizational directory and personal Microsoft accounts)

**3. URI de redirection (Redirect URI) :**
- **Plateforme** : Sélectionnez **"Client public/natif (mobile et Bureau)"** (Public client/native)
- **URI de redirection** :
```
msauth://flutter_gps_calendar_poc/auth
```

**4. Cliquez sur "Inscrire" (Register)**

### Étape 3.4 : Copier l'ID d'application (Client ID)

Après l'inscription, vous arrivez sur la page de votre application.

**IMPORTANT - Copiez ces informations :**

1. **ID d'application (client)** (Application (client) ID)
   - Format : `12345678-1234-1234-1234-123456789abc`
   - **⚠️ Gardez cette valeur, vous en aurez besoin !**

2. **ID de l'annuaire (locataire)** (Directory (tenant) ID)
   - Vous pouvez utiliser `common` pour tous les comptes Microsoft

### Étape 3.5 : Configurer les permissions API

1. Dans le menu de gauche de votre application, cliquez sur **"Autorisations de l'API"** (API permissions)
2. Vous devriez voir déjà `User.Read` (permission par défaut)
3. Cliquez sur **"+ Ajouter une autorisation"** (Add a permission)
4. Sélectionnez **"Microsoft Graph"**
5. Sélectionnez **"Autorisations déléguées"** (Delegated permissions)
6. Cochez les permissions suivantes :
   - ✅ **`Calendars.Read`** (Cherchez "Calendars" dans la barre de recherche)
   - ✅ **`offline_access`** (pour le refresh token)
7. Cliquez sur **"Ajouter des autorisations"** (Add permissions)

### Étape 3.6 : Vérifier les permissions

Dans **"Autorisations de l'API"**, vous devriez voir :
- ✅ `User.Read` (Microsoft Graph)
- ✅ `Calendars.Read` (Microsoft Graph)
- ✅ `offline_access` (Microsoft Graph)

**Statut** : "Non accordé" (Not granted) - C'est normal, les permissions seront demandées lors de la connexion.

### Étape 3.7 : (Optionnel) Configurer l'authentification mobile

Si vous avez des problèmes de redirection :

1. Menu de gauche : **"Authentification"** (Authentication)
2. Vérifiez que votre URI de redirection est bien là : `msauth://flutter_gps_calendar_poc/auth`
3. Descendez vers **"Paramètres avancés"** (Advanced settings)
4. **"Autoriser les flux de clients publics"** : Mettez sur **"Oui"**
5. Cliquez sur **"Enregistrer"** en haut

### Étape 3.8 : Résumé des informations à garder

Vous aurez besoin de :

**ID d'application (Client ID)** : `12345678-1234-1234-1234-123456789abc`

**Microsoft Calendar est maintenant configuré ! ✅**

---

## 4. Configuration du projet Flutter

### Étape 4.1 : Ouvrir le projet

```powershell
cd C:\Users\antoi\Github\efreg
code .
```

Ou ouvrez le dossier dans votre éditeur préféré.

### Étape 4.2 : Configurer le Client ID Microsoft

1. Ouvrez le fichier : **`lib/services/microsoft_calendar_service.dart`**
2. Ligne 33, remplacez :

**AVANT :**
```dart
static const String _clientId = 'YOUR_MICROSOFT_CLIENT_ID';
```

**APRÈS :**
```dart
static const String _clientId = '12345678-1234-1234-1234-123456789abc'; // Remplacez par VOTRE Client ID
```

3. Enregistrez le fichier (**Ctrl+S**)

### Étape 4.3 : Installer les dépendances Flutter

```powershell
flutter pub get
```

**Résultat attendu :**
```
Running "flutter pub get" in efreg...
Resolving dependencies...
+ geolocator 12.0.0
+ google_sign_in 6.2.1
+ googleapis 13.2.0
...
Got dependencies!
```

### Étape 4.4 : Vérifier la configuration Android

Le fichier `android/app/src/main/AndroidManifest.xml` doit contenir :

```xml
<!-- Permissions GPS -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET"/>
```

✅ **C'est déjà configuré dans le projet !**

### Étape 4.5 : Vérifier la configuration iOS (si applicable)

Le fichier `ios/Runner/Info.plist` doit contenir :

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>Cette application a besoin d'accéder à votre position pour afficher vos coordonnées GPS en temps réel.</string>
```

✅ **C'est déjà configuré dans le projet !**

---

## 5. Lancer l'application

### Étape 5.1 : Vérifier Flutter

```powershell
flutter doctor
```

**Résultat attendu :** Au moins un ✓ pour Android ou iOS.

### Étape 5.2 : Connecter un appareil

**Option A : Émulateur Android**

1. Ouvrez Android Studio
2. Cliquez sur **"Device Manager"**
3. Lancez un émulateur
4. Vérifiez avec : `flutter devices`

**Option B : Appareil physique Android**

1. Activez le **mode développeur** sur votre téléphone :
   - Paramètres > À propos du téléphone
   - Tapez 7 fois sur "Numéro de build"
2. Activez le **débogage USB** :
   - Paramètres > Options pour les développeurs
   - Activez "Débogage USB"
3. Connectez le téléphone à votre PC via USB
4. Vérifiez avec : `flutter devices`

### Étape 5.3 : Lancer l'application

```powershell
flutter run
```

**Résultat attendu :**
```
Launching lib\main.dart on Android SDK built for x86 in debug mode...
Running Gradle task 'assembleDebug'...
✓ Built build\app\outputs\flutter-apk\app-debug.apk
Installing build\app\outputs\flutter-apk\app-debug.apk...
Syncing files to device Android SDK built for x86...
```

### Étape 5.4 : Tester les fonctionnalités

**1. GPS :**
- L'app demande les permissions
- Acceptez
- La position GPS s'affiche automatiquement

**2. Google Calendar :**
- Cliquez sur **"Se connecter à Google"**
- Sélectionnez votre compte Google (utilisez le compte que vous avez ajouté en "Utilisateur test")
- Acceptez les permissions
- Les événements du jour s'affichent

**3. Microsoft Calendar :**
- Cliquez sur **"Se connecter à Microsoft"**
- Sélectionnez votre compte Microsoft
- Acceptez les permissions
- Les événements du jour s'affichent

---

## 6. Résolution de problèmes

### Problème 1 : "keytool n'est pas reconnu"

**Solution :**

keytool fait partie du JDK. Ajoutez-le au PATH :

1. Trouvez le JDK installé (souvent dans : `C:\Program Files\Android\Android Studio\jbr\bin`)
2. Ajoutez ce chemin au PATH :
   ```powershell
   $env:Path += ";C:\Program Files\Android\Android Studio\jbr\bin"
   ```
3. Réessayez la commande keytool

### Problème 2 : "Échec de la connexion Google"

**Vérifications :**

1. ✅ Avez-vous activé Google Calendar API ?
2. ✅ Avez-vous créé DEUX ID clients (Android ET Web) ?
3. ✅ Le SHA-1 est-il correct ?
4. ✅ Le package name est-il `com.example.flutter_gps_calendar_poc` ?
5. ✅ Avez-vous ajouté votre email en "Utilisateur test" ?

**Solution :**

Supprimez les données de l'app sur votre appareil et réessayez.

### Problème 3 : "Échec de la connexion Microsoft"

**Vérifications :**

1. ✅ Avez-vous bien copié le Client ID dans `microsoft_calendar_service.dart` ?
2. ✅ Le redirect URI est-il `msauth://flutter_gps_calendar_poc/auth` ?
3. ✅ Avez-vous activé les permissions `Calendars.Read` et `offline_access` ?

**Solution :**

Vérifiez que l'URI de redirection dans Azure Portal et dans `AndroidManifest.xml` correspondent.

### Problème 4 : "Permissions de localisation refusées"

**Solution :**

1. Ouvrez les paramètres de votre appareil
2. Apps > Flutter GPS Calendar POC
3. Permissions > Localisation
4. Sélectionnez "Toujours autoriser" ou "Autoriser uniquement lors de l'utilisation"

### Problème 5 : L'app ne se lance pas

**Solution :**

```powershell
# Nettoyez le projet
flutter clean

# Réinstallez les dépendances
flutter pub get

# Relancez
flutter run
```

### Problème 6 : "Gradle build failed"

**Solution :**

1. Ouvrez `android/app/build.gradle`
2. Vérifiez que `minSdkVersion` est au moins `21`
3. Exécutez :
   ```powershell
   cd android
   ./gradlew clean
   cd ..
   flutter run
   ```

---

## 7. Commandes de référence rapide

### Récupérer le SHA-1 (Windows)

```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

### Lancer l'app

```powershell
flutter run
```

### Lancer les tests

```powershell
flutter test
```

### Build APK

```powershell
flutter build apk --release
```

### Nettoyer le projet

```powershell
flutter clean
flutter pub get
```

---

## 8. Checklist finale

Avant de lancer l'app, vérifiez :

**Google Calendar :**
- [ ] Projet créé dans Google Cloud Console
- [ ] Google Calendar API activée
- [ ] Écran de consentement OAuth configuré
- [ ] Email ajouté en "Utilisateur test"
- [ ] ID client Android créé avec le bon SHA-1
- [ ] ID client Web créé

**Microsoft Calendar :**
- [ ] Application enregistrée dans Azure Portal
- [ ] Client ID copié dans `microsoft_calendar_service.dart`
- [ ] Permissions `Calendars.Read` et `offline_access` ajoutées
- [ ] URI de redirection configuré : `msauth://flutter_gps_calendar_poc/auth`

**Flutter :**
- [ ] `flutter pub get` exécuté
- [ ] Appareil connecté ou émulateur lancé
- [ ] `flutter doctor` sans erreurs critiques

---

## 9. Support

Si vous rencontrez un problème :

1. Vérifiez les logs : `flutter logs`
2. Consultez le README.md
3. Vérifiez les configurations dans Google Cloud Console et Azure Portal

---

**Bon développement ! 🚀**
