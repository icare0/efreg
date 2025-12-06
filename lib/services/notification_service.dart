import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:permission_handler/permission_handler.dart';

/// Service de notifications locales
/// Gère l'envoi de notifications push locales sur l'appareil
class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();

  bool _isInitialized = false;

  /// Initialise le service de notifications
  /// Doit être appelé au démarrage de l'app
  Future<void> initialize() async {
    if (_isInitialized) {
      print('⚠️ NotificationService déjà initialisé');
      return;
    }

    print('🔧 Initialisation du NotificationService...');

    // Configuration Android
    const AndroidInitializationSettings androidSettings =
        AndroidInitializationSettings('ic_notification');

    // Configuration iOS
    const DarwinInitializationSettings iosSettings =
        DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    // Configuration globale
    const InitializationSettings settings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    // Initialiser le plugin
    final result = await _notificationsPlugin.initialize(
      settings,
      onDidReceiveNotificationResponse: _onNotificationTapped,
    );

    print('📱 Plugin initialisé: $result');

    // Créer les canaux de notification Android
    await _createNotificationChannels();

    _isInitialized = true;
    print('✅ NotificationService initialisé avec succès');
  }

  /// Crée les canaux de notification pour Android
  Future<void> _createNotificationChannels() async {
    print('📢 Création des canaux de notification Android...');

    const AndroidNotificationChannel testChannel = AndroidNotificationChannel(
      'test_channel',
      'Notifications de test',
      description: 'Canal pour les notifications de test',
      importance: Importance.high,
      playSound: true,
      enableVibration: true,
    );

    const AndroidNotificationChannel gpsChannel = AndroidNotificationChannel(
      'gps_channel',
      'Notifications GPS',
      description: 'Notifications liées à la géolocalisation',
      importance: Importance.high,
      playSound: true,
      enableVibration: true,
    );

    const AndroidNotificationChannel calendarChannel = AndroidNotificationChannel(
      'calendar_channel',
      'Notifications Calendrier',
      description: 'Notifications pour les événements du calendrier',
      importance: Importance.high,
      playSound: true,
      enableVibration: true,
    );

    const AndroidNotificationChannel scheduledChannel = AndroidNotificationChannel(
      'scheduled_channel',
      'Notifications planifiées',
      description: 'Notifications planifiées dans le futur',
      importance: Importance.high,
      playSound: true,
      enableVibration: true,
    );

    // Créer les canaux sur Android
    await _notificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(testChannel);

    await _notificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(gpsChannel);

    await _notificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(calendarChannel);

    await _notificationsPlugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(scheduledChannel);

    print('✅ Canaux de notification créés');
  }

  /// Callback quand l'utilisateur tape sur une notification
  void _onNotificationTapped(NotificationResponse response) {
    print('Notification tappée : ${response.payload}');
  }

  /// Demande les permissions de notifications (Android 13+ et iOS)
  Future<bool> requestPermissions() async {
    if (!_isInitialized) {
      await initialize();
    }

    // Demander la permission de notification (Android 13+ et iOS)
    final status = await Permission.notification.request();

    if (status.isGranted) {
      print('✅ Permission de notification accordée');
      return true;
    } else if (status.isDenied) {
      print('❌ Permission de notification refusée');
      return false;
    } else if (status.isPermanentlyDenied) {
      print('❌ Permission de notification refusée définitivement');
      // Ouvrir les paramètres de l'app
      await openAppSettings();
      return false;
    }

    return false;
  }

  /// Envoie une notification simple de test
  Future<void> showTestNotification() async {
    print('🔔 showTestNotification() appelée');

    if (!_isInitialized) {
      print('⚠️ Service non initialisé, initialisation en cours...');
      await initialize();
    }

    print('📤 Envoi de la notification de test...');

    // Utiliser un ID unique basé sur le timestamp pour éviter le remplacement
    final int notificationId = DateTime.now().millisecondsSinceEpoch.remainder(100000);
    print('🆔 ID de notification: $notificationId');

    const AndroidNotificationDetails androidDetails =
        AndroidNotificationDetails(
      'test_channel',
      'Notifications de test',
      channelDescription: 'Canal pour les notifications de test',
      importance: Importance.max,
      priority: Priority.high,
      showWhen: true,
      playSound: true,
      enableVibration: true,
      enableLights: true,
      autoCancel: true,
      ongoing: false,
      ticker: 'Test de notification',
    );

    const DarwinNotificationDetails iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    const NotificationDetails details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    try {
      await _notificationsPlugin.show(
        notificationId,
        '🔔 Test de notification',
        'Ceci est une notification de test du POC Flutter ! 🎉',
        details,
        payload: 'test_notification_$notificationId',
      );
      print('✅ Notification #$notificationId envoyée avec succès !');
      print('📱 Tirez la barre de notification vers le bas pour la voir');
    } catch (e) {
      print('❌ Erreur lors de l\'envoi de la notification: $e');
    }
  }

  /// Envoie une notification avec position GPS
  Future<void> showGPSNotification(double lat, double lng) async {
    if (!_isInitialized) {
      await initialize();
    }

    const AndroidNotificationDetails androidDetails =
        AndroidNotificationDetails(
      'gps_channel',
      'Notifications GPS',
      channelDescription: 'Notifications liées à la géolocalisation',
      importance: Importance.high,
      priority: Priority.high,
    );

    const DarwinNotificationDetails iosDetails = DarwinNotificationDetails();

    const NotificationDetails details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _notificationsPlugin.show(
      1,
      'Position GPS',
      'Lat: ${lat.toStringAsFixed(4)}, Lng: ${lng.toStringAsFixed(4)}',
      details,
      payload: 'gps_notification',
    );
  }

  /// Envoie une notification pour un événement calendrier
  Future<void> showCalendarNotification(String eventTitle, String provider) async {
    if (!_isInitialized) {
      await initialize();
    }

    const AndroidNotificationDetails androidDetails =
        AndroidNotificationDetails(
      'calendar_channel',
      'Notifications Calendrier',
      channelDescription: 'Notifications pour les événements du calendrier',
      importance: Importance.high,
      priority: Priority.high,
    );

    const DarwinNotificationDetails iosDetails = DarwinNotificationDetails();

    const NotificationDetails details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _notificationsPlugin.show(
      2,
      'Événement $provider',
      eventTitle,
      details,
      payload: 'calendar_notification',
    );
  }

  /// Envoie une notification planifiée (dans X secondes)
  Future<void> showScheduledNotification(int delaySeconds) async {
    if (!_isInitialized) {
      await initialize();
    }

    const AndroidNotificationDetails androidDetails =
        AndroidNotificationDetails(
      'scheduled_channel',
      'Notifications planifiées',
      channelDescription: 'Notifications planifiées dans le futur',
      importance: Importance.high,
      priority: Priority.high,
    );

    const DarwinNotificationDetails iosDetails = DarwinNotificationDetails();

    const NotificationDetails details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    // Note: Pour les notifications planifiées, il faut utiliser le package timezone
    // Pour ce POC, on envoie juste une notification immédiate
    await Future.delayed(Duration(seconds: delaySeconds));

    await _notificationsPlugin.show(
      3,
      'Notification planifiée',
      'Cette notification était planifiée il y a $delaySeconds secondes !',
      details,
      payload: 'scheduled_notification',
    );
  }

  /// Ouvre les paramètres de notification de l'application
  Future<void> openNotificationSettings() async {
    print('🔧 Ouverture des paramètres de notification...');
    await openAppSettings();
  }

  /// Annule toutes les notifications
  Future<void> cancelAllNotifications() async {
    await _notificationsPlugin.cancelAll();
  }

  /// Annule une notification spécifique
  Future<void> cancelNotification(int id) async {
    await _notificationsPlugin.cancel(id);
  }
}
