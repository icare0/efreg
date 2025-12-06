# POC Flutter GPS & Calendar

POC Flutter complet pour une application mobile avec :
- Récupération de la position GPS en temps réel
- Intégration Google Calendar en lecture seule
- Intégration Microsoft Outlook Calendar en lecture seule

## Technologies utilisées

- Flutter 3.22+
- Dart 3.4+ (null-safety)
- `geolocator` pour la géolocalisation
- `google_sign_in` + `googleapis` pour Google Calendar
- `flutter_web_auth_2` + Microsoft Graph API pour Outlook Calendar

## Architecture du projet

```
lib/
├── main.dart                                    # Point d'entrée de l'application
├── screens/
│   └── home_screen.dart                         # Écran principal (UI uniquement)
└── services/
    ├── location_service.dart                    # Service GPS
    ├── google_calendar_service.dart             # Service Google Calendar
    └── microsoft_calendar_service.dart          # Service Microsoft Calendar

test/
└── services/
    └── location_service_test.dart               # Tests unitaires
```

### Règles d'architecture

- **Toute la logique API** (OAuth, appels calendrier, géoloc) est dans `/services`
- **Les écrans** ne contiennent que de l'UI
- Chaque méthode importante est clairement commentée

## Prérequis

### 1. Installation Flutter

Assurez-vous d'avoir Flutter 3.22+ installé :

```bash
flutter --version
```

Si nécessaire, installez Flutter depuis : https://flutter.dev/docs/get-started/install

### 2. Configuration Google Calendar API

1. Allez sur [Google Cloud Console](https://console.cloud.google.com/)
2. Créez un nouveau projet ou sélectionnez un projet existant
3. Activez l'API Google Calendar :
   - Menu "APIs & Services" > "Library"
   - Recherchez "Google Calendar API" et activez-la
4. Créez des identifiants OAuth 2.0 :
   - Menu "APIs & Services" > "Credentials"
   - "Create Credentials" > "OAuth client ID"

   **Pour Android :**
   - Type : Android
   - Package name : `com.example.flutter_gps_calendar_poc`
   - SHA-1 : Obtenez-le avec `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`

   **Pour iOS :**
   - Type : iOS
   - Bundle ID : `com.example.flutterGpsCalendarPoc`
   - Téléchargez le fichier `GoogleService-Info.plist` et placez-le dans `ios/Runner/`

5. **Ajoutez le fichier de configuration iOS** :
   - Placez `GoogleService-Info.plist` dans `ios/Runner/`

### 3. Configuration Microsoft OAuth & Graph API

1. Allez sur [Azure Portal](https://portal.azure.com/)
2. Créez une App Registration :
   - Menu "Azure Active Directory" > "App registrations" > "New registration"
   - Nom : "Flutter GPS Calendar POC"
   - Supported account types : "Accounts in any organizational directory and personal Microsoft accounts"
   - Redirect URI : `msauth://flutter_gps_calendar_poc/auth`
   - Cliquez sur "Register"

3. Notez le **Client ID** (Application ID)

4. Configurez les permissions API :
   - Dans votre app, allez dans "API permissions"
   - "Add a permission" > "Microsoft Graph" > "Delegated permissions"
   - Ajoutez : `User.Read`, `Calendars.Read`, `offline_access`
   - Cliquez sur "Grant admin consent"

5. **Mettez à jour le code** :
   - Ouvrez `lib/services/microsoft_calendar_service.dart`
   - Remplacez `YOUR_MICROSOFT_CLIENT_ID` par votre Client ID

```dart
static const String _clientId = 'VOTRE_CLIENT_ID_ICI';
```

## Installation et lancement

### 1. Installer les dépendances

```bash
cd /home/user/efreg
flutter pub get
```

### 2. Vérifier la configuration

```bash
flutter doctor
```

Assurez-vous que tout est coché ✓ (au moins un appareil Android ou iOS).

### 3. Lancer l'application

**Sur émulateur/simulateur :**

```bash
# Android
flutter run

# iOS (macOS uniquement)
flutter run
```

**Sur appareil physique :**

```bash
# Connectez votre appareil via USB
# Activez le mode développeur sur votre appareil

# Listez les appareils disponibles
flutter devices

# Lancez sur un appareil spécifique
flutter run -d <device-id>
```

### 4. Lancer en mode debug avec hot reload

```bash
flutter run --debug
```

Pendant l'exécution :
- `r` : Hot reload
- `R` : Hot restart
- `q` : Quitter

### 5. Build de production

**Android (APK) :**

```bash
flutter build apk --release
```

L'APK sera dans : `build/app/outputs/flutter-apk/app-release.apk`

**iOS (nécessite macOS et Xcode) :**

```bash
flutter build ios --release
```

## Lancer les tests

### Tests unitaires

```bash
flutter test
```

### Test spécifique

```bash
flutter test test/services/location_service_test.dart
```

### Tests avec couverture

```bash
flutter test --coverage
```

## Utilisation de l'application

### 1. GPS en temps réel

Au lancement de l'application :
- L'app demande automatiquement les permissions de localisation
- Acceptez les permissions
- La position GPS s'affiche en temps réel avec :
  - Latitude
  - Longitude
  - Précision
  - Altitude

### 2. Google Calendar

1. Cliquez sur "Se connecter à Google"
2. Sélectionnez votre compte Google
3. Acceptez les permissions Calendar
4. Les événements du jour s'affichent automatiquement
5. Cliquez sur "Actualiser" pour recharger les événements

### 3. Microsoft Outlook Calendar

1. Cliquez sur "Se connecter à Microsoft"
2. Sélectionnez votre compte Microsoft
3. Acceptez les permissions Calendar
4. Les événements du jour s'affichent automatiquement
5. Cliquez sur "Actualiser" pour recharger les événements

## Fonctionnalités implémentées

### Services (`/lib/services/`)

#### `location_service.dart`
- ✅ Vérification des permissions GPS
- ✅ Récupération de la position actuelle
- ✅ Suivi GPS en temps réel (Stream)
- ✅ Calcul de distance entre deux points

#### `google_calendar_service.dart`
- ✅ Authentification OAuth2 Google
- ✅ Connexion/déconnexion
- ✅ Récupération des événements du jour
- ✅ Récupération des événements entre deux dates

#### `microsoft_calendar_service.dart`
- ✅ Authentification OAuth2 Microsoft
- ✅ Connexion/déconnexion via Microsoft Graph
- ✅ Récupération des événements du jour
- ✅ Récupération des événements entre deux dates

### Interface (`/lib/screens/`)

#### `home_screen.dart`
- ✅ Section GPS avec position en temps réel
- ✅ Section Google Calendar avec login et événements
- ✅ Section Microsoft Calendar avec login et événements
- ✅ Design Material 3
- ✅ Gestion des états de chargement
- ✅ Messages d'erreur utilisateur

### Tests (`/test/`)

- ✅ Tests unitaires pour `LocationService`
- ✅ Test de calcul de distance
- ✅ Tests de validation

## Dépendances principales

```yaml
dependencies:
  geolocator: ^12.0.0              # GPS
  google_sign_in: ^6.2.1           # Login Google
  googleapis: ^13.2.0              # Google APIs
  flutter_web_auth_2: ^3.1.2       # OAuth Microsoft
  http: ^1.2.2                     # Requêtes HTTP
  provider: ^6.1.2                 # State management
```

## Résolution de problèmes

### Erreur : "Permissions de localisation refusées"

**Android :**
- Ouvrez les paramètres de l'app
- Autorisez la localisation

**iOS :**
- Paramètres > Confidentialité > Service de localisation
- Activez pour l'application

### Erreur : "Échec de la connexion Google"

1. Vérifiez que Google Calendar API est activée
2. Vérifiez vos identifiants OAuth 2.0
3. Vérifiez que le SHA-1 est correct (Android)
4. Vérifiez que `GoogleService-Info.plist` est bien placé (iOS)

### Erreur : "Échec de la connexion Microsoft"

1. Vérifiez que le Client ID est correct dans `microsoft_calendar_service.dart`
2. Vérifiez que les permissions API sont accordées dans Azure Portal
3. Vérifiez que le redirect URI est `msauth://flutter_gps_calendar_poc/auth`

### L'app ne se lance pas

```bash
# Nettoyez le projet
flutter clean

# Réinstallez les dépendances
flutter pub get

# Relancez
flutter run
```

## Notes importantes

### Configuration requise pour production

Avant de déployer en production, vous DEVEZ :

1. **Google Calendar :**
   - Utiliser des identifiants OAuth production (pas debug)
   - Configurer l'écran de consentement OAuth

2. **Microsoft Calendar :**
   - Configurer un certificat de production
   - Configurer le redirect URI de production
   - Passer en revue les permissions Azure

3. **GPS :**
   - Expliquer clairement l'utilisation de la localisation dans les stores
   - Demander les permissions uniquement quand nécessaire

### Limitations actuelles du POC

- Pas de gestion du refresh token (déconnexion après expiration)
- Pas de cache local des événements
- Pas de synchronisation hors ligne
- Interface basique (POC)
- Pas de gestion multi-calendriers

## Commandes utiles

```bash
# Vérifier les erreurs de code
flutter analyze

# Formater le code
flutter format .

# Générer les icônes
flutter pub run flutter_launcher_icons

# Voir les logs
flutter logs

# Installer sur un appareil spécifique
flutter install -d <device-id>
```

## Support

Pour toute question ou problème :
1. Vérifiez la configuration des APIs (Google Cloud Console / Azure Portal)
2. Vérifiez les logs : `flutter logs`
3. Vérifiez la documentation officielle :
   - [Flutter](https://flutter.dev/docs)
   - [Geolocator](https://pub.dev/packages/geolocator)
   - [Google Sign-In](https://pub.dev/packages/google_sign_in)
   - [Microsoft Graph](https://docs.microsoft.com/en-us/graph/)

## License

Ce projet est un POC (Proof of Concept) à des fins de démonstration.
