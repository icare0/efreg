# Guide de Lancement Rapide - POC Flutter GPS & Calendar

## Démarrage rapide (5 minutes)

### 1. Installation des dépendances

```bash
cd /home/user/efreg
flutter pub get
```

### 2. Configuration minimale pour tester

**IMPORTANT:** Avant de lancer l'application, vous DEVEZ configurer les APIs.

#### Pour Google Calendar :

1. Allez sur https://console.cloud.google.com/
2. Créez un projet
3. Activez "Google Calendar API"
4. Créez des identifiants OAuth 2.0 pour Android/iOS
5. Notez vos identifiants

#### Pour Microsoft Calendar :

1. Ouvrez `lib/services/microsoft_calendar_service.dart`
2. Ligne 33, remplacez :
   ```dart
   static const String _clientId = 'YOUR_MICROSOFT_CLIENT_ID';
   ```
   Par votre Client ID obtenu sur https://portal.azure.com/

### 3. Lancer l'application

```bash
# Vérifiez que Flutter est bien installé
flutter doctor

# Lancez l'application
flutter run
```

Si vous avez plusieurs appareils :
```bash
# Listez les appareils
flutter devices

# Lancez sur un appareil spécifique
flutter run -d <device-id>
```

### 4. Tester les fonctionnalités

Une fois l'app lancée :

1. **GPS** : Acceptez les permissions → La position s'affiche automatiquement
2. **Google Calendar** : Cliquez sur "Se connecter à Google" → Connectez-vous
3. **Microsoft Calendar** : Cliquez sur "Se connecter à Microsoft" → Connectez-vous

## Commandes utiles

```bash
# Hot reload (pendant que l'app tourne)
r

# Hot restart
R

# Lancer les tests
flutter test

# Analyser le code
flutter analyze

# Formater le code
flutter format .

# Build Android APK
flutter build apk --release

# Nettoyer le projet
flutter clean && flutter pub get
```

## Résolution de problèmes rapide

### "Permissions de localisation refusées"
→ Ouvrez les paramètres de votre appareil et autorisez la localisation pour l'app

### "Échec de la connexion Google"
→ Vérifiez que vous avez bien configuré OAuth 2.0 dans Google Cloud Console

### "Échec de la connexion Microsoft"
→ Vérifiez que vous avez bien remplacé `YOUR_MICROSOFT_CLIENT_ID` dans le code

### L'app ne se lance pas
```bash
flutter clean
flutter pub get
flutter run
```

## Structure du code

```
lib/
├── main.dart                          # Point d'entrée
├── screens/
│   └── home_screen.dart              # UI (Interface uniquement)
└── services/
    ├── location_service.dart         # Logique GPS
    ├── google_calendar_service.dart  # Logique Google Calendar
    └── microsoft_calendar_service.dart # Logique Microsoft Calendar
```

## Documentation complète

Pour plus de détails, consultez `README.md`
